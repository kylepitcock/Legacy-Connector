package org.kylepitcock.mccompat.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Legacy libraries that were never updated, replaced by their modern equivalent instead of being loaded: when the
 * modern mod is installed, the legacy library jar is skipped and every reference to its API package is relocated
 * into the shim, where the API is re-implemented on top of the modern mod (like Connector Extras bridges Trinkets
 * to Curios). Without the modern mod, the legacy library is translated and loaded as usual.
 */
public final class LibraryBridges {
    /** @param apiRoot internal-name package root of the legacy API, re-implemented under the shim prefix */
    public record Bridge(String legacyModId, String apiRoot, String modernModId) {}

    public static final List<Bridge> KNOWN = List.of(
            new Bridge("Baubles", "baubles/api/", "curios"));

    private static final Pattern MOD_ID = Pattern.compile("modId\\s*=\\s*\"([a-z0-9_.-]+)\"");

    private LibraryBridges() {}

    /** Bridges whose modern mod is present in {@code modsDir} (or forced with -Dmccompat.bridges=baubles,...). */
    public static List<Bridge> active(Path modsDir) {
        Set<String> modern = modernModIds(modsDir);
        String forced = System.getProperty("mccompat.bridges", "").toLowerCase(Locale.ROOT);
        List<Bridge> out = new ArrayList<>();
        for (Bridge b : KNOWN) {
            if (modern.contains(b.modernModId) || forced.contains(b.legacyModId.toLowerCase(Locale.ROOT))) out.add(b);
        }
        return out;
    }

    private static Set<String> modernModIds(Path modsDir) {
        Set<String> ids = new HashSet<>();
        if (!Files.isDirectory(modsDir)) return ids;
        try (Stream<Path> files = Files.list(modsDir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".jar")).toList()) {
                try (ZipFile zip = new ZipFile(p.toFile())) {
                    ZipEntry e = zip.getEntry("META-INF/neoforge.mods.toml");
                    if (e == null) continue;
                    try (InputStream in = zip.getInputStream(e)) {
                        Matcher m = MOD_ID.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        while (m.find()) ids.add(m.group(1));
                    }
                } catch (IOException | RuntimeException ignored) {
                    // not a readable jar; FML reports it
                }
            }
        } catch (IOException ignored) {
            // no mods folder access
        }
        return ids;
    }

    /** True if this legacy jar is the library a bridge replaces (it declares the bridged mod id). */
    public static Bridge replacing(LegacyJar jar, List<Bridge> active) {
        for (Bridge b : active) {
            for (LegacyModDecl m : jar.mods) if (m.legacyId.equalsIgnoreCase(b.legacyModId)) return b;
        }
        return null;
    }
}
