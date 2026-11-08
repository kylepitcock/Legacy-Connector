package org.kylepitcock.legacyconnector.vb1_7_3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection of b1.7.3 mods on vanilla classes. ModLoader's {@code getPrivateValue(Class, Object, int)} takes the index of a declared
 * field and {@code getPrivateValue(Class, Object, String)} its obfuscated name (IndustrialCraft 2 reads {@code BlockFire}'s field 0, the
 * fire-spread table; Portal Gun writes {@code Achievement}'s field 3). The vanilla classes here are the shim's or the synthesizer's, with
 * other fields and names, so both are looked up in b1.7.3's own field list ({@code field-order.tsv}: declaration order, readable name,
 * client and server obfuscated names) and the field is then found by its readable name.
 */
public final class ReflectionB173 {
    private static final String SHIM = "legacyconnector.vb1_7_3.";
    private static final String SYNTH = "legacyconnector_synth.vb1_7_3.";

    /** One declared field of a b1.7.3 class: readable, client and server name. */
    private record Entry(String named, String client, String server) {}

    private static volatile Map<String, List<Entry>> fields;

    private ReflectionB173() {}

    /** The b1.7.3 class name ({@code net/minecraft/block/FireBlock}) of a shim or synthesized class, or null. */
    static String legacyName(Class<?> type) {
        String n = type.getName();
        if (n.startsWith(SHIM)) return n.substring(SHIM.length()).replace('.', '/');
        if (n.startsWith(SYNTH)) return n.substring(SYNTH.length()).replace('.', '/');
        return null;
    }

    /** The field {@code getDeclaredFields()[index]} was in b1.7.3; for mod classes, the class's own declared field at that index. */
    public static Field field(Class<?> owner, int index) throws NoSuchFieldException {
        String legacy = legacyName(owner);
        if (legacy != null) {
            List<Entry> list = table().get(legacy);
            if (list != null && index >= 0 && index < list.size()) return byName(owner, list.get(index).named());
        }
        Field[] declared = owner.getDeclaredFields();
        if (index < 0 || index >= declared.length) throw new NoSuchFieldException(owner.getName() + " has no field #" + index);
        return declared[index];
    }

    /** A declared field by its readable or obfuscated (client or server) b1.7.3 name, or a mod class's own field of that name. */
    public static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        String legacy = legacyName(owner);
        if (legacy != null) {
            List<Entry> list = table().get(legacy);
            if (list != null) {
                for (Entry e : list) if (e.named().equals(name)) return byName(owner, e.named());
                for (Entry e : list) if (e.client().equals(name) || e.server().equals(name)) return byName(owner, e.named());
            }
        }
        return owner.getDeclaredField(name);
    }

    /** The field of that name on the class or the shim/synthesized classes above it (a field may live on a superclass here). */
    private static Field byName(Class<?> owner, String named) throws NoSuchFieldException {
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(named);
            } catch (NoSuchFieldException e) {
                if (legacyName(c) == null) break;
            }
        }
        throw new NoSuchFieldException(owner.getName() + "." + named + " (the layer does not provide this b1.7.3 field yet)");
    }

    private static Map<String, List<Entry>> table() {
        Map<String, List<Entry>> t = fields;
        if (t != null) return t;
        synchronized (ReflectionB173.class) {
            if (fields != null) return fields;
            t = new HashMap<>();
            try (InputStream in = ReflectionB173.class.getResourceAsStream("/legacyconnector/b1.7.3/field-order.tsv")) {
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    for (String l = r.readLine(); l != null; l = r.readLine()) {
                        if (l.isEmpty() || l.charAt(0) == '#') continue;
                        String[] p = l.split("\t");
                        List<Entry> list = new ArrayList<>(p.length - 1);
                        for (int i = 1; i < p.length; i++) {
                            String[] n = p[i].split("\\|", -1);
                            list.add(new Entry(n[0], n.length > 1 ? n[1] : "-", n.length > 2 ? n[2] : "-"));
                        }
                        t.put(p[0], list);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("cannot read field-order.tsv", e);
            }
            return fields = t;
        }
    }
}
