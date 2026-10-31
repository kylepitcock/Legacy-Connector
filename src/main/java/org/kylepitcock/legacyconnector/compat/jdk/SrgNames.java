package org.kylepitcock.legacyconnector.compat.jdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * SRG -> MCP member names at run time, for reflection on legacy game classes.
 *
 * <p>A released 1.7.10 mod ran on SRG names ({@code func_72977_a}, {@code field_70170_p}) and, when
 * {@code FMLForgePlugin.RUNTIME_DEOBF} says so, reflects with them. The loader renames SRG ids that appear as string
 * literals, but names a mod builds or reads at run time reach {@code getDeclaredField} unchanged while the shim declares
 * MCP names. This maps them with the same tables the loader links with ({@code McpMappings}: the version's
 * {@code methods.csv}/{@code fields.csv}, the 1.7.10 MCP export as fallback). SRG ids are globally unique, so no owner is
 * needed; the caller retries the same lookup, which keeps the class hierarchy rules of the original call.</p>
 */
public final class SrgNames {
    private static final String V1710 = "1.7.10";
    private static final String V1122 = "1.12.2";

    /** Legacy version of a class: its own name, or that of a superclass or interface it inherits, decides. */
    private static final ClassValue<String> VERSION = new ClassValue<>() {
        @Override
        protected String computeValue(Class<?> type) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                String v = versionOf(c.getName());
                if (v != null) return v;
                for (Class<?> i : c.getInterfaces()) {
                    v = versionOf(i.getName());
                    if (v != null) return v;
                }
            }
            return "";
        }
    };

    private static volatile Tables v1710;
    private static volatile Tables v1122;

    private SrgNames() {}

    private record Tables(Map<String, String> methods, Map<String, String> fields) {}

    private static String versionOf(String name) {
        if (name.startsWith("legacyconnector.v1_7_10.") || name.startsWith("legacyconnector_synth.v1_7_10.")) return V1710;
        if (name.startsWith("legacyconnector.v1_12_2.") || name.startsWith("legacyconnector_synth.v1_12_2.")) return V1122;
        return null;
    }

    /** The MCP name of SRG field id {@code name} when {@code owner} is (or extends) a legacy game class, else null. */
    public static String field(Class<?> owner, String name) {
        if (!name.startsWith("field_")) return null;
        Tables t = tables(owner);
        return t == null ? null : t.fields.get(name);
    }

    /** The MCP name of SRG method id {@code name} when {@code owner} is (or extends) a legacy game class, else null. */
    public static String method(Class<?> owner, String name) {
        if (!name.startsWith("func_")) return null;
        Tables t = tables(owner);
        return t == null ? null : t.methods.get(name);
    }

    private static Tables tables(Class<?> owner) {
        if (owner == null) return null;
        String version = VERSION.get(owner);
        if (V1710.equals(version)) {
            Tables t = v1710;
            if (t == null) v1710 = t = load(V1710, true);
            return t;
        }
        if (V1122.equals(version)) {
            Tables t = v1122;
            if (t == null) v1122 = t = load(V1122, false);
            return t;
        }
        return null;
    }

    private static synchronized Tables load(String version, boolean fallback) {
        String base = "legacyconnector/mappings/" + version + "/";
        Map<String, String> methods = new HashMap<>();
        Map<String, String> fields = new HashMap<>();
        // same precedence as the loader's McpMappings: the primary table wins over the MCP export
        if (fallback) {
            read(base + "fallback-methods.csv", methods);
            read(base + "fallback-fields.csv", fields);
        }
        boolean found = read(base + "methods.csv", methods) | read(base + "fields.csv", fields);
        if (!found) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Reflection").warn("No SRG name table for {}: SRG names in reflection stay unresolved", version);
        return new Tables(methods, fields);
    }

    private static boolean read(String resource, Map<String, String> out) {
        // the tables ship with the loader (the FML service), which the game layer sees through its parent loaders
        for (ClassLoader loader : new ClassLoader[] {SrgNames.class.getClassLoader(), Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader()}) {
            if (loader == null) continue;
            try (InputStream in = loader.getResourceAsStream(resource)) {
                if (in == null) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line = reader.readLine(); // header: searge,name,side,desc
                while ((line = reader.readLine()) != null) {
                    int a = line.indexOf(',');
                    if (a <= 0) continue;
                    int b = line.indexOf(',', a + 1);
                    out.put(line.substring(0, a), b < 0 ? line.substring(a + 1) : line.substring(a + 1, b));
                }
                return true;
            } catch (IOException | RuntimeException e) {
                // try the next loader
            }
        }
        return false;
    }
}
