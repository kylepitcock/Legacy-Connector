package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Verifies PalladiumCore 1.0.1's Forge overlay registry against Legacy Connector's
 * existing Forge 1.20.1 overlay shims.
 *
 * <p>The normal 1.20.1 remapper intentionally maps {@code RegisterGuiOverlaysEvent},
 * {@code ForgeGui}, and {@code IGuiOverlay} to the compatibility shim classes (the
 * shim takes precedence over the direct Forge-to-NeoForge class rename). The runtime
 * shim posts the old event to each legacy mod's mod bus from NeoForge's
 * {@code RegisterGuiLayersEvent}, and its registration methods create
 * {@code LayeredDraw.Layer} callbacks. Rebuilding PalladiumCore's lambdas here would
 * bypass that established event-bus path and duplicate the bridge.</p>
 *
 * <p>This pass therefore validates the exact remapped callback contract and leaves
 * PalladiumCore's original map iteration and render callback intact.</p>
 */
final class PalladiumCoreOverlay1201 implements Opcodes {
    static final String OVERLAY_REGISTRY =
            "net/threetag/palladiumcore/registry/client/forge/OverlayRegistryImpl";
    static final String INGAME_OVERLAY =
            "net/threetag/palladiumcore/registry/client/OverlayRegistry$IngameOverlay";

    static final String REGISTER_EVENT =
            "legacyconnector/v1_20_1/net/minecraftforge/client/event/RegisterGuiOverlaysEvent";
    static final String FORGE_GUI =
            "legacyconnector/v1_20_1/net/minecraftforge/client/gui/overlay/ForgeGui";
    static final String FORGE_OVERLAY =
            "legacyconnector/v1_20_1/net/minecraftforge/client/gui/overlay/IGuiOverlay";
    static final String MINECRAFT = "net/minecraft/client/Minecraft";
    static final String GUI = "net/minecraft/client/gui/Gui";
    static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";

    static final String EVENT_DESC = "(L" + REGISTER_EVENT + ";)V";
    static final String REGISTER_LAMBDA_DESC =
            "(L" + REGISTER_EVENT + ";Ljava/lang/String;L" + INGAME_OVERLAY + ";)V";
    static final String RENDER_LAMBDA_DESC =
            "(L" + INGAME_OVERLAY + ";L" + FORGE_GUI + ";L" + GUI_GRAPHICS + ";FII)V";
    static final String REGISTER_METHOD_DESC =
            "(Ljava/lang/String;L" + FORGE_OVERLAY + ";)V";
    static final String RENDER_METHOD_DESC =
            "(L" + MINECRAFT + ";L" + GUI + ";L" + GUI_GRAPHICS + ";FII)V";
    static final String OVERLAY_FACTORY_DESC =
            "(L" + INGAME_OVERLAY + ";)L" + FORGE_OVERLAY + ";";
    static final String ACCEPT_FACTORY_DESC =
            "(L" + REGISTER_EVENT + ";)Ljava/util/function/BiConsumer;";

    private static final String SUBSCRIBE_EVENT =
            "Lnet/neoforged/bus/api/SubscribeEvent;";
    private static final String MAP_FOR_EACH_DESC =
            "(Ljava/util/function/BiConsumer;)V";

    private PalladiumCoreOverlay1201() {
    }

    /** @return one when PalladiumCore's original callback is correctly routed through the existing shim */
    static int apply(ClassNode node) {
        if (!OVERLAY_REGISTRY.equals(node.name)) {
            return 0;
        }

        MethodNode handler = find(node, "onRegisterGuiOverlays", EVENT_DESC);
        MethodNode registration = find(node, "lambda$onRegisterGuiOverlays$1", REGISTER_LAMBDA_DESC);
        MethodNode render = find(node, "lambda$onRegisterGuiOverlays$0", RENDER_LAMBDA_DESC);

        boolean hasSubscriber = hasAnnotation(handler, SUBSCRIBE_EVENT);
        boolean hasMapIteration = hasCall(handler, INVOKEINTERFACE,
                "java/util/Map", "forEach", MAP_FOR_EACH_DESC);
        boolean hasAcceptLambda = hasLambda(handler, "accept", ACCEPT_FACTORY_DESC,
                "lambda$onRegisterGuiOverlays$1", REGISTER_LAMBDA_DESC);
        boolean hasOverlayFactory = hasLambda(registration, "render", OVERLAY_FACTORY_DESC,
                "lambda$onRegisterGuiOverlays$0", RENDER_LAMBDA_DESC);
        boolean hasShimRegistration = hasCall(registration, INVOKEVIRTUAL,
                REGISTER_EVENT, "registerAboveAll", REGISTER_METHOD_DESC);
        boolean hasLegacyRenderCallback = hasCall(render, INVOKEINTERFACE,
                INGAME_OVERLAY, "render", RENDER_METHOD_DESC);

        if (handler != null && registration != null && render != null
                && hasSubscriber && hasMapIteration && hasAcceptLambda
                && hasOverlayFactory && hasShimRegistration && hasLegacyRenderCallback) {
            return 1;
        }

        List<String> handlerCandidates = node.methods.stream()
                .filter(method -> "onRegisterGuiOverlays".equals(method.name))
                .map(method -> method.name + method.desc)
                .toList();
        throw new IllegalStateException(
                "PalladiumCore OverlayRegistryImpl does not match Legacy Connector's "
                        + "Forge 1.20.1 GUI overlay shim contract. Expected "
                        + "onRegisterGuiOverlays" + EVENT_DESC
                        + ", its two original shim-backed lambda methods, the mod-bus "
                        + "@SubscribeEvent annotation, and calls to RegisterGuiOverlaysEvent.registerAboveAll "
                        + "and IngameOverlay.render. Found handlers: " + handlerCandidates
                        + ". Check the Forge event class remapping and overlay shim before changing this adapter.");
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static boolean hasAnnotation(MethodNode method, String descriptor) {
        return method != null && (hasAnnotation(method.visibleAnnotations, descriptor)
                || hasAnnotation(method.invisibleAnnotations, descriptor));
    }

    private static boolean hasAnnotation(List<AnnotationNode> annotations, String descriptor) {
        return annotations != null && annotations.stream().anyMatch(annotation -> descriptor.equals(annotation.desc));
    }

    private static boolean hasCall(
            MethodNode method, int opcode, String owner, String name, String descriptor) {
        return method != null && Arrays.stream(method.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.getOpcode() == opcode
                        && owner.equals(call.owner)
                        && name.equals(call.name)
                        && descriptor.equals(call.desc));
    }

    private static boolean hasLambda(
            MethodNode method, String name, String factoryDescriptor,
            String implementationName, String implementationDescriptor) {
        if (method == null) {
            return false;
        }
        return Arrays.stream(method.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof InvokeDynamicInsnNode dynamic
                        && name.equals(dynamic.name)
                        && factoryDescriptor.equals(dynamic.desc)
                        && Arrays.stream(dynamic.bsmArgs).anyMatch(argument ->
                                argument instanceof Handle handle
                                        && H_INVOKESTATIC == handle.getTag()
                                        && OVERLAY_REGISTRY.equals(handle.getOwner())
                                        && implementationName.equals(handle.getName())
                                        && implementationDescriptor.equals(handle.getDesc())));
    }
}
