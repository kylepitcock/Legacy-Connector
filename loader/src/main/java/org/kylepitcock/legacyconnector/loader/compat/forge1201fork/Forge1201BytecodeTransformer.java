package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201MappingCatalog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Applies only the reviewed Forge-to-NeoForge package relocations to an already
 * metadata-translated Forge 1.20.1 JAR.
 *
 * <p>This is deliberately not a complete mod port. Types classified as requiring a
 * semantic adapter, and unreviewed Forge API types, remain unchanged and are reported.
 * Minecraft 1.20.1 API members are not renamed here. The stage is kept separate from
 * the legacy 1.7.10/1.12.2 transformer.</p>
 */
public final class Forge1201BytecodeTransformer {
    private static final String NEOFORGE_METADATA = "META-INF/neoforge.mods.toml";
    private static final String SERVICES_PREFIX = "META-INF/services/";

    private Forge1201BytecodeTransformer() {}

    public record Result(
            Path input,
            Path output,
            String inputSha256,
            String outputSha256,
            int classesScanned,
            int classesChanged,
            int classEntriesRelocated,
            int resourcesCopied,
            int signaturesRemoved,
            List<String> relocatedTypes,
            List<String> manualAdapterTypes,
            List<String> unmappedForgeApiTypes,
            Forge1201RegistryAdapter.Stats registryAdapters,
            Forge1201MenuAdapter.Stats menuAdapters,
            String adapterProfileId,
            int adapterRulesApplied,
            int adapterOperationsApplied) {
        public Result {
            relocatedTypes = List.copyOf(relocatedTypes);
            manualAdapterTypes = List.copyOf(manualAdapterTypes);
            unmappedForgeApiTypes = List.copyOf(unmappedForgeApiTypes);
        }
    }

    public static Result transform(Path input, Path output) throws IOException {
        return transform(input, output, null);
    }

