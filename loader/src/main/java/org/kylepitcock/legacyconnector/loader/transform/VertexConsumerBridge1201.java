package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * A mod class that implements {@code VertexConsumer} itself (Mekanism's quad unpacker, mods that capture vertices) has the 1.20.1 methods
 * ({@code vertex(DDD)}, {@code color(IIII)}, {@code uv}, {@code overlayCoords}, {@code uv2}, {@code normal}, {@code endVertex}); 1.21.1's interface has other abstract
 * ones ({@code addVertex(FFF)}, {@code setColor(IIII)}, {@code setUv}, {@code setUv1}, {@code setUv2}, {@code setNormal}) that vanilla calls, one vertex at a
 * time with the normal last. Bridges calling the legacy methods are added; the normal bridge ends the vertex like {@code endVertex()} did.
 */
final class VertexConsumerBridge1201 {
    private VertexConsumerBridge1201() {}

    private static final String VC = "com/mojang/blaze3d/vertex/VertexConsumer";
    private static final String VC_DESC = "L" + VC + ";";

    /** @return the number of bridge methods added */
    static int apply(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) != 0 || node.interfaces == null || !node.interfaces.contains(VC)) return 0;
        if (!has(node, "vertex", "(DDD)" + VC_DESC)) return 0;
        int n = 0;
        n += bridge(node, "addVertex", "(FFF)" + VC_DESC, "vertex", "(DDD)" + VC_DESC, "FFF", true, false);
        n += bridge(node, "setColor", "(IIII)" + VC_DESC, "color", "(IIII)" + VC_DESC, "IIII", false, false);
        n += bridge(node, "setUv", "(FF)" + VC_DESC, "uv", "(FF)" + VC_DESC, "FF", false, false);
        n += bridge(node, "setUv1", "(II)" + VC_DESC, "overlayCoords", "(II)" + VC_DESC, "II", false, false);
        n += bridge(node, "setUv2", "(II)" + VC_DESC, "uv2", "(II)" + VC_DESC, "II", false, false);
        n += bridge(node, "setNormal", "(FFF)" + VC_DESC, "normal", "(FFF)" + VC_DESC, "FFF", false, has(node, "endVertex", "()V"));
        return n;
    }

    private static boolean has(ClassNode node, String name, String desc) {
        for (MethodNode m : node.methods) if (m.name.equals(name) && m.desc.equals(desc)) return true;
        return false;
    }

    private static int bridge(ClassNode node, String name, String desc, String target, String targetDesc, String args, boolean widen, boolean endVertex) {
        if (has(node, name, desc)) return 0;
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, name, desc, null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (has(node, target, targetDesc)) {
            int slot = 1;
            for (char c : args.toCharArray()) {
                m.instructions.add(new VarInsnNode(c == 'F' ? Opcodes.FLOAD : Opcodes.ILOAD, slot++));
                if (widen && c == 'F') m.instructions.add(new InsnNode(Opcodes.F2D));
            }
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, target, targetDesc, false));
            if (endVertex) {
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, "endVertex", "()V", false));
            }
        }
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        m.maxStack = 8;
        m.maxLocals = 1 + Type.getArgumentsAndReturnSizes(desc) / 4 + 4;
        node.methods.add(m);
        return 1;
    }
}
