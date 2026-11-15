package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.Type;

/**
 * Produces a deterministic, read-only inventory of Minecraft classes visible from a resolved
 * target compile classpath. Class metadata is read through the existing TargetIndex so this
 * inventory shares the verifier's descriptor parser and hierarchy cache implementation.
 *
 * <p>This tool inventories the target namespace. It does not generate mappings, compare versions,
 * rewrite selectors, load Minecraft classes, or claim semantic compatibility.</p>
 */
public final class Forge1201TargetApiInventory {
    private static final String MINECRAFT_PREFIX = "net/minecraft/";
    private static final int MAX_CLASSPATH_ENTRIES = 4096;
    private static final int MAX_CLASSPATH_FILE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_CLASS_ENTRY_OCCURRENCES = 200_000;
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private Forge1201TargetApiInventory() {}

    public record ClasspathEntry(
            int index,
            String path,
            Long sizeBytes,
            int minecraftClassEntryOccurrences,
            String sha256) {}

    public record ClasspathOccurrence(int classpathIndex, String path) {}

    public record Member(String name, String descriptor) {}

    public record ClassRecord(
            String internalName,
            String resolvedResource,
            List<ClasspathOccurrence> classpathOccurrences,
            String superName,
            List<String> interfaces,
            List<Member> methods,
            List<Member> fields) {
        public ClassRecord {
            classpathOccurrences = List.copyOf(classpathOccurrences);
            interfaces = List.copyOf(interfaces);
            methods = List.copyOf(methods);
            fields = List.copyOf(fields);
        }
    }

    public record UnresolvedClass(String internalName, String reason) {}

    public record Report(
            String schemaVersion,
            String targetMinecraftVersion,
            String targetNeoForgeVersion,
            String namespaceEvidence,
            boolean complete,
            int classpathEntryCount,
            int classEntryOccurrenceCount,
            int uniqueClassCount,
            int duplicateClassNameCount,
            int resolvedClassCount,
            int unresolvedClassCount,
            int declaredMethodCount,
            int declaredFieldCount,
            int uniqueMinecraftReferencedTypeCount,
            int missingMinecraftReferencedTypeCount,
            List<String> minecraftReferencedTypes,
            List<String> missingMinecraftReferencedTypes,
            List<ClasspathEntry> classpathEntries,
            List<ClassRecord> classes,
            List<UnresolvedClass> unresolvedClasses) {
        public Report {
            minecraftReferencedTypes = List.copyOf(minecraftReferencedTypes);
            missingMinecraftReferencedTypes = List.copyOf(missingMinecraftReferencedTypes);
            classpathEntries = List.copyOf(classpathEntries);
            classes = List.copyOf(classes);
            unresolvedClasses = List.copyOf(unresolvedClasses);
        }
    }

    private record ScannedEntry(
            int index,
            Path path,
            List<String> classNames,
            Long sizeBytes,
            String sha256) {}

