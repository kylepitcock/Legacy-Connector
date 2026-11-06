package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy mods distributed as a folder (MCHeli: {@code mods/mcheli/} holding {@code mcmod.info}, the class packages and {@code assets/}, with the
 * user's content packs added next to them). A folder in mods/ is ignored by FML's own locator; here it is packed into a jar under
 * {@code .legacyconnector/dirmods/} (rebuilt only when a file's name, size or time changed) which is then inspected and translated like any other
 * legacy jar. The legacy mod's {@code ModContainer.getSource()} stays the real folder ({@link LegacyJar#sourceDir}), so mods that list their own
 * content with {@code java.io.File} find it, and the zips/jars placed in the folder (content packs) are left out of the packed jar.
 */
final class DirectoryMods {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");
    private static final Set<String> VERSION_FOLDERS = Set.of("1.4.7", "1.5.2", "1.6.4", "1.7.10", "1.12.2", "1.20.1", "1.16.5", "1.2.5");

    private DirectoryMods() {}

    /** Packs every mod-like subfolder of {@code dir} (not the version folders) and returns the packed jars with their folders. */
    static List<Path[]> pack(Path dir, Path outDir) {
        List<Path[]> out = new ArrayList<>();
        List<Path> folders;
        try (Stream<Path> files = Files.list(dir)) {
            folders = files.filter(Files::isDirectory).filter(f -> !VERSION_FOLDERS.contains(f.getFileName().toString()))
                    .sorted(Comparator.comparing(f -> f.getFileName().toString().toLowerCase())).toList();
        } catch (IOException e) {
            return out;
        }
        for (Path folder : folders) {
            try {
                if (!Files.exists(folder.resolve("mcmod.info")) && !hasClasses(folder)) continue;
                out.add(new Path[] {packFolder(folder, outDir), folder});
            } catch (IOException | RuntimeException e) {
                LOG.debug("Legacy Connector: could not pack the mod folder {}", folder, e);
            }
        }
        return out;
    }

    private static boolean hasClasses(Path folder) throws IOException {
        try (Stream<Path> walk = Files.walk(folder, 8)) {
            return walk.anyMatch(p -> p.getFileName().toString().endsWith(".class"));
        }
    }

    private static boolean included(Path root, Path file) {
        // content packs and libraries put next to the mod are read by the mod itself from the folder
        return !(root.relativize(file).getNameCount() == 1 && isArchive(file.getFileName().toString().toLowerCase()));
    }

    private static boolean isArchive(String name) {
        return name.endsWith(".zip") || name.endsWith(".jar");
    }

    private static Path packFolder(Path folder, Path outDir) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(folder)) {
            files = walk.filter(Files::isRegularFile).filter(f -> included(folder, f))
                    .sorted(Comparator.comparing(f -> folder.relativize(f).toString().replace('\\', '/'))).toList();
        }
        MessageDigest md = SplitPackageResolver.sha256();
        for (Path f : files) {
            md.update((folder.relativize(f).toString().replace('\\', '/') + '\0' + Files.size(f) + '\0'
                    + Files.getLastModifiedTime(f).toMillis() + '\n').getBytes(StandardCharsets.UTF_8));
        }
        String stamp = HexFormat.of().formatHex(md.digest());
        String name = folder.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        Path jar = outDir.resolve(name + ".jar");
        Path stampFile = outDir.resolve(name + ".jar.stamp");
        if (Files.exists(jar) && Files.exists(stampFile) && stamp.equals(Files.readString(stampFile).trim())) return jar;

        Files.createDirectories(outDir);
        Path tmp = Files.createTempFile(outDir, name, ".tmp");
        try (OutputStream os = Files.newOutputStream(tmp); JarOutputStream jos = new JarOutputStream(os)) {
            for (Path f : files) {
                String entryName = folder.relativize(f).toString().replace('\\', '/');
                JarEntry e = new JarEntry(entryName);
                e.setTime(Files.getLastModifiedTime(f).toMillis());
                jos.putNextEntry(e);
                Files.copy(f, jos);
                jos.closeEntry();
            }
        }
        Files.move(tmp, jar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(stampFile, stamp);
        LOG.info("Legacy Connector: packed the mod folder {} ({} files) into {}", folder.getFileName(), files.size(), jar.getFileName());
        return jar;
    }
}
