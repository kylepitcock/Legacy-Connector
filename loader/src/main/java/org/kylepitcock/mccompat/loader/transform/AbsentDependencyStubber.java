package org.kylepitcock.mccompat.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
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
 * class fails to load. Methods whose code references classes that exist nowhere get their body replaced by
 * {@code throw new NoClassDefFoundError(<class>)}: the class verifies, and only that path fails - lazily, as the
 * mod expected. Methods that catch Throwable/Error/LinkageError themselves are left alone.
 */
public final class AbsentDependencyStubber {
    private AbsentDependencyStubber() {}

    public static int apply(ClassNode node, Hierarchy hierarchy) {
        int stubbed = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions.size() == 0 || guarded(m)) continue;
            String missing = firstMissing(m, hierarchy);
            if (missing == null) continue;
            InsnList body = new InsnList();
            body.add(new TypeInsnNode(Opcodes.NEW, "java/lang/NoClassDefFoundError"));
            body.add(new InsnNode(Opcodes.DUP));
            body.add(new LdcInsnNode(missing));
            body.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/NoClassDefFoundError", "<init>", "(Ljava/lang/String;)V", false));
            body.add(new InsnNode(Opcodes.ATHROW));
            m.instructions = body;
            m.tryCatchBlocks.clear();
            m.localVariables = null;
            m.visibleLocalVariableAnnotations = null;
            m.invisibleLocalVariableAnnotations = null;
            m.maxStack = Math.max(m.maxStack, 3);
            stubbed++;
        }
        return stubbed;
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

    private static String firstMissing(MethodNode m, Hierarchy h) {
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            String found = switch (insn) {
                case MethodInsnNode mi -> missing(h, mi.owner, Type.getMethodType(mi.desc));
                case FieldInsnNode fi -> missing(h, fi.owner, Type.getType(fi.desc));
                case TypeInsnNode ti -> missing(h, ti.desc, null);
                case MultiANewArrayInsnNode ma -> missing(h, ma.desc, null);
                default -> null;
            };
            if (found != null) return found.replace('/', '.');
        }
        return null;
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