    /**
     * Inspects ordered classpath entries. Order is preserved because it determines which
     * duplicate class resource a URLClassLoader resolves first.
     */
    public static Report inspect(List<Path> suppliedClasspath) throws IOException {
        if (suppliedClasspath == null || suppliedClasspath.isEmpty()) {
            throw new IllegalArgumentException("At least one target compile-classpath entry is required");
        }

        LinkedHashSet<Path> unique = new LinkedHashSet<>();
        for (Path supplied : suppliedClasspath) {
            if (supplied == null) continue;
            Path path = supplied.toAbsolutePath().normalize();
            if (!Files.exists(path) || (!Files.isDirectory(path) && !Files.isRegularFile(path))) {
                throw new IllegalArgumentException(
                        "Target classpath entry does not exist or is not a file/directory: " + path);
            }
            unique.add(path);
            if (unique.size() > MAX_CLASSPATH_ENTRIES) {
                throw new IllegalArgumentException(
                        "Target classpath exceeds " + MAX_CLASSPATH_ENTRIES + " entries");
            }
        }
        if (unique.isEmpty()) {
            throw new IllegalArgumentException("Target compile classpath contains no usable entries");
        }

        List<ScannedEntry> scannedEntries = new ArrayList<>();
        Map<String, List<ClasspathOccurrence>> occurrences = new TreeMap<>();
        int totalOccurrences = 0;
        int index = 0;
        for (Path path : unique) {
            ScannedEntry scanned = scanEntry(index, path);
            scannedEntries.add(scanned);
            for (String className : scanned.classNames()) {
                occurrences.computeIfAbsent(className, ignored -> new ArrayList<>())
                        .add(new ClasspathOccurrence(index, path.toString()));
                totalOccurrences++;
                if (totalOccurrences > MAX_CLASS_ENTRY_OCCURRENCES) {
                    throw new IOException("Minecraft class-entry occurrence count exceeds "
                            + MAX_CLASS_ENTRY_OCCURRENCES);
                }
            }
            index++;
        }

        if (occurrences.isEmpty()) {
            throw new IOException("No " + MINECRAFT_PREFIX + "**/*.class entries were found on the target classpath");
        }

        List<ClasspathEntry> classpathReports = scannedEntries.stream()
                .map(entry -> new ClasspathEntry(
                        entry.index(),
                        entry.path().toString(),
                        entry.sizeBytes(),
                        entry.classNames().size(),
                        entry.sha256()))
                .toList();

        List<ClassRecord> classRecords = new ArrayList<>();
        List<UnresolvedClass> unresolved = new ArrayList<>();
        Set<String> referencedMinecraftTypes = new TreeSet<>();
        int methodCount = 0;
        int fieldCount = 0;

        URL[] urls = new URL[unique.size()];
        int urlIndex = 0;
        for (Path path : unique) urls[urlIndex++] = path.toUri().toURL();

        try (URLClassLoader targetLoader =
                     new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            Forge1201TargetApiVerifier.TargetIndex targetIndex =
                    Forge1201TargetApiVerifier.createIndex(internalName ->
                            targetLoader.getResourceAsStream(internalName + ".class"));

            for (Map.Entry<String, List<ClasspathOccurrence>> candidate : occurrences.entrySet()) {
                String internalName = candidate.getKey();
                URL resource = targetLoader.getResource(internalName + ".class");
                if (resource == null) {
                    unresolved.add(new UnresolvedClass(internalName,
                            "Class entry was enumerated but URLClassLoader could not resolve its resource"));
                    continue;
                }

                Optional<Forge1201TargetApiVerifier.TargetClassInfo> optionalInfo;
                try {
                    optionalInfo = targetIndex.classInfo(internalName);
                } catch (IOException | RuntimeException failure) {
                    unresolved.add(new UnresolvedClass(internalName, conciseMessage(failure)));
                    continue;
                }
                if (optionalInfo.isEmpty()) {
                    unresolved.add(new UnresolvedClass(internalName,
                            "TargetIndex did not find the enumerated class resource"));
                    continue;
                }

                Forge1201TargetApiVerifier.TargetClassInfo info = optionalInfo.get();
                if (!internalName.equals(info.name())) {
                    unresolved.add(new UnresolvedClass(internalName,
                            "Resolved class name differs from enumerated resource: " + info.name()));
                    continue;
                }

                List<Member> methods = info.methods().stream()
                        .map(signature -> new Member(signature.name(), signature.descriptor()))
                        .sorted(Comparator.comparing(Member::name).thenComparing(Member::descriptor))
                        .toList();
                List<Member> fields = info.fields().stream()
                        .map(signature -> new Member(signature.name(), signature.descriptor()))
                        .sorted(Comparator.comparing(Member::name).thenComparing(Member::descriptor))
                        .toList();

                methodCount += methods.size();
                fieldCount += fields.size();

                if (isMinecraftType(info.superName())) referencedMinecraftTypes.add(info.superName());
                for (String implementedInterface : info.interfaces()) {
                    if (isMinecraftType(implementedInterface)) referencedMinecraftTypes.add(implementedInterface);
                }
                for (Member method : methods) {
                    collectDescriptorTypes(method.descriptor(), referencedMinecraftTypes);
                }
                for (Member field : fields) {
                    collectDescriptorTypes(field.descriptor(), referencedMinecraftTypes);
                }

                classRecords.add(new ClassRecord(
                        internalName,
                        resource.toExternalForm(),
                        candidate.getValue(),
                        info.superName(),
                        info.interfaces().stream().sorted().toList(),
                        methods,
                        fields));
            }
        }

        List<String> minecraftTypes = List.copyOf(referencedMinecraftTypes);
        List<String> discoveredClassNames = List.copyOf(occurrences.keySet());
        Set<String> discoveredSet = Set.copyOf(discoveredClassNames);
        List<String> missingMinecraftTypes = minecraftTypes.stream()
                .filter(type -> !discoveredSet.contains(type))
                .toList();
        int duplicateClassNames = (int) occurrences.values().stream()
                .filter(classOccurrences -> classOccurrences.size() > 1)
                .count();

        return new Report(
                "1.0",
                Forge1201PreflightAnalyzer.TARGET_MINECRAFT_VERSION,
                Forge1201PreflightAnalyzer.TARGET_NEOFORGE_VERSION,
                "Mojang-official names confirmed by representative javap anchors; class bytes include NeoForge patches",
                unresolved.isEmpty() && missingMinecraftTypes.isEmpty(),
                classpathReports.size(),
                totalOccurrences,
                occurrences.size(),
                duplicateClassNames,
                classRecords.size(),
                unresolved.size(),
                methodCount,
                fieldCount,
                minecraftTypes.size(),
                missingMinecraftTypes.size(),
                minecraftTypes,
                missingMinecraftTypes,
                classpathReports,
                classRecords,
                unresolved);
    }

