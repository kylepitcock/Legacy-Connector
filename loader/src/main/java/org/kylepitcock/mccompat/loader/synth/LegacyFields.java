package org.kylepitcock.mccompat.loader.synth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.kylepitcock.mccompat.loader.LegacyVersion;

/** Every SRG-named field of a legacy version (owner, MCP name, descriptor), for fields mods only reach by reflection. */
public final class LegacyFields {
    public record Field(String owner, String name, String desc, boolean isStatic) {}

    private static final Map<LegacyVersion, Map<String, Field>> CACHE = new ConcurrentHashMap<>();

    private LegacyFields() {}

    /** Every legacy field of every class (for completing hand-written shim classes), by owner. */
    public static Map<String, java.util.List<Field>> byOwner(String versionId) {
        Map<String, java.util.List<Field>> out = new HashMap<>();
        try (InputStream in = LegacyFields.class.getClassLoader().getResourceAsStream("mccompat/legacy/" + versionId + "/fields.txt")) {
            if (in == null) return out;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                String[] p = line.split(" ");
                if (p.length == 5) out.computeIfAbsent(p[1], k -> new java.util.ArrayList<>()).add(new Field(p[1], p[2], p[3], p[4].equals("1")));
            }
        } catch (Exception e) {
            // no table
        }
        return out;
    }

    public static Field bySrg(LegacyVersion version, String srg) {
        return CACHE.computeIfAbsent(version, LegacyFields::load).get(srg);
    }

    private static Map<String, Field> load(LegacyVersion version) {
        Map<String, Field> map = new HashMap<>();
        String path = "mccompat/legacy/" + version.id() + "/fields.txt";
        try (InputStream in = LegacyFields.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return map;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                String[] p = line.split(" ");
                if (p.length == 5 && p[0].startsWith("field_")) map.put(p[0], new Field(p[1], p[2], p[3], p[4].equals("1")));
            }
        } catch (Exception e) {
            // no table: reflected fields are simply not synthesized
        }
        return map;
    }
}
