package org.kylepitcock.legacyconnector.loader.transform.compat;

import java.util.Arrays;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Supplies all abstract 1.21.1 VertexConsumer mutators on AlienEvo's deliberately no-op vertex sink. */
public final class AlienEvoNoopRenderBuffer1201 implements Opcodes {
    public static final String TARGET_CLASS = "net/habb/alienevo/client/renderer/NoopRenderBuffer$Noop";
    public static final String VERTEX = "com/mojang/blaze3d/vertex/VertexConsumer";
    private static final String LEGACY_POSITION_DESC = "(DDD)L" + VERTEX + ";";
    private static final String ADD_VERTEX_DESC = "(FFF)L" + VERTEX + ";";
    private static final String SET_COLOR_DESC = "(IIII)L" + VERTEX + ";";
    private static final String SET_UV_DESC = "(FF)L" + VERTEX + ";";
    private static final String SET_UV1_DESC = "(II)L" + VERTEX + ";";
    private static final String SET_UV2_DESC = "(II)L" + VERTEX + ";";
    private static final String SET_NORMAL_DESC = "(FFF)L" + VERTEX + ";";

    private AlienEvoNoopRenderBuffer1201() {}

    public static int apply(ClassNode node) {
        if (!TARGET_CLASS.equals(node.name)) return 0;
        if (!"java/lang/Object".equals(node.superName) || !node.interfaces.contains(VERTEX)) {
            throw new IllegalStateException(
                    "AlienEvo NoopRenderBuffer$Noop no longer directly implements VertexConsumer");
        }

        long legacyPositionMethods = node.methods.stream()
                .filter(method -> LEGACY_POSITION_DESC.equals(method.desc))
                .count();
        if (legacyPositionMethods != 1) {
            throw new IllegalStateException(
                    "AlienEvo NoopRenderBuffer$Noop expected one legacy double-position method; found "
                            + legacyPositionMethods);
        }

        int changes = 0;
        changes += ensureNoopMethod(node, "addVertex", ADD_VERTEX_DESC, 4);
        changes += ensureNoopMethod(node, "setColor", SET_COLOR_DESC, 5);
        changes += ensureNoopMethod(node, "setUv", SET_UV_DESC, 3);
        changes += ensureNoopMethod(node, "setUv1", SET_UV1_DESC, 3);
        changes += ensureNoopMethod(node, "setUv2", SET_UV2_DESC, 3);
        changes += ensureNoopMethod(node, "setNormal", SET_NORMAL_DESC, 4);
        return changes;
    }

    private static int ensureNoopMethod(ClassNode node, String name, String descriptor, int locals) {
        MethodNode existing = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name) || !descriptor.equals(method.desc)) continue;
            if (existing != null) {
                throw new IllegalStateException(
                        "AlienEvo NoopRenderBuffer$Noop has duplicate " + name + descriptor + " methods");
            }
            existing = method;
        }

        if (existing == null) {
            node.methods.add(noopFluentMethod(name, descriptor, locals));
            return 1;
        }
        if ((existing.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
            replaceWithNoop(existing, locals);
            return 1;
        }
        if (!isThisReturningNoop(existing)) {
            throw new IllegalStateException(
                    "AlienEvo NoopRenderBuffer$Noop already has a non-no-op " + name + descriptor + " implementation");
        }
        return 0;
    }

    private static MethodNode noopFluentMethod(String name, String descriptor, int locals) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, name, descriptor, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new InsnNode(ARETURN));
        method.maxStack = 1;
        method.maxLocals = locals;
        return method;
    }

    private static void replaceWithNoop(MethodNode method, int locals) {
        method.access = (method.access & ~(ACC_ABSTRACT | ACC_NATIVE | ACC_PRIVATE | ACC_PROTECTED))
                | ACC_PUBLIC | ACC_SYNTHETIC;
        method.instructions.clear();
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new InsnNode(ARETURN));
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.maxStack = 1;
        method.maxLocals = locals;
    }

    private static boolean isThisReturningNoop(MethodNode method) {
        AbstractInsnNode[] real = Arrays.stream(method.instructions.toArray())
                .filter(instruction -> instruction.getOpcode() >= 0)
                .toArray(AbstractInsnNode[]::new);
        return real.length == 2
                && real[0] instanceof VarInsnNode variable
                && variable.getOpcode() == ALOAD && variable.var == 0
                && real[1].getOpcode() == ARETURN;
    }
}
