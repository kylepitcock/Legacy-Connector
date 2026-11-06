package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Minecraft 1.6.4's obfuscated (notch) names to SRG names. Most 1.6.4 mods were reobfuscated to SRG ids ({@code func_71857_b}), but the coremod-era ones
 * (CodeChickenCore, NEI, ChickenChunks, Ender Storage, ...) reference the game by its notch names ({@code aqz.a(Lym;)V}). The table is
 * {@code legacy/1.6.4/notch.txt}, written by {@code tools/extract_legacy_signatures.py 1.6.4} from the client jar and the MCP joined.srg:
 * <pre>
 * C notchClass mcpClass notchSuper notchInterfaces,...
 * F notchOwner notchName srgName
 * M notchOwner notchName notchDescriptor srgName
 * </pre>
 * A member is looked up on its owner and then up the owner's supertypes, which for a mod's own subclass of a vanilla class
 * come from the {@code locals} the caller supplies.
 */
public final class NotchNames {
    private static NotchNames instance;

    private final Map<String, String> classes = new HashMap<>();
    private final Map<String, String[]> supers = new HashMap<>();
    private final Map<String, String> fields = new HashMap<>();
    private final Map<String, String> methods = new HashMap<>();

    public static synchronized NotchNames get() {
        if (instance == null) instance = load();
        return instance;
    }

    /** Drops the loaded table (perf.releaseLoaderData). */
    public static synchronized void releaseCaches() {
        instance = null;
    }

    private static NotchNames load() {
        NotchNames n = new NotchNames();
        String resource = "legacyconnector/legacy/1.6.4/notch.txt";
        try (InputStream in = NotchNames.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return n;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] p = line.split(" ");
                if (p.length < 3) continue;
                switch (p[0]) {
                    case "C" -> {
                        n.classes.put(p[1], p[2]);
                        String[] ifaces = p.length > 4 && !p[4].equals("-") ? p[4].split(",") : new String[0];
                        String[] all = new String[1 + ifaces.length];
                        all[0] = p.length > 3 && !p[3].equals("-") ? p[3] : null;
                        System.arraycopy(ifaces, 0, all, 1, ifaces.length);
                        n.supers.put(p[1], all);
                    }
                    case "F" -> {
                        if (p.length >= 4) n.fields.put(p[1] + "." + p[2], p[3]);
                    }
                    case "M" -> {
                        if (p.length >= 5) n.methods.put(p[1] + "." + p[2] + p[3], p[4]);
                    }
                    default -> { }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Corrupt " + resource, e);
        }
        return n;
    }

    public boolean isEmpty() {
        return classes.isEmpty();
    }

    /** The MCP class name of a notch class name (no package), or null. */
    public String mcpClass(String notch) {
        return classes.get(notch);
    }

    /** The SRG method name of {@code owner.name desc} (notch owner and descriptor), or null. */
    public String method(String owner, String name, String desc, Map<String, String[]> locals) {
        return find(owner, name, desc, methods, locals, 0);
    }

    public String field(String owner, String name, Map<String, String[]> locals) {
        return find(owner, name, "", fields, locals, 0);
    }

    private String find(String owner, String name, String desc, Map<String, String> table, Map<String, String[]> locals, int depth) {
        if (owner == null || depth > 24) return null;
        String hit = table.get(owner + "." + name + desc);
        if (hit != null) return hit;
        String[] up = supers.get(owner);
        if (up == null && locals != null) up = locals.get(owner);
        if (up == null) return null;
        for (String s : up) {
            String r = find(s, name, desc, table, locals, depth + 1);
            if (r != null) return r;
        }
        return null;
    }
}
