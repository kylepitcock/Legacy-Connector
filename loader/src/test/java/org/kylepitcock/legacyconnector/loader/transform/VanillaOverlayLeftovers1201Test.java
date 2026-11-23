package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Enum-shaped uses of Forge's VanillaGuiOverlay that survive the class alias go back to the shim enum; ResourceLocation layer ids stay native. */
class VanillaOverlayLeftovers1201Test implements Opcodes {
    private static final String NATIVE = "net/neoforged/neoforge/client/gui/VanillaGuiLayers";
    private static final String SHIM = "legacyconnector/v1_20_1/net/minecraftforge/client/gui/overlay/VanillaGuiOverlay";

    @Test
    void enumUsesAreRepointedAndLayerIdsAreLeftAlone() {
        ClassNode node = new ClassNode();
        node.name = "example/Hud";
        node.superName = "java/lang/Object";
        MethodNode m = new MethodNode(ACC_PUBLIC, "check", "()V", null, null);
        m.instructions.add(new FieldInsnNode(GETSTATIC, NATIVE, "HOTBAR", "L" + NATIVE + ";"));
        m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, NATIVE, "id", "()Lnet/minecraft/resources/ResourceLocation;", false));
        m.instructions.add(new InsnNode(POP));
        m.instructions.add(new FieldInsnNode(GETSTATIC, NATIVE, "CROSSHAIR", "Lnet/minecraft/resources/ResourceLocation;"));
        m.instructions.add(new InsnNode(POP));
        m.instructions.add(new InsnNode(RETURN));
        node.methods.add(m);

        int changed = VanillaOverlayLeftovers1201.apply(node);

        FieldInsnNode constant = (FieldInsnNode) m.instructions.getFirst();
        MethodInsnNode id = (MethodInsnNode) constant.getNext();
        FieldInsnNode layerId = (FieldInsnNode) id.getNext().getNext();
        assertEquals(2, changed);
        assertEquals(SHIM, constant.owner);
        assertEquals("L" + SHIM + ";", constant.desc);
        assertEquals(SHIM, id.owner);
        assertEquals(NATIVE, layerId.owner);
        assertEquals("Lnet/minecraft/resources/ResourceLocation;", layerId.desc);
    }
}
