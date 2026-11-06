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
        String name = flat(version, legacyName);
        return forVersion(version).getOrDefault(name, name);
    }

    private static final Map<LegacyVersion, Map<String, String>> FLAT = new EnumMap<>(LegacyVersion.class);

    /**
     * The packaged (1.5.2) name of a flat {@code net/minecraft/src/<Name>} class of a version with flat names (1.4.7), {@code legacyName} itself otherwise or
     * when the class has no packaged name (the ModLoader emulation: {@code BaseMod}, {@code ModLoader}, ...). A nested class follows its outer class.
     */
    public static String flat(LegacyVersion version, String legacyName) {
        if (!version.flatNames() || !legacyName.startsWith("net/minecraft/src/")) return legacyName;
        int nested = legacyName.indexOf('$');
        String outer = nested < 0 ? legacyName : legacyName.substring(0, nested);
        String packaged = flatTable(version).get(outer);
        if (packaged == null) return legacyName;
        return nested < 0 ? packaged : packaged + legacyName.substring(nested);
    }

    private static synchronized Map<String, String> flatTable(LegacyVersion version) {
        return FLAT.computeIfAbsent(version, v -> readTable("legacyconnector/forward/" + v.id() + "/flat-names.tsv"));
    }

    private static Map<String, String> readTable(String resource) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = ForwardRenames.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return out;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int tab = line.indexOf('	');
                if (tab > 0) out.put(line.substring(0, tab), line.substring(tab + 1).strip());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Corrupt rename table " + resource, e);
        }
        return Map.copyOf(out);
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
