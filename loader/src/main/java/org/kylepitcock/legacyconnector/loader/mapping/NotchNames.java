package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Minecraft 1.6.4's (and 1.5.2's, same layout) obfuscated (notch) names to SRG names. Most 1.6.4 mods were reobfuscated to SRG ids ({@code func_71857_b}), but the coremod-era ones
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
    private static final java.util.Map<String, NotchNames> INSTANCES = new HashMap<>();

    private final Map<String, String> classes = new HashMap<>();
    private final Map<String, String[]> supers = new HashMap<>();
    /** MCP class name to notch name, for walking up from a Forge class (not obfuscated) into the vanilla interfaces it extends. */
    private final Map<String, String> byMcp = new HashMap<>();
    private org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData hierarchy;
    private final Map<String, String> fields = new HashMap<>();
    private final Map<String, String> methods = new HashMap<>();

    /** The 1.6.4 table. */
    public static NotchNames get() {
        return get("1.6.4");
    }

    /** The table of a version with notch-named jars: {@code legacy/<id>/notch.txt} (1.5.2 and 1.6.4). */
    public static synchronized NotchNames get(String versionId) {
        return INSTANCES.computeIfAbsent(versionId, NotchNames::load);
    }

    /** Drops the loaded table (perf.releaseLoaderData). */
    public static synchronized void releaseCaches() {
        INSTANCES.clear();
    }

    private static NotchNames load(String versionId) {
        NotchNames n = new NotchNames();
        String resource = "legacyconnector/legacy/" + versionId + "/notch.txt";
        try (InputStream in = NotchNames.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return n;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] p = line.split(" ");
                if (p.length < 3) continue;
                switch (p[0]) {
                    case "C" -> {
                        n.classes.put(p[1], p[2]);
                        n.byMcp.put(p[2], p[1]);
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
        for (org.kylepitcock.legacyconnector.loader.LegacyVersion v : org.kylepitcock.legacyconnector.loader.LegacyVersion.values()) {
            if (v.id().equals(versionId)) {
                n.hierarchy = org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData.forVersion(v);
                if (v.flatNames()) {
                    // the hierarchy data uses packaged names; this table lists the flat net/minecraft/src names (1.4.7): index both
                    for (Map.Entry<String, String> e : new java.util.ArrayList<>(n.byMcp.entrySet())) {
                        String packaged = org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames.flat(v, e.getKey());
                        if (!packaged.equals(e.getKey())) n.byMcp.putIfAbsent(packaged, e.getValue());
                    }
                }
            }
        }
        return n;
    }

    /** The supertypes of a class this table does not list (a Forge class such as ISidedInventory), in notch names where they have one: from the version's API data. */
    private String[] upFromHierarchy(String owner) {
        if (hierarchy == null || owner.indexOf('/') < 0 && classes.containsKey(owner)) return null;
        var entry = hierarchy.get(owner);
        if (entry == null) return null;
        java.util.List<String> all = new java.util.ArrayList<>();
        if (entry.superName() != null) all.add(entry.superName());
        all.addAll(entry.interfaces());
        String[] out = new String[all.size()];
        for (int i = 0; i < out.length; i++) out[i] = byMcp.getOrDefault(all.get(i), all.get(i));
        return out;
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
        if (up == null) up = upFromHierarchy(owner);
        if (up == null) return null;
        for (String s : up) {
            String r = find(s, name, desc, table, locals, depth + 1);
            if (r != null) return r;
        }
        return null;
    }
}
