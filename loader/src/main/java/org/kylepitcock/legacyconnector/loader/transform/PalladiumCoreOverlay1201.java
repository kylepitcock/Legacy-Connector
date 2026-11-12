package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * PalladiumCore 1.0.1 registers Palladium's ability bar, wheel, and power
 * overlays through Forge's RegisterGuiOverlaysEvent/IGuiOverlay API. NeoForge
 * 1.21.1 replaced that API with RegisterGuiLayersEvent/LayeredDraw.Layer.
 *
 * <p>This rewrites only PalladiumCore's OverlayRegistryImpl. The bridge keeps
 * the original IngameOverlay callback contract, supplying the current
 * Minecraft GUI, render partial tick, and scaled GUI dimensions.</p>
 */
final class PalladiumCoreOverlay1201 implements Opcodes {
    static final String OVERLAY_REGISTRY =
            "net/threetag/palladiumcore/registry/client/forge/OverlayRegistryImpl";
    static final String INGAME_OVERLAY =
            "net/threetag/palladiumcore/registry/client/OverlayRegistry$IngameOverlay";

    static final String REGISTER_EVENT =
            "net/neoforged/neoforge/client/event/RegisterGuiLayersEvent";
    static final String LAYER =
            "net/minecraft/client/gui/LayeredDraw$Layer";
    static final String GUI_GRAPHICS =
            "net/minecraft/client/gui/GuiGraphics";
    static final String DELTA_TRACKER =
            "net/minecraft/client/DeltaTracker";
    static final String MINECRAFT =
            "net/minecraft/client/Minecraft";
    static final String GUI =
            "net/minecraft/client/gui/Gui";
    static final String RESOURCE_LOCATION =
            "net/minecraft/resources/ResourceLocation";

    private static final String EVENT_DESC = "(L" + REGISTER_EVENT + ";)V";
    private static final String REGISTER_LAMBDA_DESC =
            "(L" + REGISTER_EVENT + ";Ljava/lang/String;L" + INGAME_OVERLAY + ";)V";
    private static final String RENDER_LAMBDA_DESC =
            "(L" + INGAME_OVERLAY + ";L" + GUI_GRAPHICS + ";L" + DELTA_TRACKER + ";)V";
    private static final String LAYER_FACTORY_DESC =
            "(L" + INGAME_OVERLAY + ";)L" + LAYER + ";";
    private static final String LAYER_RENDER_DESC =
            "(L" + GUI_GRAPHICS + ";L" + DELTA_TRACKER + ";)V";

    private static final Handle LAMBDA_METAFACTORY = new Handle(
            H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                    + "Ljava/lang/invoke/CallSite;",
            false);

    private PalladiumCoreOverlay1201() {
    }

