package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for PalladiumCore's use of the existing Forge overlay event shim. */
class PalladiumCoreOverlay1201Test implements Opcodes {
    private static final String OBJECTS_BI_CONSUMER = "(Ljava/lang/Object;Ljava/lang/Object;)V";
    private static final Handle LAMBDA_METAFACTORY = new Handle(
            H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                    + "Ljava/lang/invoke/CallSite;",
            false);

    /** Models OverlayRegistryImpl after Forge1201Transformer's normal class remapping. */
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
                "onRegisterGuiOverlays", PalladiumCoreOverlay1201.EVENT_DESC, null, null);
        handler.visibleAnnotations = new ArrayList<>();
        handler.visibleAnnotations.add(new AnnotationNode("Lnet/neoforged/bus/api/SubscribeEvent;"));
        handler.instructions.add(new FieldInsnNode(
                GETSTATIC, PalladiumCoreOverlay1201.OVERLAY_REGISTRY,
                "OVERLAYS", "Ljava/util/Map;"));
        handler.instructions.add(new VarInsnNode(ALOAD, 0));
        handler.instructions.add(new InvokeDynamicInsnNode(
                "accept",
                PalladiumCoreOverlay1201.ACCEPT_FACTORY_DESC,
                LAMBDA_METAFACTORY,
                Type.getMethodType(OBJECTS_BI_CONSUMER),
                new Handle(H_INVOKESTATIC, PalladiumCoreOverlay1201.OVERLAY_REGISTRY,
                        "lambda$onRegisterGuiOverlays$1",
                        PalladiumCoreOverlay1201.REGISTER_LAMBDA_DESC, false),
                Type.getMethodType(OBJECTS_BI_CONSUMER)));
        handler.instructions.add(new MethodInsnNode(
                INVOKEINTERFACE, "java/util/Map", "forEach",
                "(Ljava/util/function/BiConsumer;)V", true));
        handler.instructions.add(new InsnNode(RETURN));
        handler.maxStack = 2;
        handler.maxLocals = 1;
        node.methods.add(handler);

        MethodNode registration = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$onRegisterGuiOverlays$1",
                PalladiumCoreOverlay1201.REGISTER_LAMBDA_DESC, null, null);
        registration.instructions.add(new VarInsnNode(ALOAD, 0));
        registration.instructions.add(new VarInsnNode(ALOAD, 1));
        registration.instructions.add(new VarInsnNode(ALOAD, 2));
        registration.instructions.add(new InvokeDynamicInsnNode(
                "render",
                PalladiumCoreOverlay1201.OVERLAY_FACTORY_DESC,
                LAMBDA_METAFACTORY,
                Type.getMethodType("(L" + PalladiumCoreOverlay1201.FORGE_GUI
                        + ";L" + PalladiumCoreOverlay1201.GUI_GRAPHICS + ";FII)V"),
                new Handle(H_INVOKESTATIC, PalladiumCoreOverlay1201.OVERLAY_REGISTRY,
                        "lambda$onRegisterGuiOverlays$0",
                        PalladiumCoreOverlay1201.RENDER_LAMBDA_DESC, false),
                Type.getMethodType("(L" + PalladiumCoreOverlay1201.FORGE_GUI
                        + ";L" + PalladiumCoreOverlay1201.GUI_GRAPHICS + ";FII)V")));
        registration.instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL, PalladiumCoreOverlay1201.REGISTER_EVENT, "registerAboveAll",
                PalladiumCoreOverlay1201.REGISTER_METHOD_DESC, false));
        registration.instructions.add(new InsnNode(RETURN));
        registration.maxStack = 3;
        registration.maxLocals = 3;
        node.methods.add(registration);

        MethodNode render = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$onRegisterGuiOverlays$0",
                PalladiumCoreOverlay1201.RENDER_LAMBDA_DESC, null, null);
        render.instructions.add(new VarInsnNode(ALOAD, 0));
        render.instructions.add(new MethodInsnNode(
                INVOKESTATIC, PalladiumCoreOverlay1201.MINECRAFT, "getInstance",
                "()L" + PalladiumCoreOverlay1201.MINECRAFT + ";", false));
        render.instructions.add(new VarInsnNode(ALOAD, 1));
        render.instructions.add(new VarInsnNode(ALOAD, 2));
        render.instructions.add(new VarInsnNode(FLOAD, 3));
        render.instructions.add(new VarInsnNode(ILOAD, 4));
        render.instructions.add(new VarInsnNode(ILOAD, 5));
        render.instructions.add(new MethodInsnNode(
                INVOKEINTERFACE, PalladiumCoreOverlay1201.INGAME_OVERLAY, "render",
                PalladiumCoreOverlay1201.RENDER_METHOD_DESC, true));
        render.instructions.add(new InsnNode(RETURN));
        render.maxStack = 7;
        render.maxLocals = 6;
        node.methods.add(render);
        return node;
    }

    @Test
    void preservesPalladiumCoreCallbackThroughExistingNeoForgeLayerShim() {
        ClassNode node = fixture();
        int methodCount = node.methods.size();

        assertEquals(1, PalladiumCoreOverlay1201.apply(node));

        assertEquals(methodCount, node.methods.size(), "the validated original callback must not be rebuilt");
        assertTrue(node.methods.stream()
                .anyMatch(method -> method.name.equals("onRegisterGuiOverlays")
                        && method.desc.equals(PalladiumCoreOverlay1201.EVENT_DESC)));
        assertTrue(node.methods.stream()
                .anyMatch(method -> method.name.equals("lambda$onRegisterGuiOverlays$1")
                        && method.desc.equals(PalladiumCoreOverlay1201.REGISTER_LAMBDA_DESC)));
        assertTrue(node.methods.stream()
                .anyMatch(method -> method.name.equals("lambda$onRegisterGuiOverlays$0")
                        && method.desc.equals(PalladiumCoreOverlay1201.RENDER_LAMBDA_DESC)));
        assertFalse(node.methods.stream().anyMatch(method -> method.desc.contains("Lnet/minecraftforge/")), "direct Forge API descriptors must be remapped; the legacyconnector/.../net/minecraftforge shim descriptors are intentional");
        assertEquals(1, PalladiumCoreOverlay1201.apply(node), "recognition must be idempotent");
    }

    @Test
    void refusesAnUnexpectedOverlayEventShape() {
        ClassNode node = fixture();
        node.methods.removeIf(method -> method.name.equals("lambda$onRegisterGuiOverlays$1"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> PalladiumCoreOverlay1201.apply(node));
        assertTrue(error.getMessage().contains("Forge 1.20.1 GUI overlay shim contract"));
        assertTrue(error.getMessage().contains(PalladiumCoreOverlay1201.REGISTER_EVENT));
    }

    @Test
    void doesNotTouchUnrelatedClasses() {
        ClassNode node = fixture();
        node.name = "net/threetag/palladiumcore/registry/client/OtherRegistry";
        int methodCount = node.methods.size();

        assertEquals(0, PalladiumCoreOverlay1201.apply(node));
        assertEquals(methodCount, node.methods.size());
    }
}
