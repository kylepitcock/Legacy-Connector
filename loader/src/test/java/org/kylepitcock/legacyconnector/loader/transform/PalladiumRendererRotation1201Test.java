package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

class PalladiumRendererRotation1201Test implements Opcodes {
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String SUIT_STAND = "net/threetag/palladium/entity/SuitStand";
    private static final String POSE_STACK = "com/mojang/blaze3d/vertex/PoseStack";
    private static final String OLD_BASE_DESC = "(L" + LIVING_ENTITY + ";L" + POSE_STACK + ";FFF)V";
    private static final String NEW_BASE_DESC = "(L" + LIVING_ENTITY + ";L" + POSE_STACK + ";FFFF)V";
    private static final String OLD_TYPED_DESC = "(L" + SUIT_STAND + ";L" + POSE_STACK + ";FFF)V";
    private static final String NEW_TYPED_DESC = "(L" + SUIT_STAND + ";L" + POSE_STACK + ";FFFF)V";

    @Test
    void migratesSuitStandTypedOverrideAndItsErasedBridge() {
        ClassNode node = suitStandRenderer();
        MethodNode bridge = find(node, "setupRotations", OLD_BASE_DESC);

        assertEquals(3, PalladiumRendererRotation1201.apply(node));
        assertNotNull(find(node, "setupRotations", NEW_TYPED_DESC));
        assertNotNull(find(node, "setupRotations", NEW_BASE_DESC));
        assertEquals(null, find(node, "setupRotations", OLD_TYPED_DESC));
        assertEquals(null, find(node, "setupRotations", OLD_BASE_DESC));

        MethodInsnNode forwarded = findCall(bridge, node.name, "setupRotations", NEW_TYPED_DESC);
        assertNotNull(forwarded);
        assertTrue(previousReal(forwarded) instanceof VarInsnNode scale
                        && scale.getOpcode() == FLOAD && scale.var == 6,
                "the modern bridge forwards the scale supplied by 1.21.1");
        assertTrue(bridge.maxLocals >= 7);
        assertEquals(0, PalladiumRendererRotation1201.apply(node), "second application is idempotent");
        assertEmittable(node);
    }

    @Test
    void migratesTrailSegmentInheritedRotationCallWithDefaultScale() {
        ClassNode node = trailSegmentRenderer();
        MethodNode renderModel = find(node, "renderModel",
                "(Lnet/threetag/palladium/entity/TrailSegmentEntity;FFL" + POSE_STACK
                        + ";Lnet/minecraft/client/renderer/MultiBufferSource;I)V");

        assertEquals(1, PalladiumRendererRotation1201.apply(node));
        MethodInsnNode migrated = findCall(renderModel, node.name, "setupRotations", NEW_BASE_DESC);
        assertNotNull(migrated);
        assertTrue(previousReal(migrated).getOpcode() == FCONST_1);
        assertFalse(hasCall(renderModel, node.name, "setupRotations", OLD_BASE_DESC));
        assertTrue(renderModel.maxStack >= 7);
        assertEquals(0, PalladiumRendererRotation1201.apply(node), "second application is idempotent");
        assertEmittable(node);
    }

    @Test
    void unrelatedClassesAreNotChanged() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.name = "example/UnrelatedRenderer";
        node.superName = "java/lang/Object";
        assertEquals(0, PalladiumRendererRotation1201.apply(node));
    }

    private static ClassNode suitStandRenderer() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumRendererRotation1201.SUIT_STAND_RENDERER;
        node.superName = "net/minecraft/client/renderer/entity/LivingEntityRenderer";

        MethodNode typed = new MethodNode(ACC_PROTECTED, "setupRotations", OLD_TYPED_DESC, null, null);
        typed.instructions.add(new InsnNode(RETURN));
        typed.maxStack = 0;
        typed.maxLocals = 6;
        node.methods.add(typed);

        MethodNode bridge = new MethodNode(
                ACC_PROTECTED | ACC_BRIDGE | ACC_SYNTHETIC, "setupRotations", OLD_BASE_DESC, null, null);
        bridge.instructions.add(new VarInsnNode(ALOAD, 0));
        bridge.instructions.add(new VarInsnNode(ALOAD, 1));
        bridge.instructions.add(new TypeInsnNode(CHECKCAST, SUIT_STAND));
        bridge.instructions.add(new VarInsnNode(ALOAD, 2));
        bridge.instructions.add(new VarInsnNode(FLOAD, 3));
        bridge.instructions.add(new VarInsnNode(FLOAD, 4));
        bridge.instructions.add(new VarInsnNode(FLOAD, 5));
        bridge.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, node.name, "setupRotations", OLD_TYPED_DESC, false));
        bridge.instructions.add(new InsnNode(RETURN));
        bridge.maxStack = 6;
        bridge.maxLocals = 6;
        node.methods.add(bridge);
        return node;
    }

    private static ClassNode trailSegmentRenderer() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumRendererRotation1201.TRAIL_SEGMENT_RENDERER;
        node.superName = "net/minecraft/client/renderer/entity/LivingEntityRenderer";

        String desc = "(Lnet/threetag/palladium/entity/TrailSegmentEntity;FFL" + POSE_STACK
                + ";Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
        MethodNode renderModel = new MethodNode(ACC_PUBLIC, "renderModel", desc, null, null);
        renderModel.instructions.add(new VarInsnNode(ALOAD, 0));
        renderModel.instructions.add(new VarInsnNode(ALOAD, 1));
        renderModel.instructions.add(new VarInsnNode(ALOAD, 4));
        renderModel.instructions.add(new VarInsnNode(FLOAD, 3));
        renderModel.instructions.add(new VarInsnNode(FLOAD, 2));
        renderModel.instructions.add(new VarInsnNode(FLOAD, 3));
        renderModel.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, node.name,
                "setupRotations", OLD_BASE_DESC, false));
        renderModel.instructions.add(new InsnNode(RETURN));
        renderModel.maxStack = 6;
        renderModel.maxLocals = 7;
        node.methods.add(renderModel);
        return node;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
    }

    private static MethodInsnNode findCall(MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && owner.equals(call.owner) && name.equals(call.name) && descriptor.equals(call.desc)) return call;
        }
        return null;
    }

    private static boolean hasCall(MethodNode method, String owner, String name, String descriptor) {
        return findCall(method, owner, name, descriptor) != null;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static void assertEmittable(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }
}