    static List<Path> readClasspathFile(Path classpathFile) throws IOException {
        if (classpathFile == null || !Files.isRegularFile(classpathFile)) {
            throw new IllegalArgumentException("Target classpath list is not a regular file: " + classpathFile);
        }
        if (Files.size(classpathFile) > MAX_CLASSPATH_FILE_BYTES) {
            throw new IllegalArgumentException(
                    "Target classpath list exceeds " + MAX_CLASSPATH_FILE_BYTES + " bytes");
        }

        LinkedHashSet<Path> entries = new LinkedHashSet<>();
        for (String line : Files.readAllLines(classpathFile, StandardCharsets.UTF_8)) {
            String value = line.trim();
            if (value.isEmpty()) continue;
            Path path = Path.of(value).toAbsolutePath().normalize();
            if (!Files.exists(path) || (!Files.isDirectory(path) && !Files.isRegularFile(path))) {
                throw new IllegalArgumentException(
                        "Target classpath entry does not exist or is not a file/directory: " + path);
            }
            entries.add(path);
            if (entries.size() > MAX_CLASSPATH_ENTRIES) {
                throw new IllegalArgumentException(
                        "Target classpath exceeds " + MAX_CLASSPATH_ENTRIES + " entries");
            }
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Target classpath list contains no entries");
        }
        return List.copyOf(entries);
    }

    static String toJson(Report report) {
        return GSON.toJson(report);
    }

    private static ScannedEntry scanEntry(int index, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Path minecraftRoot = path.resolve(MINECRAFT_PREFIX);
            if (!Files.isDirectory(minecraftRoot)) {
                return new ScannedEntry(index, path, List.of(), null, null);
            }

            List<String> classNames;
            try (var files = Files.walk(minecraftRoot)) {
                classNames = files.filter(Files::isRegularFile)
                        .map(file -> path.relativize(file).toString().replace('\\', '/'))
                        .filter(name -> name.startsWith(MINECRAFT_PREFIX) && name.endsWith(".class"))
                        .map(name -> name.substring(0, name.length() - ".class".length()))
                        .sorted()
                        .toList();
            }
            if (classNames.size() > MAX_CLASS_ENTRY_OCCURRENCES) {
                throw new IOException("Minecraft class-entry count in directory " + path + " exceeds "
                        + MAX_CLASS_ENTRY_OCCURRENCES);
            }
            String digest = classNames.isEmpty() ? null : sha256DirectoryClasses(path, classNames);
            return new ScannedEntry(index, path, classNames, null, digest);
        }

