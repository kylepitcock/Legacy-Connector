package org.kylepitcock.legacyconnector.loader.shim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal structural view of a class: hierarchy plus member signatures. Used for link checking and for
 * resolving which class actually declares a member referenced through a subclass.
 */
public final class ClassInfo {
    public final String name;
    public final String superName;
    public final List<String> interfaces;
    public final int access;
    /** Keyed by {@code name + descriptor}. Value is the member access flags. */
    public final Map<String, Integer> methods = new HashMap<>();
    /** Keyed by {@code name + ":" + descriptor}. */
    public final Map<String, Integer> fields = new HashMap<>();

    public ClassInfo(String name, String superName, List<String> interfaces, int access) {
        this.name = name;
        this.superName = superName;
        this.interfaces = interfaces;
        this.access = access;
    }

    public static ClassInfo of(org.objectweb.asm.tree.ClassNode node) {
        ClassInfo info = new ClassInfo(node.name, node.superName, List.copyOf(node.interfaces), node.access);
        for (var f : node.fields) info.fields.put(f.name + ":" + f.desc, f.access);
        for (var m : node.methods) info.methods.put(m.name + m.desc, m.access);
        return info;
    }

    public boolean hasMethod(String name, String desc) {
        return methods.containsKey(name + desc);
    }

    public boolean hasField(String name, String desc) {
        return fields.containsKey(name + ":" + desc);
    }

    /** Field names only; virtual-field accessors are matched by name and owner type. */
    public boolean hasFieldNamed(String name) {
        String prefix = name + ":";
        for (String key : fields.keySet()) {
            if (key.startsWith(prefix)) return true;
        }
        return false;
    }
}
