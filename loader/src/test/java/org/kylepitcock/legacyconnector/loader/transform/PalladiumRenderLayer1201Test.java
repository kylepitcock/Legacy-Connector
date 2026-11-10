package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class PalladiumRenderLayer1201Test implements Opcodes {
    private static final String OLD_DESC =
            "(Lnet/minecraft/client/model/EntityModel;Lnet/minecraft/client/model/EntityModel;"
                    + "Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
                    + "Lnet/minecraft/world/entity/LivingEntity;FFFFFFFFF)V";
    private static final String NEW_DESC =
            "(Lnet/minecraft/client/model/EntityModel;Lnet/minecraft/client/model/EntityModel;"
                    + "Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
                    + "Lnet/minecraft/world/entity/LivingEntity;FFFFFFI)V";
    private static final String OWNER = PalladiumRenderLayer1201.COLORABLE_SUIT_STAND_LAYER;
    private static final String HELPER = "legacyconnector/v1_20_1/PalladiumColorCompat";

    @Test
    void replacesTheNineFloatCallWithSixFloatsAndPackedTint() {
        ClassNode node = legacyLayer();

        assertEquals(1, PalladiumRenderLayer1201.apply(node));

        MethodInsnNode replacement = findCall(node, NEW_DESC);
        assertNotNull(replacement);
        assertEquals(0, countCalls(node, OLD_DESC));
        assertEquals(1, countCalls(node, NEW_DESC));

        AbstractInsnNode previous = previousReal(replacement);
        org.junit.jupiter.api.Assertions.assertTrue(previous instanceof MethodInsnNode call
                && call.getOpcode() == INVOKESTATIC
                && HELPER.equals(call.owner)
                && "packRgb".equals(call.name)
                && "(FFF)I".equals(call.desc));

        assertEquals(0, PalladiumRenderLayer1201.apply(node),
                "the adapter must be idempotent");
        assertSerializes(node);
    }

    @Test
    void unrelatedClassesAreNotChanged() {
        ClassNode node = legacyLayer();
        node.name = "example/OtherLayer";

        assertEquals(0, PalladiumRenderLayer1201.apply(node));
        assertEquals(1, countCalls(node, OLD_DESC));
    }

    @Test
    void unexpectedCallCountIsRejectedWithoutMutation() {
        ClassNode node = legacyLayer();
        MethodNode render = findRender(node);
        render.instructions.remove(findCall(node, OLD_DESC));

        assertThrows(IllegalStateException.class, () -> PalladiumRenderLayer1201.apply(node));
        assertEquals(0, countCalls(node, OLD_DESC));
    }

    @Test
    void unexpectedSuperclassIsRejected() {
        ClassNode node = legacyLayer();
        node.superName = "net/minecraft/client/renderer/entity/layers/SomeOtherLayer";

        assertThrows(IllegalStateException.class, () -> PalladiumRenderLayer1201.apply(node));
    }

    private static ClassNode legacyLayer() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = OWNER;
        node.superName = "net/minecraft/client/renderer/entity/layers/RenderLayer";

        String renderDesc =
                "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                        + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                        + "ILnet/threetag/palladium/entity/SuitStand;FFFFFF)V";
        MethodNode render = new MethodNode(ACC_PUBLIC, "render", renderDesc, null, null);

        // Model, model, texture, pose stack, buffer source, packed light, entity.
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new InsnNode(ACONST_NULL));
        render.instructions.add(new InsnNode(ICONST_0));
        render.instructions.add(new InsnNode(ACONST_NULL));

        // Six legacy animation floats, then the RGB floats to be packed.
        for (int i = 0; i < 9; i++) {
            render.instructions.add(new InsnNode(FCONST_0));
        }
        render.instructions.add(new MethodInsnNode(INVOKESTATIC, OWNER,
                "coloredCutoutModelCopyLayerRender", OLD_DESC, false));
        render.instructions.add(new InsnNode(RETURN));
        render.maxStack = 16;
        render.maxLocals = 11;
        node.methods.add(render);
        return node;
    }

    private static MethodNode findRender(ClassNode node) {
        for (MethodNode method : node.methods) {
            if ("render".equals(method.name)
                    && method.desc.endsWith("SuitStand;FFFFFF)V")) {
                return method;
            }
        }
        return null;
    }

    private static MethodInsnNode findCall(ClassNode node, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && "coloredCutoutModelCopyLayerRender".equals(call.name)
                        && descriptor.equals(call.desc)) {
                    return call;
                }
            }
        }
        return null;
    }

    private static int countCalls(ClassNode node, String descriptor) {
        int count = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && "coloredCutoutModelCopyLayerRender".equals(call.name)
                        && descriptor.equals(call.desc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static void assertSerializes(ClassNode node) {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
        });
    }
}
