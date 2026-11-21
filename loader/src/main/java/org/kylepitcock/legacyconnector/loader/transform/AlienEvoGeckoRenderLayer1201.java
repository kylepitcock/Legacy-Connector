package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Adapts AlienEvo's GeckoLib renderer calls to GeckoLib 4.9.x.
 * GeckoLib added a trailing partial-tick parameter to GeoModel.handleAnimations;
 * each existing AnimationState already carries that exact value.
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

    // Inspected from the supplied AlienEvo 1.5.0-dev.24 JAR:
    // render, renderArmImpl, and renderBoneSubtree each contain one call.
    static final int EXPECTED_CALL_SITES = 3;

    private AlienEvoGeckoRenderLayer1201() {}

    /**
     * Immediately before each invocation the stack contains
     * [GeoModel, GeoAnimatable, long, AnimationState]. Duplicate AnimationState
     * and read its partial tick to append GeckoLib's new float argument while
     * preserving the original state argument.
     */
    static int apply(ClassNode node) {
        if (!TARGET_CLASS.equals(node.name)) return 0;

        int legacyCalls = 0;
        int adaptedCalls = 0;
        List<MethodNode> targetMethods = new ArrayList<>();
        List<MethodInsnNode> targetCalls = new ArrayList<>();

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !GEO_MODEL.equals(call.owner)
                        || !"handleAnimations".equals(call.name)) {
                    continue;
                }
                if (call.getOpcode() != INVOKEVIRTUAL) {
                    throw new IllegalStateException(
                            "AlienEvo AlienGeckoRenderLayer has a non-virtual GeoModel.handleAnimations call");
                }
                if (OLD_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    legacyCalls++;
                    targetMethods.add(method);
                    targetCalls.add(call);
                } else if (NEW_HANDLE_ANIMATIONS_DESC.equals(call.desc)) {
                    adaptedCalls++;
                } else {
                    throw new IllegalStateException(
                            "AlienEvo AlienGeckoRenderLayer has an unknown GeoModel.handleAnimations descriptor: "
                                    + call.desc);
                }
            }
        }

        // The supplied dev.24 class has three call sites. Permit a repeated pass
        // only when all three are already migrated; reject mixed/partial states.
        if (legacyCalls == 0 && adaptedCalls == EXPECTED_CALL_SITES) return 0;
        if (legacyCalls != EXPECTED_CALL_SITES || adaptedCalls != 0) {
            throw new IllegalStateException(
                    "AlienEvo AlienGeckoRenderLayer expected exactly " + EXPECTED_CALL_SITES
                            + " legacy GeoModel.handleAnimations call sites; found "
                            + legacyCalls + " legacy and " + adaptedCalls + " adapted");
        }

        // maxStack is a per-method maximum, so increase it once per method even
        // when more than one invocation in that method is rewritten.
        List<MethodNode> stackExpandedMethods = new ArrayList<>();
        for (int i = 0; i < targetCalls.size(); i++) {
            MethodNode method = targetMethods.get(i);
            MethodInsnNode call = targetCalls.get(i);

            InsnList partialTickArgument = new InsnList();
            partialTickArgument.add(new InsnNode(DUP));
            partialTickArgument.add(new MethodInsnNode(
                    INVOKEVIRTUAL, ANIMATION_STATE, "getPartialTick", "()F", false));
            method.instructions.insertBefore(call, partialTickArgument);
            call.desc = NEW_HANDLE_ANIMATIONS_DESC;

            if (!stackExpandedMethods.contains(method)) {
                method.maxStack++;
                stackExpandedMethods.add(method);
            }
        }
        return legacyCalls;
    }
}
