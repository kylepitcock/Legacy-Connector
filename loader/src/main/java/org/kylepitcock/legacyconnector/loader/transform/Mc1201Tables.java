package org.kylepitcock.legacyconnector.loader.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * The Minecraft 1.20.1 to 1.21.1 migration tables ({@code legacyconnector/forward/1.20.1/mc-*.tsv}, formats in each file's header):
 * <ul>
 *   <li>{@code mc-classes.tsv}: class renames;</li>
 *   <li>{@code mc-members.tsv}: method and field renames;</li>
 *   <li>{@code mc-calls.tsv}: calls (and constructors) redirected to a static helper in {@link #HELPER_PACKAGE};</li>
 *   <li>{@code mc-fields.tsv}: static fields whose name or type changed;</li>
 *   <li>{@code mc-bridges.tsv}: methods a mod overrides whose signature changed (bridge method generation).</li>
 * </ul>
 */
final class Mc1201Tables {
    static final String HELPER_PACKAGE = "org/kylepitcock/legacyconnector/v1_20_1/migrate/";
    private static final String DIR = "legacyconnector/forward/1.20.1/";

    /** A call redirected to {@code helper.helperName helperDesc} (the receiver is the first parameter of an instance method). */
    record Call(String owner, String name, String desc, String helper, String helperName, String helperDesc) {}
    // helperDesc null: derived at the call site (receiver first for instance calls, the constructed object returned for constructors)

    /** A static field read: {@code GETSTATIC owner.newName:newDesc}, then {@code post}: none, {@code value} (Holder.value + cast) or a helper method. */
    record Field(String owner, String name, String desc, String newName, String newDesc, String post) {}

    /** A changed override; argument tokens: index into the new parameters, {@code this}, or {@code Helper.method@index}. */
    record Bridge(String owner, String oldName, String oldDesc, String newName, String newDesc, List<String> args, String ret, int remember) {}

    final Map<String, String> classes = new HashMap<>();
    /** key: owner.name+desc (desc "*" for any) to the new name. */
    final Map<String, String> members = new HashMap<>();
    final Map<String, Call> calls = new HashMap<>();
    final Map<String, Field> fields = new HashMap<>();
    final List<Bridge> bridges = new ArrayList<>();
    /** Overrides of methods that became final in 1.21.1: owner, name, descriptor, new name ({@code -}: no replacement, the override is disabled). */
    final List<String[]> overrides = new ArrayList<>();

    private Map<String, Call> specialByDesc;

    /** The {@code @special} (super call) rule for a method name and descriptor whatever its owner, if exactly one owner has one; else null. */
    synchronized Call specialRuleFor(String name, String desc) {
        if (specialByDesc == null) {
            Map<String, Call> m = new HashMap<>();
            Set<String> ambiguous = new HashSet<>();
            for (Map.Entry<String, Call> e : calls.entrySet()) {
                String k = e.getKey();
                if (!k.endsWith("@special") || e.getValue().helper().startsWith("=")) continue;
                String key = k.substring(k.indexOf('.') + 1, k.length() - "@special".length());
                if (m.putIfAbsent(key, e.getValue()) != null) ambiguous.add(key);
            }
            for (String a : ambiguous) m.remove(a);
            specialByDesc = m;
        }
        return specialByDesc.get(name + desc);
    }

    private Set<String> watched;

    /** Names of methods the tables mention (calls, overrides and bridges): what another jar's class must say about itself so subclasses can be judged. */
    synchronized Set<String> watchedMethodNames() {
        if (watched != null) return watched;
        Set<String> out = new HashSet<>();
        for (String k : calls.keySet()) {
            int dot = k.indexOf('.');
            int paren = k.indexOf('(');
            if (dot > 0 && paren > dot) out.add(k.substring(dot + 1, paren));
        }
        for (Bridge b : bridges) {
            out.add(b.oldName());
            out.add(b.newName());
        }
        for (String[] o : overrides) out.add(o[1]);
        return watched = out;
    }

    private static volatile Mc1201Tables instance;

    static Mc1201Tables get() {
        Mc1201Tables t = instance;
        if (t == null) {
            synchronized (Mc1201Tables.class) {
                if ((t = instance) == null) instance = t = load();
            }
        }
        return t;
    }

    static Mc1201Tables load() {
        Mc1201Tables t = new Mc1201Tables();
        for (String[] f : rows("mc-classes.tsv", 2)) t.classes.put(f[0], f[1]);
        for (String[] f : rows("mc-members.tsv", 4)) t.members.put(f[0] + "." + f[1] + f[2], f[3]);
        for (String[] f : rows("mc-calls.tsv", 5)) {
            String helper = f[3].contains("/") || f[3].startsWith("=") ? f[3] : HELPER_PACKAGE + f[3];
            String hdesc = f.length > 5 && !f[5].equals("-") ? f[5] : null;
            t.calls.put(f[0] + "." + f[1] + f[2], new Call(f[0], f[1], f[2], helper, f[4], hdesc));
        }
        for (String[] f : rows("mc-fields.tsv", 6)) t.fields.put(f[0] + "." + f[1] + ":" + f[2], new Field(f[0], f[1], f[2], f[3], f[4], f[5]));
        t.overrides.addAll(rows("mc-overrides.tsv", 4));
        for (String[] f : rows("mc-bridges.tsv", 8)) {
            List<String> args = f[5].equals("-") ? List.of() : List.of(f[5].split(","));
            t.bridges.add(new Bridge(f[0], f[1], f[2], f[3], f[4], args, f[6].equals("-") ? null : f[6], f[7].equals("-") ? -1 : Integer.parseInt(f[7])));
        }
        return t;
    }

    /**
     * The 1.21.1 name of a method or field (owner in the 1.20.1 class name, name already Mojang's). A field is looked up with ":" and its descriptor,
     * a method with its descriptor; the descriptor {@code *} (fields {@code :*}) matches any.
     */
    String renamedMember(String owner, String name, String desc) {
        if (members.isEmpty()) return name;
        String r = members.get(owner + "." + name + desc);
        if (r == null) r = members.get(owner + "." + name + (desc.startsWith(":") ? ":*" : "*"));
        return r != null ? r : name;
    }

    /** The rows of {@code mc-X.tsv} followed by those of {@code lib-X.tsv} (the 1.20.1 library bridges: same formats, kept in their own files). */
    private static List<String[]> rows(String file, int min) {
        List<String[]> out = rows1(file, min);
        if (file.startsWith("mc-")) out.addAll(rows1("lib-" + file.substring(3), min));
        return out;
    }

    private static List<String[]> rows1(String file, int min) {
        List<String[]> out = new ArrayList<>();
        try (InputStream in = Mc1201Tables.class.getClassLoader().getResourceAsStream(DIR + file)) {
            if (in == null) return out;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String l = r.readLine(); l != null; l = r.readLine()) {
                if (l.isBlank() || l.startsWith("#")) continue;
                String[] f = l.strip().split("\t", -1);
                if (f.length < min) throw new IllegalStateException("Corrupt row in " + file + ": " + l);
                out.add(f);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Corrupt table " + file, e);
        }
        return out;
    }
}
