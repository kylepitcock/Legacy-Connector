package org.kylepitcock.mccompat.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.kylepitcock.mccompat.loader.LegacyVersion;

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

    private final Map<String, String> methods;
    private final Map<String, String> fields;

    public McpMappings(Map<String, String> methods, Map<String, String> fields) {
        this.methods = methods;
        this.fields = fields;
    }

    public static synchronized McpMappings forVersion(LegacyVersion version) {
        return CACHE.computeIfAbsent(version, v -> {
            String base = "/mccompat/mappings/" + v.id() + "/";
            Map<String, String> methods = readCsv(base + "fallback-methods.csv");
            Map<String, String> fields = readCsv(base + "fallback-fields.csv");
            methods.putAll(readCsv(base + "methods.csv")); // primary table wins
            fields.putAll(readCsv(base + "fields.csv"));
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