    /**
     * @return one when PalladiumCore's registry class was migrated, otherwise zero
     */
    static int apply(ClassNode node) {
        if (!OVERLAY_REGISTRY.equals(node.name)) {
            return 0;
        }

        MethodNode original = null;
        for (MethodNode method : node.methods) {
            if ("onRegisterGuiOverlays".equals(method.name)
                    && EVENT_DESC.equals(method.desc)) {
                original = method;
                break;
            }
        }
        if (original == null) {
            boolean alreadyMigrated = node.methods.stream().anyMatch(method ->
                    "onRegisterGuiOverlays".equals(method.name)
                            && EVENT_DESC.equals(method.desc)
                            && node.methods.stream().anyMatch(candidate ->
                                    "lambda$onRegisterGuiOverlays$0".equals(candidate.name)
                                            && RENDER_LAMBDA_DESC.equals(candidate.desc)));
            if (alreadyMigrated) return 0;
            throw new IllegalStateException(
                    "PalladiumCore OverlayRegistryImpl no longer contains the expected "
                            + "RegisterGuiLayersEvent callback; its GUI overlay integration needs review");
        }

        // The old callback and its two Forge-specific lambda targets are not
        // usable on 1.21.1. Keep the method annotation and rebuild all three.
        node.methods.removeIf(method ->
                "onRegisterGuiOverlays".equals(method.name)
                        || "lambda$onRegisterGuiOverlays$0".equals(method.name)
                        || "lambda$onRegisterGuiOverlays$1".equals(method.name));

        MethodNode handler = new MethodNode(
                original.access,
                "onRegisterGuiOverlays",
                EVENT_DESC,
                original.signature,
                original.exceptions == null ? null : original.exceptions.toArray(String[]::new));
        handler.visibleAnnotations = original.visibleAnnotations;
        handler.invisibleAnnotations = original.invisibleAnnotations;
        handler.visibleTypeAnnotations = original.visibleTypeAnnotations;
        handler.invisibleTypeAnnotations = original.invisibleTypeAnnotations;
        handler.instructions.add(new FieldInsnNode(
                GETSTATIC, OVERLAY_REGISTRY, "OVERLAYS", "Ljava/util/Map;"));
        handler.instructions.add(new VarInsnNode(ALOAD, 0));
        handler.instructions.add(new InvokeDynamicInsnNode(
                "accept",
                "(L" + REGISTER_EVENT + ";)Ljava/util/function/BiConsumer;",
                LAMBDA_METAFACTORY,
                Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;)V"),
                new Handle(H_INVOKESTATIC, OVERLAY_REGISTRY,
                        "lambda$onRegisterGuiOverlays$1", REGISTER_LAMBDA_DESC, false),
                Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;)V")));
        handler.instructions.add(new MethodInsnNode(
                INVOKEINTERFACE, "java/util/Map", "forEach",
                "(Ljava/util/function/BiConsumer;)V", true));
        handler.instructions.add(new InsnNode(RETURN));
        handler.maxStack = 2;
        handler.maxLocals = 1;
        node.methods.add(handler);

        node.methods.add(createRegisterLambda());
        node.methods.add(createRenderLambda());
        return 1;
    }

    private static MethodNode createRegisterLambda() {
        MethodNode method = new MethodNode(
                ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$onRegisterGuiOverlays$1",
                REGISTER_LAMBDA_DESC,
                null,
                null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new LdcInsnNode("palladiumcore"));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new MethodInsnNode(
                INVOKESTATIC, RESOURCE_LOCATION, "fromNamespaceAndPath",
                "(Ljava/lang/String;Ljava/lang/String;)L" + RESOURCE_LOCATION + ";", false));
        method.instructions.add(new VarInsnNode(ALOAD, 2));
        method.instructions.add(new InvokeDynamicInsnNode(
                "render",
                LAYER_FACTORY_DESC,
                LAMBDA_METAFACTORY,
                Type.getMethodType(LAYER_RENDER_DESC),
                new Handle(H_INVOKESTATIC, OVERLAY_REGISTRY,
                        "lambda$onRegisterGuiOverlays$0", RENDER_LAMBDA_DESC, false),
                Type.getMethodType(LAYER_RENDER_DESC)));
        method.instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL, REGISTER_EVENT, "registerAboveAll",
                "(L" + RESOURCE_LOCATION + ";L" + LAYER + ";)V", false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 3;
        method.maxLocals = 3;
        return method;
    }

    private static MethodNode createRenderLambda() {
        MethodNode method = new MethodNode(
                ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$onRegisterGuiOverlays$0",
                RENDER_LAMBDA_DESC,
                null,
                null);
        method.instructions.add(new MethodInsnNode(
                INVOKESTATIC, MINECRAFT, "getInstance",
                "()L" + MINECRAFT + ";", false));
        method.instructions.add(new VarInsnNode(ASTORE, 3));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 3));
        method.instructions.add(new VarInsnNode(ALOAD, 3));
        method.instructions.add(new FieldInsnNode(
                GETFIELD, MINECRAFT, "gui", "L" + GUI + ";"));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(ALOAD, 2));
        method.instructions.add(new InsnNode(ICONST_0));
        method.instructions.add(new MethodInsnNode(
                INVOKEINTERFACE, DELTA_TRACKER, "getGameTimeDeltaPartialTick",
                "(Z)F", true));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL, GUI_GRAPHICS, "guiWidth", "()I", false));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL, GUI_GRAPHICS, "guiHeight", "()I", false));
        method.instructions.add(new MethodInsnNode(
                INVOKEINTERFACE, INGAME_OVERLAY, "render",
                "(L" + MINECRAFT + ";L" + GUI + ";L" + GUI_GRAPHICS + ";FII)V", true));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 8;
        method.maxLocals = 4;
        return method;
    }
}
