package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Adapts AlienEvo's first-person alien arm renderer to GeckoLib 4.9.x.
 * GeckoLib added a trailing partial-tick parameter to GeoModel.handleAnimations;
 * AlienEvo's AnimationState already carries that exact value.
 */
final class AlienEvoGeckoRenderLayer1201 implements Opcodes {
    static final String TARGET_CLASS =
            "net/habb/alienevo/compat/geckolib/renderlayer/AlienGeckoRenderLayer";
    static final String GEO_MODEL = PalladiumGeckoRenderLayer1201.GEO_MODEL;
    static final String ANIMATION_STATE = PalladiumGeckoRenderLayer1201.ANIMATION_STATE;

    static final String OLD_HANDLE_ANIMATIONS_DESC =
            PalladiumGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC;
    static final String NEW_HANDLE_ANIMATIONS_DESC =
            PalladiumGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC;

    private AlienEvoGeckoRenderLayer1201() {}

    /**
     * The observed AlienEvo dev.24 call is in renderArmImpl. Immediately before
     * the invocation the stack is [GeoModel, GeoAnimatable, long, AnimationState].
     * Duplicate AnimationState and read its partial tick to append GeckoLib's
     * new float argument without changing the existing state argument.
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
                if (!"renderArmImpl".equals(method.name) || call.getOpcode() != INVOKEVIRTUAL) {
                    throw new IllegalStateException(
                            "AlienEvo AlienGeckoRenderLayer has an unexpected GeoModel.handleAnimations call site");
                }
                if (OLD_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    legacyCalls++;
                    targetMethod = method;
                    targetCall = call;
                } else if (NEW_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    adaptedCalls++;
                } else {
                    throw new IllegalStateException(
                            "AlienEvo AlienGeckoRenderLayer has an unknown GeoModel.handleAnimations descriptor: "
                                    + call.desc);
                }
            }
        }

        // Permit an idempotent second pass over an already adapted AlienEvo class.
        if (legacyCalls == 0 && adaptedCalls == 1) return 0;
        if (legacyCalls != 1 || adaptedCalls != 0 || targetMethod == null || targetCall == null) {
            throw new IllegalStateException(
                    "AlienEvo AlienGeckoRenderLayer expected exactly one legacy GeoModel.handleAnimations call in "
                            + "renderArmImpl; found " + legacyCalls + " legacy and " + adaptedCalls + " adapted");
        }

        InsnList partialTickArgument = new InsnList();
        partialTickArgument.add(new InsnNode(DUP));
        partialTickArgument.add(new MethodInsnNode(
                INVOKEVIRTUAL, ANIMATION_STATE, "getPartialTick", "()F", false));
        targetMethod.instructions.insertBefore(targetCall, partialTickArgument);
        targetCall.desc = NEW_HANDLE_ANIMATIONS_DESC;
        targetMethod.maxStack++;
        return 1;
    }
}
