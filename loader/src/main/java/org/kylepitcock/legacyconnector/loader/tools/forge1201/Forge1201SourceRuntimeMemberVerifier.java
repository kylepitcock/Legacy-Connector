package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.mapping.Forge1201SourceOfficialMappingIndex;
import org.objectweb.asm.ClassReader;

/**
 * Read-only verifier for Mojang-named members in the exact Forge 1.20.1 source runtime classpath.
 *
 * <p>The classpath reader preserves declared order and first-match semantics. Class bytes are
 * parsed through {@link Forge1201TargetApiVerifier#createIndex} (the shared ASM classfile index);
 * no Minecraft class is defined, initialized, or executed. The target verifier is reused only as
 * a classfile parser/hierarchy walker: callers of this class supply the pinned Forge source
 * classpath, never the NeoForge target classpath.</p>
 */
public final class Forge1201SourceRuntimeMemberVerifier {
    private static final long MAX_CLASSPATH_LIST_BYTES = 16L * 1024 * 1024;
    private static final int MAX_CLASSPATH_ENTRIES = 4096;
    private static final int MAX_INDEXED_CLASSES = 2_000_000;

    private Forge1201SourceRuntimeMemberVerifier() {}

    public enum Status {
        SOURCE_RUNTIME_CLASS_MISSING,
        SOURCE_RUNTIME_MEMBER_DECLARED_EXACT,
        SOURCE_RUNTIME_MEMBER_INHERITED_EXACT,
        SOURCE_RUNTIME_MEMBER_NAME_FOUND_DESCRIPTOR_CHANGED,
        SOURCE_RUNTIME_MEMBER_ABSENT,
        SOURCE_RUNTIME_HIERARCHY_INCOMPLETE
    }

    public record MemberResult(
            String owner,
            String name,
            String descriptor,
            Status status,
            String declaringOwner,
            List<String> hierarchyVisited,
            List<String> observedSameNameMembers) {
        public MemberResult {
            hierarchyVisited = List.copyOf(hierarchyVisited);
            observedSameNameMembers = List.copyOf(observedSameNameMembers);
        }

        public String display() {
            return owner.replace('/', '.') + "#" + name + descriptor + " [" + status + "]"
                    + (declaringOwner == null ? "" : " declared by " + declaringOwner.replace('/', '.'));
        }
    }

    public record SelectorVerification(
            Forge1201SourceOfficialMappingIndex.Status mappingStatus,
            List<MemberResult> members) {
        public SelectorVerification {
            members = List.copyOf(members);
        }
    }

    public record ClasspathSummary(
            String classpathList,
            long classpathListBytes,
            String classpathListSha256,
            int classpathEntryCount,
            int indexedClassCount,
            int duplicateClassNames) {}

    /**
     * Per-analysis source class index. Each instance has an isolated ASM metadata cache.
     */
    public static final class Index {
        private final Forge1201TargetApiVerifier.TargetIndex classIndex;
        private final ClasspathSummary classpathSummary;

        private Index(
                Forge1201TargetApiVerifier.TargetIndex classIndex,
                ClasspathSummary classpathSummary) {
            this.classIndex = classIndex;
            this.classpathSummary = classpathSummary;
        }

        public ClasspathSummary classpathSummary() {
            return classpathSummary;
        }

        public MemberResult inspectMethod(String owner, String name, String descriptor) throws IOException {
            return inspect(owner, name, descriptor, true);
        }

        public MemberResult inspectField(String owner, String name, String descriptor) throws IOException {
            return inspect(owner, name, descriptor, false);
        }

        /** Maps an SRG method selector, then verifies its exact Mojang-named source identity. */
        public SelectorVerification verifyMethod(
                Forge1201SourceOfficialMappingIndex mappings,
                String srgOwner,
                String srgName,
                String srgDescriptor) throws IOException {
            Forge1201SourceOfficialMappingIndex.SelectorResolution resolution =
                    mappings.resolveMethod(srgOwner, srgName, srgDescriptor);
            if (resolution.status() != Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT
                    && resolution.status() != Forge1201SourceOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES) {
                return new SelectorVerification(resolution.status(), List.of());
            }
            List<MemberResult> results = new ArrayList<>();
            for (Forge1201SourceOfficialMappingIndex.SelectorCandidate candidate : resolution.candidates()) {
                results.add(inspectMethod(
                        candidate.officialOwner(), candidate.officialName(), candidate.officialDescriptor()));
            }
            return new SelectorVerification(resolution.status(), results);
        }

        /** Maps an SRG field selector, then verifies its exact Mojang-named source identity. */
        public SelectorVerification verifyField(
                Forge1201SourceOfficialMappingIndex mappings,
                String srgOwner,
                String srgName) throws IOException {
            Forge1201SourceOfficialMappingIndex.SelectorResolution resolution =
                    mappings.resolveField(srgOwner, srgName);
            if (resolution.status() != Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT
                    && resolution.status() != Forge1201SourceOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES) {
                return new SelectorVerification(resolution.status(), List.of());
            }
            List<MemberResult> results = new ArrayList<>();
            for (Forge1201SourceOfficialMappingIndex.SelectorCandidate candidate : resolution.candidates()) {
                results.add(inspectField(
                        candidate.officialOwner(), candidate.officialName(), candidate.officialDescriptor()));
            }
            return new SelectorVerification(resolution.status(), results);
        }

