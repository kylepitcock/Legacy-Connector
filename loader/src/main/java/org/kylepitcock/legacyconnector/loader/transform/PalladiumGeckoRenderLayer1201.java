package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Adapts Palladium 4.5.10's GeckoLib 4.4-era first-person arm call to the
 * GeckoLib 4.9.x API. GeckoLib added a partial-tick parameter to
 * GeoModel.handleAnimations; the existing AnimationState already carries the
 * same partial tick, so read it rather than inventing a replacement value.
 */
final class PalladiumGeckoRenderLayer1201 implements Opcodes {
    static final String TARGET_CLASS = "net/threetag/palladium/compat/geckolib/renderlayer/GeckoRenderLayer";
    static final String GEO_MODEL = "software/bernie/geckolib/model/GeoModel";
    static final String GEO_ANIMATABLE = "software/bernie/geckolib/animatable/GeoAnimatable";
    static final String ANIMATION_STATE = "software/bernie/geckolib/animation/AnimationState";

    static final String OLD_HANDLE_ANIMATIONS_DESC =
            "(L" + GEO_ANIMATABLE + ";JL" + ANIMATION_STATE + ";)V";
    static final String NEW_HANDLE_ANIMATIONS_DESC =
            "(L" + GEO_ANIMATABLE + ";JL" + ANIMATION_STATE + ";F)V";

    private PalladiumGeckoRenderLayer1201() {}

    /**
     * Rewrites only the known Palladium first-person arm call. Stack before
     * the old call: [GeoModel, GeoAnimatable, long, AnimationState].
     * Duplicating AnimationState and calling getPartialTick() appends the new
     * float argument while preserving the original state argument.
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
                        || !GEO_MODEL.equals(call.owner)
                        || !"handleAnimations".equals(call.name)) {
                    continue;
                }
                if (!"renderArm".equals(method.name) || call.getOpcode() != INVOKEVIRTUAL) {
                    throw new IllegalStateException(
                            "Palladium GeckoRenderLayer has an unexpected GeoModel.handleAnimations call site");
                }
                if (OLD_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    legacyCalls++;
                    targetMethod = method;
                    targetCall = call;
                } else if (NEW_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    adaptedCalls++;
                } else {
                    throw new IllegalStateException(
                            "Palladium GeckoRenderLayer has an unknown GeoModel.handleAnimations descriptor: "
                                    + call.desc);
                }
            }
        }

        // Permit the transformer to be run again on an already adapted class.
        if (legacyCalls == 0 && adaptedCalls == 1) return 0;
        if (legacyCalls != 1 || adaptedCalls != 0 || targetMethod == null || targetCall == null) {
            throw new IllegalStateException(
                    "Palladium GeckoRenderLayer expected exactly one legacy GeoModel.handleAnimations call; found "
                            + legacyCalls + " legacy and " + adaptedCalls + " adapted");
        }

        InsnList partialTickArgument = new InsnList();
        partialTickArgument.add(new InsnNode(DUP));
        partialTickArgument.add(new MethodInsnNode(
                INVOKEVIRTUAL, ANIMATION_STATE, "getPartialTick", "()F", false));
        targetMethod.instructions.insertBefore(targetCall, partialTickArgument);
        targetCall.desc = NEW_HANDLE_ANIMATIONS_DESC;
        // The duplicated AnimationState adds one slot to the peak operand stack.
        targetMethod.maxStack++;
        return 1;
    }
}