    /**
     * Runs the package relocation pass with an optional external, fail-fast call-site adapter profile.
     * The profile is applied to original Forge-namespaced class files before package relocation.
     */
    public static Result transform(Path input, Path output, Path adapterProfilePath) throws IOException {
        if (input == null || output == null) {
            throw new IllegalArgumentException("Input and output paths are required");
        }

        Path source = input.toAbsolutePath().normalize();
        Path destination = output.toAbsolutePath().normalize();
        if (source.equals(destination)) {
            throw new IllegalArgumentException("The bytecode transform must not overwrite its input JAR");
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("Input JAR does not exist or is not a regular file: " + source);
        }

        Path parent = destination.getParent();
        if (parent == null) throw new IOException("Output JAR must have a parent directory: " + destination);
        Files.createDirectories(parent);

        try (ZipFile check = new ZipFile(source.toFile())) {
            if (check.getEntry(NEOFORGE_METADATA) == null) {
                throw new IllegalArgumentException(
                        "Input must already contain " + NEOFORGE_METADATA
                                + "; run the Forge 1.20.1 metadata stage first");
            }
            if (check.getEntry("META-INF/mods.toml") != null) {
                throw new IllegalArgumentException(
                        "Input still contains Forge mods.toml; run the metadata stage first");
            }
        }

        Forge1201CallSiteAdapter.Profile adapterProfile = adapterProfilePath == null
                ? null : Forge1201CallSiteAdapter.load(adapterProfilePath);
        Forge1201CallSiteAdapter.Session adapterSession = adapterProfile == null
                ? null : new Forge1201CallSiteAdapter.Session(adapterProfile);

        String inputDigest = sha256(source);
        String temporaryPrefix = destination.getFileName().toString();
        if (temporaryPrefix.length() < 3) temporaryPrefix = (temporaryPrefix + "___").substring(0, 3);
        Path temporary = Files.createTempFile(parent, temporaryPrefix, ".tmp");

        CatalogRemapper remapper = new CatalogRemapper();
        int classesScanned = 0;
        int classesChanged = 0;
        int classEntriesRelocated = 0;
        int resourcesCopied = 0;
        int signaturesRemoved = 0;
        Forge1201RegistryAdapter.Stats registryAdapters = Forge1201RegistryAdapter.Stats.empty();
        Forge1201MenuAdapter.Stats menuAdapters = Forge1201MenuAdapter.Stats.empty();
        Set<String> sourceEntries = new HashSet<>();
        Set<String> outputEntries = new HashSet<>();

        try {
            try (ZipFile zip = new ZipFile(source.toFile());
                 OutputStream fileOut = Files.newOutputStream(temporary);
                 ZipOutputStream jarOut = new ZipOutputStream(fileOut)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String sourceName = entry.getName();
                    if (!sourceEntries.add(sourceName)) {
                        throw new IOException("Duplicate ZIP entry in input JAR: " + sourceName);
                    }

                    if (entry.isDirectory()) {
                        // Directory entries are optional in ZIP/JAR files. Omit them so that
                        // stale pre-relocation package directories are not carried forward.
                        continue;
                    }

                    if (isSignatureEntry(sourceName)) {
                        signaturesRemoved++;
                        continue;
                    }

                    byte[] contents;
                    try (InputStream in = zip.getInputStream(entry)) {
                        contents = in.readAllBytes();
                    }

                    String outputName = sourceName;
                    if (sourceName.endsWith(".class")) {
                        classesScanned++;
                        ClassReader reader;
                        try {
                            reader = new ClassReader(contents);
                        } catch (RuntimeException malformed) {
                            throw new IOException("Unreadable class entry " + sourceName, malformed);
                        }

                        String originalInternalName = reader.getClassName();
                        String mappedInternalName = remapper.map(originalInternalName);
                        outputName = remapClassEntryName(sourceName, originalInternalName, mappedInternalName);

                        byte[] originalClassBytes = contents;
                        ClassWriter writer = new ClassWriter(0);
                        try {
                            ClassNode classNode = new ClassNode();
                            reader.accept(classNode, 0);
                            if (adapterSession != null) adapterSession.apply(classNode);
                            menuAdapters = menuAdapters.plus(Forge1201MenuAdapter.apply(classNode));
                            registryAdapters = registryAdapters.plus(Forge1201RegistryAdapter.apply(classNode));
                            classNode.accept(new ClassRemapper(writer, remapper));
                        } catch (RuntimeException malformed) {
                            throw new IOException("Could not remap class entry " + sourceName, malformed);
                        }
                        contents = writer.toByteArray();
                        if (!Arrays.equals(contents, originalClassBytes)) {
                            classesChanged++;
                        }
                        if (!outputName.equals(sourceName)) classEntriesRelocated++;
                    } else if (sourceName.startsWith(SERVICES_PREFIX)) {
                        ServiceResource service = remapServiceResource(sourceName, contents, remapper);
                        outputName = service.name();
                        contents = service.contents();
                    }

                    if (!outputEntries.add(outputName)) {
                        throw new IOException("Two input entries map to the same output entry: " + outputName);
                    }

                    ZipEntry target = new ZipEntry(outputName);
                    if (entry.getTime() >= 0) target.setTime(entry.getTime());
                    if (entry.getComment() != null) target.setComment(entry.getComment());
                    jarOut.putNextEntry(target);
                    jarOut.write(contents);
                    jarOut.closeEntry();
                    if (!sourceName.endsWith(".class")) resourcesCopied++;
                }
            }

            Forge1201CallSiteAdapter.Stats adapterStats = adapterSession == null
                    ? new Forge1201CallSiteAdapter.Stats(null, 0, 0)
                    : adapterSession.finish();

            moveAtomically(temporary, destination);
            return new Result(
                    source,
                    destination,
                    inputDigest,
                    sha256(destination),
                    classesScanned,
                    classesChanged,
                    classEntriesRelocated,
                    resourcesCopied,
                    signaturesRemoved,
                    List.copyOf(remapper.relocatedTypes),
                    List.copyOf(remapper.manualAdapterTypes),
                    List.copyOf(remapper.unmappedForgeApiTypes),
                    registryAdapters,
                    menuAdapters,
                    adapterStats.profileId(),
                    adapterStats.rulesApplied(),
                    adapterStats.operationsApplied());
        } catch (IOException | RuntimeException failure) {
            Files.deleteIfExists(temporary);
            throw failure;
        }
    }

    private static String remapClassEntryName(
            String entryName, String originalInternalName, String mappedInternalName) throws IOException {
        if (originalInternalName.equals(mappedInternalName)) return entryName;
        String expectedSuffix = originalInternalName + ".class";
        if (!entryName.endsWith(expectedSuffix)) {
            throw new IOException("Class entry path does not match its class name: " + entryName
                    + " contains " + originalInternalName);
        }
        String prefix = entryName.substring(0, entryName.length() - expectedSuffix.length());
        return prefix + mappedInternalName + ".class";
    }

    private static ServiceResource remapServiceResource(
            String entryName, byte[] contents, CatalogRemapper remapper) {
        String serviceName = entryName.substring(SERVICES_PREFIX.length());
        String mappedServiceName = remapBinaryClassName(serviceName, remapper);

        String text = new String(contents, StandardCharsets.UTF_8);
        StringBuilder mappedText = new StringBuilder(text.length() + 32);
        int start = 0;
        while (start < text.length()) {
            int lineEnd = start;
            while (lineEnd < text.length() && text.charAt(lineEnd) != '\n' && text.charAt(lineEnd) != '\r') {
                lineEnd++;
            }
            String line = text.substring(start, lineEnd);
            int leading = 0;
            while (leading < line.length() && Character.isWhitespace(line.charAt(leading))) leading++;
            int tokenEnd = leading;
            while (tokenEnd < line.length() && !Character.isWhitespace(line.charAt(tokenEnd))
                    && line.charAt(tokenEnd) != '#') {
                tokenEnd++;
            }
            if (leading < tokenEnd) {
                String provider = line.substring(leading, tokenEnd);
                mappedText.append(line, 0, leading)
                        .append(remapBinaryClassName(provider, remapper))
                        .append(line.substring(tokenEnd));
            } else {
                mappedText.append(line);
            }

            if (lineEnd < text.length()) {
                if (text.charAt(lineEnd) == '\r' && lineEnd + 1 < text.length()
                        && text.charAt(lineEnd + 1) == '\n') {
                    mappedText.append("\r\n");
                    start = lineEnd + 2;
                } else {
                    mappedText.append(text.charAt(lineEnd));
                    start = lineEnd + 1;
                }
            } else {
                start = lineEnd;
            }
        }

        String mappedEntry = SERVICES_PREFIX + mappedServiceName;
        return new ServiceResource(mappedEntry, mappedText.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String remapBinaryClassName(String binaryName, CatalogRemapper remapper) {
        if (binaryName.isBlank()) return binaryName;
        String internalName = binaryName.replace('.', '/');
        String mapped = remapper.map(internalName);
        return mapped.replace('/', '.');
    }

    private static boolean isSignatureEntry(String name) {
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        if (!upper.startsWith("META-INF/")) return false;
        String leaf = upper.substring("META-INF/".length());
        if (leaf.contains("/")) return false;
        return leaf.endsWith(".SF") || leaf.endsWith(".RSA") || leaf.endsWith(".DSA")
                || leaf.endsWith(".EC") || leaf.startsWith("SIG-");
    }

    private static void moveAtomically(Path temporary, Path destination) throws IOException {
        try {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(Path path) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("The Java runtime does not provide SHA-256", impossible);
        }

        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private record ServiceResource(String name, byte[] contents) {}

    private static final class CatalogRemapper extends Remapper {
        private final Set<String> relocatedTypes = new TreeSet<>();
        private final Set<String> manualAdapterTypes = new TreeSet<>();
        private final Set<String> unmappedForgeApiTypes = new TreeSet<>();

        @Override
        public String map(String internalName) {
            if (internalName == null) return null;
            Forge1201MappingCatalog.Decision decision = Forge1201MappingCatalog.classify(internalName);
            return switch (decision.kind()) {
                case PACKAGE_RELOCATION_CANDIDATE, EXPLICIT_TYPE_REPLACEMENT -> {
                    relocatedTypes.add(internalName + " -> " + decision.targetInternalName());
                    yield decision.targetInternalName();
                }
                case MANUAL_ADAPTER_REQUIRED -> {
                    manualAdapterTypes.add(internalName);
                    yield internalName;
                }
                case UNMAPPED_FORGE_API -> {
                    unmappedForgeApiTypes.add(internalName);
                    yield internalName;
                }
                case NOT_FORGE_API -> internalName;
            };
        }
    }
}
