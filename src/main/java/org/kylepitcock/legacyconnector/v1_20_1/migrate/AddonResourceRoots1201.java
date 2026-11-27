package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

/** Supplies extracted addon marker URLs to Minecraft's vanilla resource scan. */
public final class AddonResourceRoots1201 {
    public static final String ROOTS_PROPERTY = "legacyconnector.addonResourceRoots";
    private static final String ADDON_MARKER = "addon/.mcassetsroot";

    private AddonResourceRoots1201() {}

    public static List<Path> addonRoots() {
        String configured = System.getProperty(ROOTS_PROPERTY, "");
        if (configured.isBlank()) return List.of();
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        for (String value : configured.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (value == null || value.isBlank()) continue;
            try {
                Path root = Path.of(value).toAbsolutePath().normalize();
                if (Files.isDirectory(root) && Files.isRegularFile(root.resolve(".mcassetsroot"))) {
                    roots.add(root);
                }
            } catch (RuntimeException ignored) {
                // Ignore stale or malformed cache paths; resource discovery must remain safe.
            }
        }
        return List.copyOf(roots);
    }

    public static Enumeration<URL> appendAddonRootMarkers(
            String resourceName, Enumeration<URL> classpathResources) throws IOException {
        if (!isAddonMarker(resourceName)) return classpathResources;
        LinkedHashSet<URL> urls = new LinkedHashSet<>();
        if (classpathResources != null) {
            while (classpathResources.hasMoreElements()) urls.add(classpathResources.nextElement());
        }
        for (Path root : addonRoots()) {
            Path marker = root.resolve(".mcassetsroot");
            if (Files.isRegularFile(marker)) urls.add(marker.toUri().toURL());
        }
        return Collections.enumeration(urls);
    }

    private static boolean isAddonMarker(String resourceName) {
        if (resourceName == null) return false;
        String normalized = resourceName.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return ADDON_MARKER.equals(normalized);
    }
}
