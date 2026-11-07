package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;

/**
 * SRG -> MCP member names for one legacy version.
 *
 * <p>Released legacy mods reference Minecraft members by their SRG ids ({@code func_149663_c},
 * {@code field_70165_t}); the shim API is written with the human readable MCP names those ids had in
 * the matching MCP stable export. SRG ids are globally unique, so the rename needs no owner context.</p>
 */
public final class McpMappings {
    public static final Pattern SRG_METHOD = Pattern.compile("func_\\d+_[a-zA-Z_]+");
    public static final Pattern SRG_FIELD = Pattern.compile("field_\\d+_[a-zA-Z_]+");

    private static final Map<LegacyVersion, McpMappings> CACHE = new EnumMap<>(LegacyVersion.class);

    /** Drops the loaded table (perf.releaseLoaderData: after discovery; it is read again if needed). */
    public static synchronized void releaseCaches() {
        CACHE.clear();
    }

    private final Map<String, String> methods;
    private final Map<String, String> fields;

    public McpMappings(Map<String, String> methods, Map<String, String> fields) {
        this.methods = methods;
        this.fields = fields;
    }

    public static synchronized McpMappings forVersion(LegacyVersion version) {
        return CACHE.computeIfAbsent(version, v -> {
            // 1.4.7's export names fewer members than the versions after it; an id it lacks takes the name of the next version that has one (SRG ids are stable from 1.4 on)
            String[] chain = v == LegacyVersion.V1_4_7 ? new String[] {"1.7.10", "1.6.4", "1.5.2", v.id()} : new String[] {v.id()};
            Map<String, String> methods = new HashMap<>();
            Map<String, String> fields = new HashMap<>();
            for (String id : chain) {
                String base = "/legacyconnector/mappings/" + id + "/";
                methods.putAll(readCsv(base + "fallback-methods.csv"));
                fields.putAll(readCsv(base + "fallback-fields.csv"));
                methods.putAll(readCsv(base + "methods.csv")); // primary table wins
                fields.putAll(readCsv(base + "fields.csv"));
            }
            return new McpMappings(methods, fields);
        });
    }

    public String method(String name) {
        return name.startsWith("func_") ? methods.getOrDefault(name, name) : name;
    }

    public String field(String name) {
        return name.startsWith("field_") ? fields.getOrDefault(name, name) : name;
    }

    /** Maps a string that is exactly one SRG id (as used with reflection helpers); otherwise returns it unchanged. */
    public String anyMember(String value) {
        if (value.startsWith("func_") && SRG_METHOD.matcher(value).matches()) return method(value);
        if (value.startsWith("field_") && SRG_FIELD.matcher(value).matches()) return field(value);
        return value;
    }

    public int size() {
        return methods.size() + fields.size();
    }

    private static Map<String, String> readCsv(String resource) {
        Map<String, String> out = new HashMap<>();
        InputStream in = McpMappings.class.getResourceAsStream(resource);
        if (in == null) {
            // Missing data degrades to "no renames": mods built against MCP names (dev jars) still work.
            return out;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header: searge,name,side,desc
            while ((line = reader.readLine()) != null) {
                int a = line.indexOf(',');
                if (a <= 0) continue;
                int b = line.indexOf(',', a + 1);
                out.put(line.substring(0, a), b < 0 ? line.substring(a + 1) : line.substring(a + 1, b));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Corrupt mapping resource " + resource, e);
        }
        return out;
    }
}