        private MemberResult inspect(String owner, String name, String descriptor, boolean method)
                throws IOException {
            Optional<Forge1201TargetApiVerifier.TargetClassInfo> ownerInfo = classIndex.classInfo(owner);
            if (ownerInfo.isEmpty()) {
                return new MemberResult(owner, name, descriptor, Status.SOURCE_RUNTIME_CLASS_MISSING,
                        null, List.of(owner), List.of());
            }

            Forge1201TargetApiVerifier.MemberSignature expected =
                    new Forge1201TargetApiVerifier.MemberSignature(name, descriptor);
            SetView direct = new SetView(method ? ownerInfo.get().methods() : ownerInfo.get().fields());
            if (direct.contains(expected)) {
                return new MemberResult(owner, name, descriptor, Status.SOURCE_RUNTIME_MEMBER_DECLARED_EXACT,
                        owner, List.of(owner), List.of());
            }

            Forge1201TargetApiVerifier.MemberLookup lookup = method
                    ? classIndex.lookupMethod(owner, name, descriptor)
                    : classIndex.lookupField(owner, name, descriptor);
            List<String> visited = lookup.hierarchyVisited();

            if (lookup.status() == Forge1201TargetApiVerifier.LookupStatus.FOUND) {
                for (String visitedOwner : visited) {
                    if (visitedOwner.equals(owner)) continue;
                    Optional<Forge1201TargetApiVerifier.TargetClassInfo> info = classIndex.classInfo(visitedOwner);
                    if (info.isEmpty()) continue;
                    SetView members = new SetView(method ? info.get().methods() : info.get().fields());
                    if (members.contains(expected)) {
                        return new MemberResult(owner, name, descriptor,
                                Status.SOURCE_RUNTIME_MEMBER_INHERITED_EXACT, visitedOwner, visited, List.of());
                    }
                }
                // The hierarchy lookup found the symbol but its declaration could not be read back.
                return new MemberResult(owner, name, descriptor,
                        Status.SOURCE_RUNTIME_HIERARCHY_INCOMPLETE, null, visited, List.of());
            }

            List<String> sameName = new ArrayList<>();
            for (String visitedOwner : visited) {
                Optional<Forge1201TargetApiVerifier.TargetClassInfo> info = classIndex.classInfo(visitedOwner);
                if (info.isEmpty()) continue;
                SetView members = new SetView(method ? info.get().methods() : info.get().fields());
                for (Forge1201TargetApiVerifier.MemberSignature member : members.values()) {
                    if (member.name().equals(name)) {
                        sameName.add(visitedOwner + "#" + member.name() + member.descriptor());
                    }
                }
            }
            sameName.sort(String::compareTo);

            if (lookup.status() == Forge1201TargetApiVerifier.LookupStatus.INCOMPLETE_HIERARCHY) {
                return new MemberResult(owner, name, descriptor,
                        Status.SOURCE_RUNTIME_HIERARCHY_INCOMPLETE, null, visited, sameName);
            }
            if (!sameName.isEmpty()) {
                return new MemberResult(owner, name, descriptor,
                        Status.SOURCE_RUNTIME_MEMBER_NAME_FOUND_DESCRIPTOR_CHANGED, null, visited, sameName);
            }
            return new MemberResult(owner, name, descriptor,
                    Status.SOURCE_RUNTIME_MEMBER_ABSENT, null, visited, List.of());
        }
    }

    /**
     * Creates an index over a caller-supplied byte provider. Primarily useful for tests and
     * analyzers that already have an ordered classpath abstraction.
     */
    public static Index createIndex(Forge1201TargetApiVerifier.ClassBytesProvider classBytes) {
        if (classBytes == null) throw new IllegalArgumentException("Forge source class byte provider is required");
        return new Index(Forge1201TargetApiVerifier.createIndex(classBytes), null);
    }

