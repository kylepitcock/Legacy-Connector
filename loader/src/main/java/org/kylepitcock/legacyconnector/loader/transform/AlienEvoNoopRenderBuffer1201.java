package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Arrays;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adds the 1.21.1 float-position method to AlienEvo's deliberately no-op vertex sink. */
final class AlienEvoNoopRenderBuffer1201 implements Opcodes {
    static final String TARGET_CLASS = "net/habb/alienevo/client/renderer/NoopRenderBuffer$Noop";
    static final String VERTEX = "com/mojang/blaze3d/vertex/VertexConsumer";
    private static final String LEGACY_POSITION_DESC = "(DDD)L" + VERTEX + ";";
    private static final String ADD_VERTEX_DESC = "(FFF)L" + VERTEX + ";";

    private AlienEvoNoopRenderBuffer1201() {}

    static int apply(ClassNode node) {
        if (!TARGET_CLASS.equals(node.name)) return 0;
        if (!"java/lang/Object".equals(node.superName) || !node.interfaces.contains(VERTEX)) {
            throw new IllegalStateException(
                    "AlienEvo NoopRenderBuffer$Noop no longer directly implements VertexConsumer");
        }

        int legacyPositionMethods = 0;
        MethodNode existingAddVertex = null;
        for (MethodNode method : node.methods) {
            if (LEGACY_POSITION_DESC.equals(method.desc)) legacyPositionMethods++;
            if ("addVertex".equals(method.name) && ADD_VERTEX_DESC.equals(method.desc)) {
                if (existingAddVertex != null) {
                    throw new IllegalStateException("AlienEvo NoopRenderBuffer$Noop has duplicate addVertex(FFF) methods");
                }
                existingAddVertex = method;
            }
        }
        if (legacyPositionMethods != 1) {
            throw new IllegalStateException(
                    "AlienEvo NoopRenderBuffer$Noop expected one legacy double-position method; found "
                            + legacyPositionMethods);
        }
        if (existingAddVertex != null) {
            if (!isThisReturningNoop(existingAddVertex)) {
                throw new IllegalStateException(
                        "AlienEvo NoopRenderBuffer$Noop already has a non-no-op addVertex(FFF) implementation");
            }
            return 0;
        }

        MethodNode addVertex = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC,
                "addVertex", ADD_VERTEX_DESC, null, null);
        addVertex.instructions.add(new VarInsnNode(ALOAD, 0));
        addVertex.instructions.add(new InsnNode(ARETURN));
        addVertex.maxStack = 1;
        addVertex.maxLocals = 4;
        node.methods.add(addVertex);
        return 1;
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