        List<String> classNames = new ArrayList<>();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (!name.startsWith(MINECRAFT_PREFIX) || !name.endsWith(".class")) continue;
                classNames.add(toInternalName(name));
                if (classNames.size() > MAX_CLASS_ENTRY_OCCURRENCES) {
                    throw new IOException("Minecraft class-entry count in " + path + " exceeds "
                            + MAX_CLASS_ENTRY_OCCURRENCES);
                }
            }
        } catch (IOException failure) {
            throw new IOException("Cannot enumerate target compile-classpath archive " + path, failure);
        }
        classNames.sort(String::compareTo);
        String digest = classNames.isEmpty() ? null : sha256File(path);
        return new ScannedEntry(index, path, List.copyOf(classNames), Files.size(path), digest);
    }

    private static String toInternalName(String entryName) throws IOException {
        if (!entryName.startsWith(MINECRAFT_PREFIX)
                || !entryName.endsWith(".class")
                || entryName.indexOf('\\') >= 0
                || entryName.startsWith("/")) {
            throw new IOException("Unsafe Minecraft class entry name: " + entryName);
        }
        String internalName = entryName.substring(0, entryName.length() - ".class".length());
        for (String segment : internalName.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Unsafe Minecraft class entry name: " + entryName);
            }
        }
        return internalName;
    }

    private static boolean isMinecraftType(String internalName) {
        return internalName != null && internalName.startsWith(MINECRAFT_PREFIX);
    }

    private static void collectDescriptorTypes(String descriptor, Set<String> destination) {
        Type type = descriptor.startsWith("(") ? Type.getMethodType(descriptor) : Type.getType(descriptor);
        if (type.getSort() == Type.METHOD) {
            collectType(type.getReturnType(), destination);
            for (Type argument : type.getArgumentTypes()) collectType(argument, destination);
        } else {
            collectType(type, destination);
        }
    }

    private static void collectType(Type type, Set<String> destination) {
        if (type.getSort() == Type.ARRAY) {
            collectType(type.getElementType(), destination);
        } else if (type.getSort() == Type.OBJECT) {
            String internalName = type.getInternalName();
            if (isMinecraftType(internalName)) destination.add(internalName);
        }
    }

    private static String sha256File(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String sha256DirectoryClasses(Path root, List<String> classNames) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String className : classNames) {
                digest.update(className.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                Path file = root.resolve(className + ".class");
                try (InputStream input = Files.newInputStream(file)) {
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
                }
                digest.update((byte) 0xff);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String conciseMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : failure.getClass().getSimpleName() + ": " + message;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: Forge1201TargetApiInventory <target-classpath.txt> <output-report.json>");
        }
        Path classpathFile = Path.of(args[0]).toAbsolutePath().normalize();
        Path reportFile = Path.of(args[1]).toAbsolutePath().normalize();
        List<Path> classpath = readClasspathFile(classpathFile);
        Report report = inspect(classpath);

        Path parent = reportFile.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(reportFile, toJson(report) + System.lineSeparator(), StandardCharsets.UTF_8);

        System.out.println("Target API inventory — read-only class-file inspection");
        System.out.println("Target: Minecraft " + report.targetMinecraftVersion()
                + " / NeoForge " + report.targetNeoForgeVersion());
        System.out.println("Classpath entries: " + report.classpathEntryCount());
        System.out.println("Minecraft class-entry occurrences: " + report.classEntryOccurrenceCount());
        System.out.println("Unique Minecraft classes: " + report.uniqueClassCount());
        System.out.println("Duplicate class names across classpath: " + report.duplicateClassNameCount());
        System.out.println("Resolved class metadata: " + report.resolvedClassCount());
        System.out.println("Unresolved class metadata: " + report.unresolvedClassCount());
        System.out.println("Declared methods: " + report.declaredMethodCount());
        System.out.println("Declared fields: " + report.declaredFieldCount());
        System.out.println("Unique Minecraft superclass/interface/descriptor types: "
                + report.uniqueMinecraftReferencedTypeCount());
        System.out.println("Missing referenced Minecraft types: " + report.missingMinecraftReferencedTypeCount());
        System.out.println("INVENTORY_STATUS=" + (report.complete() ? "COMPLETE" : "PARTIAL"));
        System.out.println("REPORT=" + reportFile);
        if (!report.unresolvedClasses().isEmpty()) {
            report.unresolvedClasses().forEach(value ->
                    System.out.println("  UNRESOLVED: " + value.internalName() + " — " + value.reason()));
        }
        if (!report.missingMinecraftReferencedTypes().isEmpty()) {
            report.missingMinecraftReferencedTypes().forEach(value ->
                    System.out.println("  MISSING REFERENCED TYPE: " + value));
        }
    }
}
