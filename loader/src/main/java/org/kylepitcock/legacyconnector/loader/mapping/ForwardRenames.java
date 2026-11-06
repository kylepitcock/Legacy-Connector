package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;

/**
 * Class renames of a forward version (see {@link LegacyVersion#forward()}): a 1.6.4 name to the 1.7.10 name the shim
 * implements ({@code Icon} to {@code IIcon}). The tables are {@code legacyconnector/forward/<id>/class-renames.tsv}
 * (old, tab, new; internal names). Versions without a table (every other one) rename nothing.
 */
public final class ForwardRenames {
    private static final Map<LegacyVersion, Map<String, String>> CACHE = new EnumMap<>(LegacyVersion.class);

    private ForwardRenames() {}

    /** The 1.7.10 name of a legacy class, or {@code legacyName} itself when the version does not rename it. */
    public static String map(LegacyVersion version, String legacyName) {
        if (version.forward() == null) return legacyName;
        return forVersion(version).getOrDefault(legacyName, legacyName);
    }

    public static synchronized Map<String, String> forVersion(LegacyVersion version) {
        return CACHE.computeIfAbsent(version, v -> {
            Map<String, String> out = new HashMap<>();
            for (String id : v.forwardTableIds()) {
                String resource = "legacyconnector/forward/" + id + "/class-renames.tsv";
                try (InputStream in = ForwardRenames.class.getClassLoader().getResourceAsStream(resource)) {
                    if (in == null) continue;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        if (line.isBlank() || line.startsWith("#")) continue;
                        int tab = line.indexOf('\t');
                        if (tab > 0) out.put(line.substring(0, tab), line.substring(tab + 1).strip());
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Corrupt rename table " + resource, e);
                }
            }
            return Map.copyOf(out);
        });
    }
}
