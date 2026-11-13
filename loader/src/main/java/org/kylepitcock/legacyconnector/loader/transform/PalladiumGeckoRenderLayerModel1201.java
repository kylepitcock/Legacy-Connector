package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Adapts Palladium's GeckoRenderLayerModel#renderToBuffer animation call to
 * GeckoLib 4.9.x. The model creates AnimationState with the current frame time,
 * so getPartialTick() preserves the partial tick already selected by Palladium.
 */
final class PalladiumGeckoRenderLayerModel1201 implements Opcodes {
    static final String TARGET_CLASS =
            "net/threetag/palladium/compat/geckolib/renderlayer/GeckoRenderLayerModel";

    private PalladiumGeckoRenderLayerModel1201() {}

    /**
     * Before the old invocation the operand stack contains:
     * [GeoModel, GeoAnimatable, long, AnimationState].
     * Duplicate the state, read its partial tick, and pass that as the new
     * trailing float parameter required by GeckoLib 4.9.x.
     */
    static int apply(ClassNode node) {
        if (!TARGET_CLASS.equals(node.name)) return 0;

        int legacyCalls = 0;
        int adaptedCalls = 0;
        MethodNode targetMethod = null;
        MethodInsnNode targetCall = null;

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !PalladiumGeckoRenderLayer1201.GEO_MODEL.equals(call.owner)
                        || !"handleAnimations".equals(call.name)) {
                    continue;
                }
                if (!"renderToBuffer".equals(method.name) || call.getOpcode() != INVOKEVIRTUAL) {
                    throw new IllegalStateException(
                            "Palladium GeckoRenderLayerModel has an unexpected GeoModel.handleAnimations call site");
                }
                if (PalladiumGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    legacyCalls++;
                    targetMethod = method;
                    targetCall = call;
                } else if (PalladiumGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    adaptedCalls++;
                } else {
                    throw new IllegalStateException(
                            "Palladium GeckoRenderLayerModel has an unknown GeoModel.handleAnimations descriptor: "
                                    + call.desc);
                }
            }
        }

        // Permit a repeated transformation pass over an already adapted class.
        if (legacyCalls == 0 && adaptedCalls == 1) return 0;
        if (legacyCalls != 1 || adaptedCalls != 0 || targetMethod == null || targetCall == null) {
            throw new IllegalStateException(
                    "Palladium GeckoRenderLayerModel expected exactly one legacy GeoModel.handleAnimations call; found "
                            + legacyCalls + " legacy and " + adaptedCalls + " adapted");
        }

        InsnList partialTickArgument = new InsnList();
        partialTickArgument.add(new InsnNode(DUP));
        partialTickArgument.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                PalladiumGeckoRenderLayer1201.ANIMATION_STATE,
                "getPartialTick",
                "()F",
                false));
        targetMethod.instructions.insertBefore(targetCall, partialTickArgument);
        targetCall.desc = PalladiumGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC;
        // The duplicated AnimationState adds one slot to the peak operand stack.
        targetMethod.maxStack++;
        return 1;
    }
}
