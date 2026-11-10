package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Migrates Palladium 4.5.10's SuitStand render layer from the 1.20.1
 * nine-float RenderLayer color-copy helper to the 1.21.1 overload that takes
 * six animation floats followed by one packed ARGB tint.
 */
final class PalladiumRenderLayer1201 implements Opcodes {
    static final String COLORABLE_SUIT_STAND_LAYER =
            "net/threetag/palladium/client/renderer/renderlayer/ColorableSuitStandLayer";

    private static final String RENDER_LAYER =
            "net/minecraft/client/renderer/entity/layers/RenderLayer";
    private static final String ENTITY_MODEL = "net/minecraft/client/model/EntityModel";
    private static final String RESOURCE_LOCATION = "net/minecraft/resources/ResourceLocation";
    private static final String POSE_STACK = "com/mojang/blaze3d/vertex/PoseStack";
    private static final String BUFFER_SOURCE = "net/minecraft/client/renderer/MultiBufferSource";
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";

    private static final String METHOD = "coloredCutoutModelCopyLayerRender";
    private static final String OLD_DESC =
            "(L" + ENTITY_MODEL + ";L" + ENTITY_MODEL + ";L" + RESOURCE_LOCATION
                    + ";L" + POSE_STACK + ";L" + BUFFER_SOURCE + ";IL" + LIVING_ENTITY
                    + ";FFFFFFFFF)V";
    private static final String NEW_DESC =
            "(L" + ENTITY_MODEL + ";L" + ENTITY_MODEL + ";L" + RESOURCE_LOCATION
                    + ";L" + POSE_STACK + ";L" + BUFFER_SOURCE + ";IL" + LIVING_ENTITY
                    + ";FFFFFFI)V";

    private static final String COLOR_HELPER = "legacyconnector/v1_20_1/PalladiumColorCompat";
    private static final String PACK_RGB = "packRgb";
    private static final String PACK_RGB_DESC = "(FFF)I";

    private PalladiumRenderLayer1201() {
    }

    /** Rewrites exactly one known inherited static helper call in the SuitStand color layer. */
    static int apply(ClassNode node) {
        if (!COLORABLE_SUIT_STAND_LAYER.equals(node.name)) {
            return 0;
        }
        if (!RENDER_LAYER.equals(node.superName)) {
            throw new IllegalStateException(
                    "Palladium ColorableSuitStandLayer no longer extends the expected RenderLayer");
        }

        MethodNode render = null;
        for (MethodNode method : node.methods) {
            if ("render".equals(method.name)
                    && method.desc.equals("(L" + POSE_STACK + ";L" + BUFFER_SOURCE
                            + ";ILnet/threetag/palladium/entity/SuitStand;FFFFFF)V")) {
                if (render != null) {
                    throw new IllegalStateException("Palladium ColorableSuitStandLayer has duplicate render methods");
                }
                render = method;
            }
        }
        if (render == null || render.instructions == null) {
            throw new IllegalStateException("Palladium ColorableSuitStandLayer render method was not found");
        }

        int legacyCalls = 0;
        int adaptedCalls = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !METHOD.equals(call.name)) {
                    continue;
                }
                if (OLD_DESC.equals(call.desc)) {
                    if (method != render || call.getOpcode() != INVOKESTATIC
                            || !COLORABLE_SUIT_STAND_LAYER.equals(call.owner)) {
                        throw new IllegalStateException(
                                "Palladium ColorableSuitStandLayer has an unexpected legacy RenderLayer helper call");
                    }
                    legacyCalls++;
                } else if (NEW_DESC.equals(call.desc)) {
                    if (method != render || call.getOpcode() != INVOKESTATIC
                            || !COLORABLE_SUIT_STAND_LAYER.equals(call.owner)) {
                        throw new IllegalStateException(
                                "Palladium ColorableSuitStandLayer has an unexpected adapted RenderLayer helper call");
                    }
                    adaptedCalls++;
                }
            }
        }

        if (legacyCalls == 0 && adaptedCalls == 1) {
            return 0;
        }
        if (legacyCalls != 1 || adaptedCalls != 0) {
            throw new IllegalStateException(
                    "Palladium ColorableSuitStandLayer expected exactly one legacy RenderLayer helper call; found "
                            + legacyCalls + " legacy and " + adaptedCalls + " adapted");
        }

        MethodInsnNode target = null;
        for (AbstractInsnNode instruction : render.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && COLORABLE_SUIT_STAND_LAYER.equals(call.owner)
                    && METHOD.equals(call.name)
                    && OLD_DESC.equals(call.desc)) {
                target = call;
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Palladium SuitStand RenderLayer call disappeared during validation");
        }

        // The three legacy RGB floats are the final operands on the stack.
        // Pack them in place; the six preceding animation floats remain untouched.
        render.instructions.insertBefore(target,
                new MethodInsnNode(INVOKESTATIC, COLOR_HELPER, PACK_RGB, PACK_RGB_DESC, false));
        render.instructions.set(target,
                new MethodInsnNode(INVOKESTATIC, COLORABLE_SUIT_STAND_LAYER, METHOD, NEW_DESC, false));
        return 1;
    }
}
