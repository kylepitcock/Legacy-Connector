package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Adapts AlienEvo's GeckoLib-backed player model animation call to GeckoLib 4.9.x. */
final class AlienEvoGeckoRenderLayerModel1201 implements Opcodes {
    static final String TARGET_CLASS =
            "net/habb/alienevo/compat/geckolib/renderlayer/AlienGeckoRenderLayerModel";

    private AlienEvoGeckoRenderLayerModel1201() {}

    /**
     * The old call stack is [GeoModel, GeoAnimatable, long, AnimationState].
     * Duplicate the AnimationState to append GeckoLib 4.9's partial-tick argument.
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
                        || !"handleAnimations".equals(call.name)) continue;
                if (!"renderToBuffer".equals(method.name) || call.getOpcode() != INVOKEVIRTUAL) {
                    throw new IllegalStateException(
                            "AlienEvo AlienGeckoRenderLayerModel has an unexpected GeoModel.handleAnimations call site");
                }
                if (PalladiumGeckoRenderLayer1201.OLD_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    legacyCalls++;
                    targetMethod = method;
                    targetCall = call;
                } else if (PalladiumGeckoRenderLayer1201.NEW_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    adaptedCalls++;
                } else {
                    throw new IllegalStateException(
                            "AlienEvo AlienGeckoRenderLayerModel has an unknown GeoModel.handleAnimations descriptor: "
                                    + call.desc);
                }
            }
        }

        if (legacyCalls == 0 && adaptedCalls == 1) return 0;
        if (legacyCalls != 1 || adaptedCalls != 0 || targetMethod == null || targetCall == null) {
            throw new IllegalStateException(
                    "AlienEvo AlienGeckoRenderLayerModel expected exactly one legacy GeoModel.handleAnimations call; found "
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
        targetMethod.maxStack++;
        return 1;
    }
}
