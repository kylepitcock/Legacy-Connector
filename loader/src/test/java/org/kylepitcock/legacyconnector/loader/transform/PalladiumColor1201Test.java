package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class PalladiumColor1201Test implements Opcodes {
    private static final String DYE_COLOR = "net/minecraft/world/item/DyeColor";
    private static final String DESC = "(L" + DYE_COLOR + ";)[F";
    private static final String RENDER_DESC =
            "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "ILnet/threetag/palladium/entity/SuitStand;FFFFFF)V";

    @Test
    void redirectsAllThreeSheepColorLookups() {
        ClassNode node = legacyLayer();
        assertEquals(3, PalladiumColor1201.apply(node));
        MethodNode render = findRender(node);
        assertEquals(0, count(render, "net/minecraft/world/entity/animal/Sheep", "getColorArray"));
        assertEquals(3, count(render, "legacyconnector/v1_20_1/PalladiumColorCompat", "sheepColorArray"));
        assertEquals(0, PalladiumColor1201.apply(node));
        assertSerializes(node);
    }

    @Test
    void unrelatedClassesAreNotChanged() {
        ClassNode node = legacyLayer();
        node.name = "example/OtherLayer";
        assertEquals(0, PalladiumColor1201.apply(node));
        assertEquals(3, count(findRender(node), "net/minecraft/world/entity/animal/Sheep", "getColorArray"));
    }

    @Test
    void partialAdaptationIsRejected() {
        ClassNode node = legacyLayer();
        MethodNode render = findRender(node);
        render.instructions.insertBefore(render.instructions.getLast(), new MethodInsnNode(
                INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumColorCompat", "sheepColorArray", DESC, false));
        assertThrows(IllegalStateException.class, () -> PalladiumColor1201.apply(node));
    }

    @Test
    void unexpectedCallCountIsRejectedWithoutMutation() {
        ClassNode node = legacyLayer();
        MethodNode render = findRender(node);
        render.instructions.remove(findLegacy(render));
        assertThrows(IllegalStateException.class, () -> PalladiumColor1201.apply(node));
        assertEquals(2, count(render, "net/minecraft/world/entity/animal/Sheep", "getColorArray"));
    }

    private static ClassNode legacyLayer() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumColor1201.COLORABLE_SUIT_STAND_LAYER;
        node.superName = "net/minecraft/client/renderer/entity/layers/RenderLayer";

        MethodNode render = new MethodNode(ACC_PUBLIC, "render", RENDER_DESC, null, null);
        for (int i = 0; i < 3; i++) {
            render.instructions.add(new InsnNode(ACONST_NULL));
            render.instructions.add(new MethodInsnNode(INVOKESTATIC,
                    "net/minecraft/world/entity/animal/Sheep", "getColorArray", DESC, false));
            render.instructions.add(new InsnNode(POP));
        }
        render.instructions.add(new InsnNode(RETURN));
        render.maxStack = 1;
        render.maxLocals = 11;
        node.methods.add(render);
        return node;
    }

    private static MethodNode findRender(ClassNode node) {
        for (MethodNode method : node.methods) {
            if ("render".equals(method.name) && RENDER_DESC.equals(method.desc)) return method;
        }
        return null;
    }

    private static MethodInsnNode findLegacy(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && "net/minecraft/world/entity/animal/Sheep".equals(call.owner)
                    && "getColorArray".equals(call.name)) return call;
        }
        return null;
    }

    private static int count(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && owner.equals(call.owner) && name.equals(call.name)) count++;
        }
        return count;
    }

    private static void assertSerializes(ClassNode node) {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            writer.toByteArray();
        });
    }
}
