package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Regression tests for PalladiumCore's Forge-to-NeoForge HUD layer migration. */
class PalladiumCoreOverlay1201Test implements Opcodes {
    private static final String EVENT_DESC =
            "(L" + PalladiumCoreOverlay1201.REGISTER_EVENT + ";)V";

    private static ClassNode fixture() {
        ClassNode node = new ClassNode();
        node.name = PalladiumCoreOverlay1201.OVERLAY_REGISTRY;
        node.superName = "java/lang/Object";

        MethodNode register = new MethodNode(ACC_PUBLIC | ACC_STATIC,
                "registerOverlay",
                "(Ljava/lang/String;L" + PalladiumCoreOverlay1201.INGAME_OVERLAY + ";)V",
                null, null);
        node.methods.add(register);

        MethodNode handler = new MethodNode(ACC_PUBLIC | ACC_STATIC,
                "onRegisterGuiOverlays", EVENT_DESC, null, null);
        handler.visibleAnnotations = new java.util.ArrayList<>();
        org.objectweb.asm.tree.AnnotationNode subscribe =
                new org.objectweb.asm.tree.AnnotationNode("Lnet/neoforged/bus/api/SubscribeEvent;");
        handler.visibleAnnotations.add(subscribe);
        handler.instructions.add(new FieldInsnNode(
                GETSTATIC, PalladiumCoreOverlay1201.OVERLAY_REGISTRY,
                "OVERLAYS", "Ljava/util/Map;"));
        handler.instructions.add(new InsnNode(RETURN));
        node.methods.add(handler);

        MethodNode oldRegisterLambda = new MethodNode(ACC_PRIVATE | ACC_STATIC,
                "lambda$onRegisterGuiOverlays$1",
                "(L" + PalladiumCoreOverlay1201.REGISTER_EVENT
                        + ";Ljava/lang/String;L" + PalladiumCoreOverlay1201.INGAME_OVERLAY + ";)V",
                null, null);
        oldRegisterLambda.instructions.add(new InsnNode(RETURN));
        node.methods.add(oldRegisterLambda);

        MethodNode oldRenderLambda = new MethodNode(ACC_PRIVATE | ACC_STATIC,
                "lambda$onRegisterGuiOverlays$0",
                "(L" + PalladiumCoreOverlay1201.INGAME_OVERLAY + ";Lnet/minecraft/client/gui/GuiGraphics;FII)V",
                null, null);
        oldRenderLambda.instructions.add(new InsnNode(RETURN));
        node.methods.add(oldRenderLambda);
        return node;
    }

    @Test
    void migratesLegacyOverlayRegistryIntoNeoForgeGuiLayers() {
        ClassNode node = fixture();
        assertEquals(1, PalladiumCoreOverlay1201.apply(node));

        MethodNode handler = node.methods.stream()
                .filter(method -> "onRegisterGuiOverlays".equals(method.name))
                .findFirst().orElse(null);
        assertNotNull(handler);
        assertEquals(EVENT_DESC, handler.desc);
        assertTrue(Arrays.stream(handler.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof InvokeDynamicInsnNode dynamic
                        && dynamic.desc.equals("(L" + PalladiumCoreOverlay1201.REGISTER_EVENT
                                + ";)Ljava/util/function/BiConsumer;")));
        assertTrue(Arrays.stream(handler.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals("java/util/Map") && call.name.equals("forEach")));
        assertTrue(handler.visibleAnnotations.stream()
                .anyMatch(annotation -> "Lnet/neoforged/bus/api/SubscribeEvent;".equals(annotation.desc)));

        MethodNode registration = node.methods.stream()
                .filter(method -> "lambda$onRegisterGuiOverlays$1".equals(method.name))
                .findFirst().orElse(null);
        assertNotNull(registration);
        assertTrue(Arrays.stream(registration.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(PalladiumCoreOverlay1201.RESOURCE_LOCATION)
                        && call.name.equals("fromNamespaceAndPath")));
        assertTrue(Arrays.stream(registration.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof InvokeDynamicInsnNode dynamic
                        && dynamic.desc.equals(PalladiumCoreOverlay1201.LAYER_FACTORY_DESC)));
        assertTrue(Arrays.stream(registration.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(PalladiumCoreOverlay1201.REGISTER_EVENT)
                        && call.name.equals("registerAboveAll")
                        && call.desc.equals("(L" + PalladiumCoreOverlay1201.RESOURCE_LOCATION
                                + ";L" + PalladiumCoreOverlay1201.LAYER + ";)V")));

        MethodNode render = node.methods.stream()
                .filter(method -> "lambda$onRegisterGuiOverlays$0".equals(method.name))
                .findFirst().orElse(null);
        assertNotNull(render);
        assertTrue(Arrays.stream(render.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof FieldInsnNode field
                        && field.owner.equals(PalladiumCoreOverlay1201.MINECRAFT)
                        && field.name.equals("gui")));
        assertTrue(Arrays.stream(render.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(PalladiumCoreOverlay1201.INGAME_OVERLAY)
                        && call.name.equals("render")));
        assertFalse(node.methods.stream()
                .anyMatch(method -> method.desc.contains("ForgeGui")
                        || method.desc.contains("IGuiOverlay")));
        assertEquals(0, PalladiumCoreOverlay1201.apply(node));
    }

    @Test
    void doesNotTouchUnrelatedClasses() {
        ClassNode node = fixture();
        node.name = "net/threetag/palladiumcore/registry/client/OtherRegistry";
        assertEquals(0, PalladiumCoreOverlay1201.apply(node));
        assertFalse(node.methods.stream()
                .anyMatch(method -> method.desc.equals(EVENT_DESC)
                        && method.name.equals("lambda$onRegisterGuiOverlays$0")));
    }
}
