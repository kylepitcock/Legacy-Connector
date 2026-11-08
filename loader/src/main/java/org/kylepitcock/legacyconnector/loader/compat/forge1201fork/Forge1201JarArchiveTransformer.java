package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201JarInspector;
import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201JarInfo;
import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201MetadataTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Creates a metadata-translated copy of a Forge 1.20.1 JAR.
 *
 * <p>This stage deliberately does not rewrite classes or enable discovery. It copies
 * every ordinary archive entry, replaces META-INF/mods.toml with
 * META-INF/neoforge.mods.toml, and removes stale JAR signature files. The input JAR
 * is never modified.</p>
 */
public final class Forge1201JarArchiveTransformer {
    public static final String SOURCE_METADATA = "META-INF/mods.toml";
    public static final String TARGET_METADATA = "META-INF/neoforge.mods.toml";

    private Forge1201JarArchiveTransformer() {}

    public record Result(
            Path input,
            Path output,
            String inputSha256,
            String outputSha256,
            int copiedEntries,
            int removedSignatureEntries) {}

    public static Result transform(
            Path input,
            Path output,
            String neoForgeLoaderRange,
            String neoForgeVersionRange,
            String minecraftVersionRange) throws IOException {
        if (input == null || output == null) throw new IllegalArgumentException("Input and output paths are required");

        Path source = input.toAbsolutePath().normalize();
        Path destination = output.toAbsolutePath().normalize();
        if (source.equals(destination)) {
            throw new IllegalArgumentException("The translated JAR must not overwrite the original input");
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("Input JAR does not exist or is not a regular file: " + source);
        }

        Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(source);
        if (info == null) {
            throw new IllegalArgumentException("Input is not a recognized Forge 1.20.1 mod JAR: " + source);
        }

        Path parent = destination.getParent();
        if (parent == null) throw new IOException("Output JAR must have a parent directory: " + destination);
        Files.createDirectories(parent);

        String inputDigest = sha256(source);
        String temporaryPrefix = destination.getFileName().toString();
        if (temporaryPrefix.length() < 3) temporaryPrefix = (temporaryPrefix + "___").substring(0, 3);
        Path temporary = Files.createTempFile(parent, temporaryPrefix, ".tmp");
        int copied = 0;
        int removedSignatures = 0;
        boolean wroteMetadata = false;
        Set<String> seenEntries = new HashSet<>();

        try {
            try (ZipFile zip = new ZipFile(source.toFile());
                 OutputStream fileOut = Files.newOutputStream(temporary);
                 ZipOutputStream jarOut = new ZipOutputStream(fileOut)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        copyEntryHeader(entry, jarOut);
                        jarOut.closeEntry();
                        copied++;
                        continue;
                    }

                    String name = entry.getName();
                    if (!seenEntries.add(name)) {
                        throw new IOException("Duplicate ZIP entry in source JAR: " + name);
                    }

                    if (isSignatureEntry(name)) {
                        removedSignatures++;
                        continue;
                    }

                    if (name.equals(SOURCE_METADATA)) {
                        if (wroteMetadata) {
                            throw new IOException("Source JAR contains multiple " + SOURCE_METADATA + " entries");
                        }
                        String sourceToml;
                        try (InputStream in = zip.getInputStream(entry)) {
                            sourceToml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        }
                        String translatedToml = Forge1201MetadataTransformer.transformToml(
                                sourceToml,
                                neoForgeLoaderRange,
                                neoForgeVersionRange,
                                minecraftVersionRange);
                        ZipEntry replacement = new ZipEntry(TARGET_METADATA);
                        if (entry.getTime() >= 0) replacement.setTime(entry.getTime());
                        if (entry.getComment() != null) replacement.setComment(entry.getComment());
                        jarOut.putNextEntry(replacement);
                        jarOut.write(translatedToml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        jarOut.closeEntry();
                        wroteMetadata = true;
                        copied++;
                        continue;
                    }

                    copyEntryHeader(entry, jarOut);
                    try (InputStream in = zip.getInputStream(entry)) {
                        in.transferTo(jarOut);
                    }
                    jarOut.closeEntry();
                    copied++;
                }
            }

            if (!wroteMetadata) {
                throw new IOException("Source JAR did not contain " + SOURCE_METADATA);
            }

            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return new Result(source, destination, inputDigest, sha256(destination), copied, removedSignatures);
        } catch (IOException | RuntimeException failure) {
            Files.deleteIfExists(temporary);
            throw failure;
        }
    }

    /**
     * Creates a metadata-only test artifact. This does not make a Forge JAR loadable:
     * its Forge/Minecraft bytecode references are intentionally left untouched.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "Usage: transformForge1201Metadata <input.jar> <output.jar> "
                            + "<javafml-range> <neoforge-range> <minecraft-range>");
        }

        Result result = transform(
                Path.of(args[0]),
                Path.of(args[1]),
                args[2],
                args[3],
                args[4]);
        System.out.println("Created metadata-only Forge 1.20.1 test JAR: " + result.output());
        System.out.println("  input SHA-256:  " + result.inputSha256());
        System.out.println("  output SHA-256: " + result.outputSha256());
        System.out.println("  copied entries: " + result.copiedEntries());
        System.out.println("  removed signature entries: " + result.removedSignatureEntries());
        System.out.println("  NOTE: classes were not remapped; this artifact is not ready to load in-game.");
    }

    private static void copyEntryHeader(ZipEntry source, ZipOutputStream output) throws IOException {
        ZipEntry target = new ZipEntry(source.getName());
        if (source.getTime() >= 0) target.setTime(source.getTime());
        if (source.getComment() != null) target.setComment(source.getComment());
        output.putNextEntry(target);
    }

    private static boolean isSignatureEntry(String name) {
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        if (!upper.startsWith("META-INF/")) return false;

        String leaf = upper.substring("META-INF/".length());
        if (leaf.contains("/")) return false;
        return leaf.endsWith(".SF")
                || leaf.endsWith(".RSA")
                || leaf.endsWith(".DSA")
                || leaf.endsWith(".EC")
                || leaf.startsWith("SIG-");
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
}
