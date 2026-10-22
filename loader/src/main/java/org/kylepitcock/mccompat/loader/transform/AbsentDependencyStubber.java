package org.kylepitcock.mccompat.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Integrations with mods that are not installed (IC2, Applied Energistics, ...) usually live in methods that are only
 * called when the mod is present. The JVM verifies a whole class at once, though, and verifying such a method may
 * require loading the absent API (e.g. to check an argument is assignable to {@code IRecipeInput}), so the entire
 * class fails to load. Methods whose code references classes that exist nowhere get the reference replaced by
 * {@code throw new NoClassDefFoundError(<class>)} from the first such reference on: the class verifies, and only that
 * path fails - lazily, as the mod expected. Methods that catch Throwable/Error/LinkageError themselves are left alone.
 */
public final class AbsentDependencyStubber {
    private AbsentDependencyStubber() {}

    public static int apply(ClassNode node, Hierarchy hierarchy) {
        int stubbed = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions.size() == 0 || guarded(m)) continue;
            if (stubMethod(m, hierarchy)) stubbed++;
        }
        return stubbed;
    }

    /**
     * Replaces each instruction that references an absent class by the throw, and drops the straight-line code that
     * followed it (up to the next stack map frame, i.e. the next jump target). The code before it stays, so the
     * mod's own "is the other mod loaded" guards (an early return) keep working.
     */
    private static boolean stubMethod(MethodNode m, Hierarchy hierarchy) {
        boolean changed = false;
        AbstractInsnNode insn = m.instructions.getFirst();
        while (insn != null) {
            String missing = insn.getOpcode() >= 0 ? missingIn(insn, hierarchy) : null;
            if (missing == null) {
                insn = insn.getNext();
                continue;
            }
            changed = true;
            InsnList body = new InsnList();
            body.add(new TypeInsnNode(Opcodes.NEW, "java/lang/NoClassDefFoundError"));
            body.add(new InsnNode(Opcodes.DUP));
            body.add(new LdcInsnNode(missing));
            body.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/NoClassDefFoundError", "<init>", "(Ljava/lang/String;)V", false));
            body.add(new InsnNode(Opcodes.ATHROW));
            AbstractInsnNode next = insn.getNext();
            m.instructions.insert(insn, body);
            m.instructions.remove(insn);
            // Dead tail: real instructions until a frame (labels and line numbers stay; try/catch ranges use them).
            while (next != null && !(next instanceof FrameNode)) {
                AbstractInsnNode after = next.getNext();
                if (next.getOpcode() >= 0) m.instructions.remove(next);
                next = after;
            }
            insn = next;
        }
        if (changed) {
            m.localVariables = null;
            m.visibleLocalVariableAnnotations = null;
            m.invisibleLocalVariableAnnotations = null;
            // The throw is built on top of whatever the operand stack held at that point.
            m.maxStack += 3;
            // A try range whose code was removed is empty, which the class file format rejects.
            m.tryCatchBlocks.removeIf(t -> {
                for (AbstractInsnNode n = t.start; n != null && n != t.end; n = n.getNext()) {
                    if (n.getOpcode() >= 0) return false;
                }
                return true;
            });
            // Line entries of a removed tail can point at the end of the code, which the class file format rejects.
            for (AbstractInsnNode n = m.instructions.getFirst(); n != null; ) {
                AbstractInsnNode after = n.getNext();
                if (n instanceof org.objectweb.asm.tree.LineNumberNode) m.instructions.remove(n);
                n = after;
            }
        }
        return changed;
    }

    private static boolean guarded(MethodNode m) {
        for (TryCatchBlockNode t : m.tryCatchBlocks) {
            if (t.type == null || t.type.equals("java/lang/Throwable") || t.type.equals("java/lang/Error")
                    || t.type.equals("java/lang/LinkageError") || t.type.equals("java/lang/NoClassDefFoundError")) {
                return true;
            }
        }
        return false;
    }

    private static String missingIn(AbstractInsnNode insn, Hierarchy h) {
        String found = switch (insn) {
            case MethodInsnNode mi -> missing(h, mi.owner, Type.getMethodType(mi.desc));
            case FieldInsnNode fi -> missing(h, fi.owner, Type.getType(fi.desc));
            case TypeInsnNode ti -> missing(h, ti.desc, null);
            case MultiANewArrayInsnNode ma -> missing(h, ma.desc, null);
            default -> null;
        };
        return found == null ? null : found.replace('/', '.');
    }

    private static String missing(Hierarchy h, String owner, Type desc) {
        String c = absent(h, owner);
        if (c != null || desc == null) return c;
        if (desc.getSort() == Type.METHOD) {
            for (Type t : desc.getArgumentTypes()) if ((c = absent(h, t)) != null) return c;
            return absent(h, desc.getReturnType());
        }
        return absent(h, desc);
    }

    private static String absent(Hierarchy h, Type t) {
        if (t.getSort() == Type.ARRAY) t = t.getElementType();
        return t.getSort() == Type.OBJECT ? absent(h, t.getInternalName()) : null;
    }

    private static String absent(Hierarchy h, String name) {
        if (name.startsWith("[")) return absent(h, Type.getType(name));
        return h.isAbsent(name) ? name : null;
    }
}
