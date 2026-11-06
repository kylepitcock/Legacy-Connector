package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData;

/**
 * Member (method and field) renames of a forward version (see {@link LegacyVersion#forward()}): the name a 1.6.4 call site uses
 * (the MCP name of its SRG id) to the name the 1.7.10 shim declares ({@code onInventoryChanged} to {@code markDirty},
 * {@code getBlockTileEntity} to {@code getTileEntity}). Tables, later files winning:
 * <pre>
 * forward/&lt;id&gt;/member-heuristic.tsv  unique same-descriptor pairs (tools/gen_forward_164.py)
 * forward/&lt;id&gt;/member-renames.tsv    same SRG id, other name (tools/gen_forward_164.py)
 * forward/&lt;id&gt;/member-manual.tsv     hand written
 * </pre>
 * Lines (tab separated, 1.6.4 names): {@code M owner name desc newName} and {@code F owner name newName}. The owner is the declaring
 * class; a call through any subtype matches, the lookup walks the 1.6.4 supertypes ({@code supers}), so a mod's own subclass of
 * {@code TileEntity} that declares or calls {@code onInventoryChanged()} is renamed as well.
 */
public final class ForwardMembers {
    private static final Map<LegacyVersion, ForwardMembers> CACHE = new EnumMap<>(LegacyVersion.class);
    private static final String[] FILES = {"member-heuristic.tsv", "member-renames.tsv", "member-manual.tsv"};

    /** {@code owner.name+desc} to the new name. */
    private final Map<String, String> methods = new HashMap<>();
    /** {@code owner.name} to the new name. */
    private final Map<String, String> fields = new HashMap<>();
    /** Call-site rewrites (the manual table's R, G and C lines) by {@code owner.name+desc}, {@code owner.field}, {@code owner.<init>+desc}. */
    private final Map<String, String[]> calls = new HashMap<>();
    private final Map<String, String[]> fieldCalls = new HashMap<>();
    private final Map<String, String[]> ctors = new HashMap<>();
    private final LegacyVersion version;

    private ForwardMembers(LegacyVersion version) {
        this.version = version;
    }

    public static synchronized ForwardMembers forVersion(LegacyVersion version) {
        return CACHE.computeIfAbsent(version, ForwardMembers::load);
    }

    /** Drops the loaded table (perf.releaseLoaderData). */
    public static synchronized void releaseCaches() {
        CACHE.clear();
    }

    /** For tests: a table from lines. */
    public static ForwardMembers of(LegacyVersion version, List<String> lines) {
        ForwardMembers t = new ForwardMembers(version);
        for (String line : lines) t.parse(line);
        return t;
    }

    private static ForwardMembers load(LegacyVersion v) {
        ForwardMembers t = new ForwardMembers(v);
        if (v.forward() == null) return t;
        for (String file : FILES) {
            String resource = "legacyconnector/forward/" + v.id() + "/" + file;
            try (InputStream in = ForwardMembers.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) t.parse(line);
            } catch (IOException e) {
                throw new IllegalStateException("Corrupt rename table " + resource, e);
            }
        }
        return t;
    }

    private void parse(String line) {
        if (line.isBlank() || line.startsWith("#")) return;
        String[] p = line.stripTrailing().split("\t");
        if (p[0].equals("M") && p.length >= 5) methods.put(p[1] + "." + p[2] + p[3], p[4].strip());
        else if (p[0].equals("F") && p.length >= 4) fields.put(p[1] + "." + p[2], p[3].strip());
        else if (p[0].equals("R") && p.length >= 5) calls.put(p[1] + "." + p[2] + p[3], p);
        else if (p[0].equals("G") && p.length >= 4) fieldCalls.put(p[1] + "." + p[2], p);
        else if (p[0].equals("C") && p.length >= 4) ctors.put(p[1] + ".<init>" + p[2], p);
    }

    /** All call-site rewrite rules, keyed as above (see the header of member-manual.tsv for the line formats). */
    public Map<String, String[]> callRules() {
        return calls;
    }

    public Map<String, String[]> fieldCallRules() {
        return fieldCalls;
    }

    public Map<String, String[]> constructorRules() {
        return ctors;
    }

    public int methodCount() {
        return methods.size();
    }

    public int fieldCount() {
        return fields.size();
    }

    public Map<String, String> methodRules() {
        return methods;
    }

    public Map<String, String> fieldRules() {
        return fields;
    }

    public boolean isEmpty() {
        return methods.isEmpty() && fields.isEmpty() && calls.isEmpty() && fieldCalls.isEmpty() && ctors.isEmpty();
    }

    /**
     * The 1.7.10 name of {@code owner.name desc} (1.6.4 MCP names), or {@code name} when it is not renamed.
     *
     * @param supers supertypes of a class in 1.6.4 terms ({@code [super, interfaces...]}, entries may be null), or null for the
     *               class itself having none known; the table of 1.6.4 classes is consulted first
     */
    public String method(String owner, String name, String desc, Function<String, String[]> supers) {
        if (methods.isEmpty()) return name;
        return find(owner, name + desc, methods, supers, name);
    }

    public String field(String owner, String name, Function<String, String[]> supers) {
        if (fields.isEmpty()) return name;
        return find(owner, name, fields, supers, name);
    }

    private String find(String owner, String key, Map<String, String> table, Function<String, String[]> extra, String unchanged) {
        LegacyHierarchyData data = LegacyHierarchyData.forVersion(version);
        ArrayDeque<String> todo = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        todo.add(owner);
        while (!todo.isEmpty() && seen.size() < 64) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) continue;
            String hit = table.get(c + "." + key);
            if (hit != null) return hit;
            var entry = data.get(c);
            if (entry != null) {
                if (entry.superName() != null) todo.add(entry.superName());
                todo.addAll(entry.interfaces());
            } else if (extra != null) {
                String[] up = extra.apply(c);
                if (up != null) for (String s : up) if (s != null) todo.add(s);
            }
        }
        return unchanged;
    }
}
