package org.kylepitcock.legacyconnector.loader.transform;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Forge 1.20.1 shipped its own access transformers that opened many private Minecraft members (a mod's code reads {@code Minecraft.textureManager}, calls
 * {@code Level.getEntities()} and so on without a transformer of its own); NeoForge 1.21.1 opens a different set. A reference to a private or package-private
 * member of a game class that the jar does not declare itself becomes a line of the jar's access transformer file, so the reference links. The members come
 * from {@code mc-private.index.gz} (written by {@code McMembers1201Tool}).
 */
final class PrivateAccess1201 {
    private PrivateAccess1201() {}

    /** The access transformer lines (sorted, unique) for the references of these classes. */
    static Set<String> lines(List<ClassNode> nodes, Mc1201Hierarchy h) {
        Set<String> out = new TreeSet<>();
        Set<String> own = new java.util.HashSet<>();
        for (ClassNode n : nodes) own.add(n.name);
        for (ClassNode n : nodes) {
            for (MethodNode m : n.methods) {
                if (m.instructions == null) continue;
                for (AbstractInsnNode in : m.instructions) {
                    if (in instanceof FieldInsnNode f && f.owner.startsWith("net/minecraft/") && !own.contains(f.owner)) {
                        String declaring = declaring(h, f.owner, f.name + ":" + f.desc, true);
                        if (declaring == null) continue;
                        McMembers1201.Members hidden = McMembers1201.hidden(declaring);
                        if (hidden == null) continue;
                        String key = f.name + ":" + f.desc;
                        boolean isHidden = hidden.fields().contains(key);
                        boolean isHiddenFinal = hidden.fields().contains(key + "#f");
                        if (!isHidden && !isHiddenFinal) continue;
                        boolean write = f.getOpcode() == Opcodes.PUTFIELD || f.getOpcode() == Opcodes.PUTSTATIC;
                        out.add((write && isHiddenFinal ? "public-f " : "public ") + declaring.replace('/', '.') + " " + f.name);
                    } else if (in instanceof MethodInsnNode mi && mi.owner.startsWith("net/minecraft/") && !own.contains(mi.owner)
                            && mi.getOpcode() != Opcodes.INVOKEINTERFACE) {
                        String key = mi.name + mi.desc;
                        String declaring = declaring(h, mi.owner, key, false);
                        if (declaring == null) continue;
                        McMembers1201.Members hidden = McMembers1201.hidden(declaring);
                        if (hidden == null || !hidden.methods().contains(key)) continue;
                        out.add("public " + declaring.replace('/', '.') + " " + key);
                    }
                }
            }
        }
        return out;
    }

    /** The first class up the super chain that declares the member (JVM resolution), or null. */
    private static String declaring(Mc1201Hierarchy h, String owner, String key, boolean field) {
        for (int depth = 0; owner != null && depth < 24; depth++) {
            McMembers1201.Members members = McMembers1201.get(owner);
            if (members != null && (field ? members.fields() : members.methods()).contains(key)) return owner;
            List<String> supers = h.supers(owner);
            owner = supers.isEmpty() ? null : supers.get(0);
        }
        return null;
    }
}
