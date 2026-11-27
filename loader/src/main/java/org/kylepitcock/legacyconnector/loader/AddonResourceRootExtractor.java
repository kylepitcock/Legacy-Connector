package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Extracts marker-bearing addon/ roots from final translated legacy jars. */
final class AddonResourceRootExtractor {
    static final String ROOTS_PROPERTY = "legacyconnector.addonResourceRoots";
    private static final String MARKER = "addon/.mcassetsroot";

    private AddonResourceRootExtractor() {}

    static List<Path> extract(Path cacheDir, List<Path> translatedJars) throws IOException {
        Path extractionDir = cacheDir.resolve("addon-resource-roots");
        deleteTree(extractionDir);
        Files.createDirectories(extractionDir);

        List<Path> roots = new ArrayList<>();
        List<Path> jars = translatedJars.stream()
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(path -> path.toAbsolutePath().toString()))
                .toList();

        for (Path jar : jars) {
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                if (zip.getEntry(MARKER) == null) continue;
                String fileName = jar.getFileName() == null ? "legacy-mod" : jar.getFileName().toString();
                String safeName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
                Path addonRoot = extractionDir.resolve(
                        String.format(Locale.ROOT, "%03d-%s", roots.size(), safeName))
                        .resolve("addon").toAbsolutePath().normalize();
                Files.createDirectories(addonRoot);

                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith("addon/")) continue;
                    String relativeName = name.substring("addon/".length());
                    if (relativeName.isBlank()) continue;
                    Path target = addonRoot.resolve(relativeName).normalize();
                    if (!target.startsWith(addonRoot)) continue;
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                        continue;
                    }
                    Files.createDirectories(target.getParent());
                    try (var input = zip.getInputStream(entry)) {
                        Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                if (Files.isRegularFile(addonRoot.resolve(".mcassetsroot"))) roots.add(addonRoot);
            }
        }

        if (roots.isEmpty()) {
            System.clearProperty(ROOTS_PROPERTY);
        } else {
            System.setProperty(ROOTS_PROPERTY, roots.stream().map(Path::toString)
                    .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator)));
        }
        return List.copyOf(roots);
    }

    private static void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