    /**
     * Reads an ordered classpath-list file emitted by the isolated ForgeGradle source-runtime
     * exporter. Entries are indexed in order; the first occurrence of every class wins.
     */
    public static Index readClasspath(Path classpathListFile) throws IOException {
        if (classpathListFile == null || !Files.isRegularFile(classpathListFile)
                || !Files.isReadable(classpathListFile)) {
            throw new IOException("Forge source runtime classpath list is not a readable regular file: "
                    + classpathListFile);
        }
        long listBytes = Files.size(classpathListFile);
        if (listBytes <= 0 || listBytes > MAX_CLASSPATH_LIST_BYTES) {
            throw new IOException("Forge source runtime classpath-list size is outside the supported range: "
                    + listBytes);
        }

        List<String> lines = Files.readAllLines(classpathListFile, StandardCharsets.UTF_8);
        List<Path> entries = new ArrayList<>();
        for (String line : lines) {
            String value = line.trim();
            if (value.isEmpty()) continue;
            if (value.startsWith("#")) continue;
            entries.add(Path.of(value).toAbsolutePath().normalize());
            if (entries.size() > MAX_CLASSPATH_ENTRIES) {
                throw new IOException("Forge source runtime classpath exceeds " + MAX_CLASSPATH_ENTRIES + " entries");
            }
        }
        if (entries.isEmpty()) throw new IOException("Forge source runtime classpath list contains no entries");

        OrderedClasspath provider = new OrderedClasspath(entries);
        ClasspathSummary summary = new ClasspathSummary(
                classpathListFile.toAbsolutePath().normalize().toString(),
                listBytes,
                sha256(classpathListFile),
                entries.size(),
                provider.classCount(),
                provider.duplicateClassNames());
        return new Index(Forge1201TargetApiVerifier.createIndex(provider), summary);
    }

    private static final class SetView {
        private final java.util.Set<Forge1201TargetApiVerifier.MemberSignature> values;

        private SetView(java.util.Set<Forge1201TargetApiVerifier.MemberSignature> values) {
            this.values = values;
        }

        private boolean contains(Forge1201TargetApiVerifier.MemberSignature value) {
            return values.contains(value);
        }

        private java.util.Set<Forge1201TargetApiVerifier.MemberSignature> values() {
            return values;
        }
    }

    private record ClassLocation(Path classFile, Path jarFile, String jarEntry) {
        static ClassLocation directory(Path classFile) {
            return new ClassLocation(classFile, null, null);
        }

        static ClassLocation jar(Path jarFile, String jarEntry) {
            return new ClassLocation(null, jarFile, jarEntry);
        }
    }

    private static final class OrderedClasspath implements Forge1201TargetApiVerifier.ClassBytesProvider {
        private final Map<String, ClassLocation> classes = new HashMap<>();
        private int duplicateClassNames;

        private OrderedClasspath(List<Path> entries) throws IOException {
            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    indexDirectory(entry);
                } else if (Files.isRegularFile(entry)) {
                    indexArchive(entry);
                } else {
                    throw new IOException("Forge source classpath entry does not exist: " + entry);
                }
            }
        }

        int classCount() {
            return classes.size();
        }

        int duplicateClassNames() {
            return duplicateClassNames;
        }

        private void indexDirectory(Path root) throws IOException {
            try (var paths = Files.walk(root)) {
                for (Path file : paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".class")).toList()) {
                    String internalName = root.relativize(file).toString()
                            .replace('\\', '/');
                    internalName = internalName.substring(0, internalName.length() - ".class".length());
                    addClass(internalName, ClassLocation.directory(file.toAbsolutePath().normalize()));
                }
            }
        }

        private void indexArchive(Path archivePath) throws IOException {
            try (JarFile archive = new JarFile(archivePath.toFile(), false, ZipFile.OPEN_READ)) {
                Enumeration<JarEntry> entries = archive.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    if (!name.endsWith(".class") || name.startsWith("META-INF/versions/")) continue;
                    String internalName = name.substring(0, name.length() - ".class".length());
                    addClass(internalName, ClassLocation.jar(
                            archivePath.toAbsolutePath().normalize(), name));
                }
            }
        }

        private void addClass(String internalName, ClassLocation location) throws IOException {
            if (internalName.isBlank() || internalName.startsWith("/") || internalName.contains("..")) return;
            if (classes.putIfAbsent(internalName, location) != null) {
                duplicateClassNames++;
                return;
            }
            if (classes.size() > MAX_INDEXED_CLASSES) {
                throw new IOException("Forge source runtime classpath exceeds "
                        + MAX_INDEXED_CLASSES + " indexed class files");
            }
        }

        @Override
        public InputStream open(String internalName) throws IOException {
            if (internalName == null || internalName.isBlank() || internalName.startsWith("/")
                    || internalName.contains("..")) {
                throw new IOException("Invalid source class resource name: " + internalName);
            }
            ClassLocation location = classes.get(internalName);
            if (location == null) return null;
            if (location.classFile() != null) return Files.newInputStream(location.classFile());

            JarFile archive = new JarFile(location.jarFile().toFile(), false, ZipFile.OPEN_READ);
            JarEntry entry = archive.getJarEntry(location.jarEntry());
            if (entry == null) {
                archive.close();
                throw new IOException("Source classpath archive changed after indexing: "
                        + location.jarFile() + "!" + location.jarEntry());
            }
            InputStream stream;
            try {
                stream = archive.getInputStream(entry);
            } catch (IOException exception) {
                archive.close();
                throw exception;
            }
            return new FilterInputStream(stream) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        archive.close();
                    }
                }
            };
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                for (int count; (count = input.read(buffer)) != -1;) digest.update(buffer, 0, count);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
