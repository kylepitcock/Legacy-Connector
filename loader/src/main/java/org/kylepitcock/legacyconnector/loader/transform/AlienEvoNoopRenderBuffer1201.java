package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Arrays;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adds the missing 1.21.1 VertexConsumer methods to AlienEvo's deliberately no-op vertex sink. */
final class AlienEvoNoopRenderBuffer1201 implements Opcodes {
    static final String TARGET_CLASS = "net/habb/alienevo/client/renderer/NoopRenderBuffer$Noop";
    static final String VERTEX = "com/mojang/blaze3d/vertex/VertexConsumer";
    private static final String LEGACY_POSITION_DESC = "(DDD)L" + VERTEX + ";";
    private static final String ADD_VERTEX_DESC = "(FFF)L" + VERTEX + ";";
    private static final String SET_COLOR_DESC = "(IIII)L" + VERTEX + ";";
    private static final String SET_UV_DESC = "(FF)L" + VERTEX + ";";

    private AlienEvoNoopRenderBuffer1201() {}

    static int apply(ClassNode node) {
        if (!TARGET_CLASS.equals(node.name)) return 0;
        if (!"java/lang/Object".equals(node.superName) || !node.interfaces.contains(VERTEX)) {
            throw new IllegalStateException(
                    "AlienEvo NoopRenderBuffer$Noop no longer directly implements VertexConsumer");
        }

        int legacyPositionMethods = 0;
        MethodNode existingAddVertex = null;
        MethodNode existingSetColor = null;
        MethodNode existingSetUv = null;
        for (MethodNode method : node.methods) {
            if (LEGACY_POSITION_DESC.equals(method.desc)) legacyPositionMethods++;
            if ("addVertex".equals(method.name) && ADD_VERTEX_DESC.equals(method.desc)) {
                if (existingAddVertex != null) {
                    throw new IllegalStateException("AlienEvo NoopRenderBuffer$Noop has duplicate addVertex(FFF) methods");
                }
                existingAddVertex = method;
            }
            if ("setColor".equals(method.name) && SET_COLOR_DESC.equals(method.desc)) {
                if (existingSetColor != null) {
                    throw new IllegalStateException("AlienEvo NoopRenderBuffer$Noop has duplicate setColor(IIII) methods");
                }
                existingSetColor = method;
            }
            if ("setUv".equals(method.name) && SET_UV_DESC.equals(method.desc)) {
                if (existingSetUv != null) {
                    throw new IllegalStateException("AlienEvo NoopRenderBuffer$Noop has duplicate setUv(FF) methods");
                }
                existingSetUv = method;
            }
        }
        if (legacyPositionMethods != 1) {
            throw new IllegalStateException(
                    "AlienEvo NoopRenderBuffer$Noop expected one legacy double-position method; found "
                            + legacyPositionMethods);
        }

        int changes = 0;
        if (existingAddVertex != null) {
            if (!isThisReturningNoop(existingAddVertex)) {
                throw new IllegalStateException(
                        "AlienEvo NoopRenderBuffer$Noop already has a non-no-op addVertex(FFF) implementation");
            }
        } else {
            node.methods.add(noopFluentMethod("addVertex", ADD_VERTEX_DESC, 4));
            changes++;
        }

        // VertexConsumer.addVertex(float,float,float) chains through setColor
        // when bulk-rendering an item. The 1.20.1 Noop sink has no matching
        // 1.21.1 implementation, so keep the sink genuinely side-effect-free.
        if (existingSetColor != null) {
            if ((existingSetColor.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
                replaceWithNoop(existingSetColor, 5);
                changes++;
            } else if (!isThisReturningNoop(existingSetColor)) {
                throw new IllegalStateException(
                        "AlienEvo NoopRenderBuffer$Noop already has a non-no-op setColor(IIII) implementation");
            }
        } else {
            node.methods.add(noopFluentMethod("setColor", SET_COLOR_DESC, 5));
            changes++;
        }

        // setUv(float,float) is another abstract VertexConsumer method in 1.21.1.
        // AlienEvo uses this sink during its pose pre-pass; it must remain a true no-op.
        if (existingSetUv != null) {
            if ((existingSetUv.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
                replaceWithNoop(existingSetUv, 3);
                changes++;
            } else if (!isThisReturningNoop(existingSetUv)) {
                throw new IllegalStateException(
                        "AlienEvo NoopRenderBuffer$Noop already has a non-no-op setUv(FF) implementation");
            }
        } else {
            node.methods.add(noopFluentMethod("setUv", SET_UV_DESC, 3));
            changes++;
        }
        return changes;
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
