package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Redirects Palladium SuitStand's removed Sheep float-array lookup to a 1.21.1 runtime bridge. */
final class PalladiumColor1201 implements Opcodes {
    static final String COLORABLE_SUIT_STAND_LAYER =
            "net/threetag/palladium/client/renderer/renderlayer/ColorableSuitStandLayer";
    private static final String SHEEP = "net/minecraft/world/entity/animal/Sheep";
    private static final String DYE_COLOR = "net/minecraft/world/item/DyeColor";
    private static final String RENDER_DESC =
            "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "ILnet/threetag/palladium/entity/SuitStand;FFFFFF)V";
    private static final String LEGACY_METHOD = "getColorArray";
    private static final String LEGACY_DESC = "(L" + DYE_COLOR + ";)[F";
    private static final String HELPER = "legacyconnector/v1_20_1/PalladiumColorCompat";
    private static final String HELPER_METHOD = "sheepColorArray";

    private PalladiumColor1201() {
    }

    /** Rewrites exactly the three old Sheep color calls in render(); rejects partial or changed shapes. */
    static int apply(ClassNode node) {
        if (!COLORABLE_SUIT_STAND_LAYER.equals(node.name)) return 0;

        MethodNode render = null;
        for (MethodNode method : node.methods) {
            if ("render".equals(method.name) && RENDER_DESC.equals(method.desc)) {
                if (render != null) throw new IllegalStateException("Palladium ColorableSuitStandLayer has duplicate render methods");
                render = method;
            }
        }
        if (render == null || render.instructions == null) {
            throw new IllegalStateException("Palladium ColorableSuitStandLayer render method was not found");
        }

        int legacyCalls = 0;
        int helperCalls = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (SHEEP.equals(call.owner) && LEGACY_METHOD.equals(call.name)) {
                    if (method != render || call.getOpcode() != INVOKESTATIC || !LEGACY_DESC.equals(call.desc)) {
                        throw new IllegalStateException("Palladium ColorableSuitStandLayer has an unexpected Sheep.getColorArray call shape");
                    }
                    legacyCalls++;
                } else if (HELPER.equals(call.owner) && HELPER_METHOD.equals(call.name)) {
                    if (method != render || call.getOpcode() != INVOKESTATIC || !LEGACY_DESC.equals(call.desc)) {
                        throw new IllegalStateException("Palladium ColorableSuitStandLayer has an unexpected adapted sheep-color call shape");
                    }
                    helperCalls++;
                }
            }
        }

        if (legacyCalls == 0 && helperCalls == 3) return 0;
        if (legacyCalls != 3 || helperCalls != 0) {
            throw new IllegalStateException("Palladium ColorableSuitStandLayer expected exactly three legacy Sheep color calls; found "
                    + legacyCalls + " legacy and " + helperCalls + " adapted");
        }

        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == INVOKESTATIC
                    && SHEEP.equals(call.owner) && LEGACY_METHOD.equals(call.name) && LEGACY_DESC.equals(call.desc)) {
                render.instructions.set(call, new MethodInsnNode(
                        INVOKESTATIC, HELPER, HELPER_METHOD, LEGACY_DESC, false));
            }
        }
        return legacyCalls;
    }
}
