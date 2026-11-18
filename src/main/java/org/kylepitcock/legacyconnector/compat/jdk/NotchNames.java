package org.kylepitcock.legacyconnector.compat.jdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notch (obfuscated) member names at run time, for reflection on legacy game classes of the id-based versions (1.4.7, 1.5.2, 1.6.4).
 *
 * <p>Coremod-era libraries address the game by its notch names ({@code ObfMapping("asp", "b", "Ljava/util/Map;")}: ForgeMultipart reads
 * {@code TileEntity.classToNameMap} that way) and, because the game they run on is obfuscated, take the name as is. The shim declares MCP names, so
 * a lookup that fails is translated: owner class (by the shim class name) to its notch class, notch member to its SRG id, SRG id to the MCP name,
 * with the loader's own tables ({@code legacy/<version>/notch.txt}, {@code mappings/<version>/*.csv}). Only a failed lookup reaches here and each
 * step is a hash lookup.</p>
 */
public final class NotchNames {
    private static final String[] VERSIONS = {"1.6.4", "1.5.2", "1.4.7"};
    private static final Map<String, NotchNames> LOADED = new HashMap<>();

    /** mcp class (slash form, packaged or flat) to notch class */
    private final Map<String, String> classes = new HashMap<>();
    private final Map<String, String> fields = new HashMap<>();
    /** "notchOwner.notchName" to the srg names of every overload */
    private final Map<String, List<String>> methods = new HashMap<>();
    private final Map<String, String> srgFields = new HashMap<>();
    private final Map<String, String> srgMethods = new HashMap<>();

    private NotchNames() {}

    private static synchronized NotchNames of(String version) {
        return LOADED.computeIfAbsent(version, NotchNames::load);
    }

    /** The MCP name of the member the notch name {@code name} stands for on {@code owner} (a legacy game class or a subclass), or null. */
    public static String field(Class<?> owner, String name) {
        for (NotchNames t : tables()) {
            for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
                String srg = t.fields.get(t.notchOf(c) + "." + name);
                if (srg != null) {
                    String mcp = t.srgFields.get(srg);
                    if (mcp != null) return mcp;
                }
            }
        }
        return null;
    }

    /** The MCP names an overload of the notch method name {@code name} may have on {@code owner}, or an empty list. */
    public static List<String> methods(Class<?> owner, String name) {
        List<String> out = new ArrayList<>(1);
        for (NotchNames t : tables()) {
            for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
                List<String> srgs = t.methods.get(t.notchOf(c) + "." + name);
                if (srgs == null) continue;
                for (String srg : srgs) {
                    String mcp = t.srgMethods.get(srg);
                    if (mcp != null && !out.contains(mcp)) out.add(mcp);
                }
            }
        }
        return out;
    }

    private static volatile NotchNames[] active;

    private static NotchNames[] tables() {
        NotchNames[] a = active;
        if (a != null) return a;
        List<NotchNames> list = new ArrayList<>();
        java.util.Set<String> present = new java.util.HashSet<>();
        for (var m : org.kylepitcock.legacyconnector.core.LegacyMods.all()) present.add(m.version);
        for (String v : VERSIONS) if (present.contains(v)) list.add(of(v));
        a = list.toArray(new NotchNames[0]);
        // the mod set is complete once mods run; before that an empty answer must not stick
        if (a.length > 0) active = a;
        return a;
    }

    /** The notch class name of a shim or synthesized game class, or a name no table has. */
    private String notchOf(Class<?> c) {
        String n = c.getName();
        String rel = null;
        for (String p : LegacyPrefixes.API_1710) {
            if (n.startsWith(p)) {
                rel = n.substring(p.length());
                break;
            }
        }
        if (rel == null) return "?";
        String slash = rel.replace('.', '/');
        String hit = classes.get(slash);
        if (hit != null) return hit;
        // 1.4.7 lists the flat net/minecraft/src names
        int i = slash.lastIndexOf('/');
        hit = classes.get("net/minecraft/src/" + slash.substring(i + 1));
        return hit == null ? "?" : hit;
    }

    private static NotchNames load(String version) {
        NotchNames n = new NotchNames();
        String base = "legacyconnector/";
        read(base + "legacy/" + version + "/notch.txt", line -> {
            String[] p = line.split(" ");
            if (p.length < 4 && !(p.length == 3 && p[0].equals("C"))) return;
            switch (p[0]) {
                case "C" -> n.classes.put(p[2], p[1]);
                case "F" -> n.fields.put(p[1] + "." + p[2], p[3]);
                case "M" -> {
                    if (p.length >= 5) n.methods.computeIfAbsent(p[1] + "." + p[2], k -> new ArrayList<>(1)).add(p[4]);
                }
                default -> { }
            }
        });
        read(base + "mappings/" + version + "/fields.csv", line -> csv(line, n.srgFields));
        read(base + "mappings/" + version + "/methods.csv", line -> csv(line, n.srgMethods));
        return n;
    }

    private static void csv(String line, Map<String, String> out) {
        int a = line.indexOf(',');
        if (a <= 0) return;
        int b = line.indexOf(',', a + 1);
        out.put(line.substring(0, a), b < 0 ? line.substring(a + 1) : line.substring(a + 1, b));
    }

    private static void read(String resource, java.util.function.Consumer<String> sink) {
        // the tables ship with the loader (the FML service), which the game layer sees through its parent loaders
        for (ClassLoader loader : new ClassLoader[] {NotchNames.class.getClassLoader(), Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader()}) {
            if (loader == null) continue;
            try (InputStream in = loader.getResourceAsStream(resource)) {
                if (in == null) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) sink.accept(line);
                return;
            } catch (IOException | RuntimeException e) {
                // try the next loader
            }
        }
    }
}
