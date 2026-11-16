package org.kylepitcock.legacyconnector.loader.transform;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Forge 1.20.1 shipped its own access transformers that opened many private Minecraft members (a mod's code reads {@code Minecraft.textureManager}, calls
 * {@code Level.getEntities()} and so on without a transformer of its own); NeoForge 1.21.1 opens a different set. A reference to a private or package-private
 * member of a game class that the jar does not declare itself becomes a line of the jar's access transformer file, so the reference links.
 */
final class PrivateAccess1201 {
    private PrivateAccess1201() {}

    /** The access transformer lines (sorted, unique) for the references of these classes. */
    static Set<String> lines(List<ClassNode> nodes, Hierarchy h) {
        Set<String> out = new TreeSet<>();
        Set<String> own = new java.util.HashSet<>();
        for (ClassNode n : nodes) own.add(n.name);
        for (ClassNode n : nodes) {
            for (MethodNode m : n.methods) {
                if (m.instructions == null) continue;
                for (AbstractInsnNode in : m.instructions) {
                    if (in instanceof FieldInsnNode f && f.owner.startsWith("net/minecraft/") && !own.contains(f.owner)) {
                        Found found = find(h, f.owner, f.name + ":" + f.desc, true);
                        if (found == null || (found.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) continue;
                        boolean write = f.getOpcode() == Opcodes.PUTFIELD || f.getOpcode() == Opcodes.PUTSTATIC;
                        out.add((write && (found.access & Opcodes.ACC_FINAL) != 0 ? "public-f " : "public ") + found.owner.replace('/', '.') + " " + f.name);
                    } else if (in instanceof MethodInsnNode mi && mi.owner.startsWith("net/minecraft/") && !own.contains(mi.owner)
                            && mi.getOpcode() != Opcodes.INVOKEINTERFACE) {
                        Found found = find(h, mi.owner, mi.name + mi.desc, false);
                        if (found == null || (found.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) continue;
                        out.add("public " + found.owner.replace('/', '.') + " " + mi.name + mi.desc);
                    }
                }
            }
        }
        return out;
    }

    private record Found(String owner, int access) {}

    private static Found find(Hierarchy h, String owner, String key, boolean field) {
        for (int depth = 0; owner != null && depth < 24; depth++) {
            ClassInfo info = h.find(owner);
            if (info == null) return null;
            Integer a = (field ? info.fields : info.methods).get(key);
            if (a != null) return new Found(owner, a);
            owner = info.superName;
        }
        return null;
    }
}
