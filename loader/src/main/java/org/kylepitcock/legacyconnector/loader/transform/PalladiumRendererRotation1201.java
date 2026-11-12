package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Palladium 4.5.10 renderer overrides/calls written against the 1.20.1
 * three-float LivingEntityRenderer.setupRotations signature.
 *
 * Minecraft 1.21.1 adds a final scale argument. The connector's shared
 * RoundCompat.defaultScale rule supplies 1.0 for legacy call sites, so these
 * Palladium-specific forms use the same default.
 */
final class PalladiumRendererRotation1201 implements Opcodes {
    static final String SUIT_STAND_RENDERER =
            "net/threetag/palladium/client/renderer/entity/SuitStandRenderer";
    static final String TRAIL_SEGMENT_RENDERER =
            "net/threetag/palladium/client/renderer/entity/TrailSegmentEntityRenderer";

    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String SUIT_STAND = "net/threetag/palladium/entity/SuitStand";
    private static final String POSE_STACK = "com/mojang/blaze3d/vertex/PoseStack";

    private static final String OLD_BASE_DESC =
            "(L" + LIVING_ENTITY + ";L" + POSE_STACK + ";FFF)V";
    private static final String NEW_BASE_DESC =
            "(L" + LIVING_ENTITY + ";L" + POSE_STACK + ";FFFF)V";
    private static final String OLD_TYPED_DESC =
            "(L" + SUIT_STAND + ";L" + POSE_STACK + ";FFF)V";
    private static final String NEW_TYPED_DESC =
            "(L" + SUIT_STAND + ";L" + POSE_STACK + ";FFFF)V";
    private static final String SETUP_ROTATIONS = "setupRotations";

    private PalladiumRendererRotation1201() {
    }

    /** Returns the number of renderer methods/call sites migrated. */
    static int apply(ClassNode node) {
        if (SUIT_STAND_RENDERER.equals(node.name)) return migrateSuitStandOverride(node);
        if (TRAIL_SEGMENT_RENDERER.equals(node.name)) return migrateTrailSegmentCall(node);
        return 0;
    }

    /**
     * SuitStandRenderer declares the typed override plus javac's erased
     * LivingEntity bridge. Both descriptors must gain the fourth float, and
     * the bridge forwards the legacy default scale to the typed override.
     */
    private static int migrateSuitStandOverride(ClassNode node) {
        MethodNode typedOld = find(node, SETUP_ROTATIONS, OLD_TYPED_DESC);
        MethodNode bridgeOld = find(node, SETUP_ROTATIONS, OLD_BASE_DESC);
        MethodNode typedNew = find(node, SETUP_ROTATIONS, NEW_TYPED_DESC);
        MethodNode bridgeNew = find(node, SETUP_ROTATIONS, NEW_BASE_DESC);

        if (typedOld == null && bridgeOld == null && typedNew != null && bridgeNew != null) return 0;
        if (typedOld == null || bridgeOld == null || typedNew != null || bridgeNew != null) {
            throw new IllegalStateException(
                    "Palladium SuitStandRenderer setupRotations override/bridge no longer matches the inspected 1.20.1 form");
        }
        if ((bridgeOld.access & (ACC_BRIDGE | ACC_SYNTHETIC)) != (ACC_BRIDGE | ACC_SYNTHETIC)) {
            throw new IllegalStateException("Palladium SuitStandRenderer erased setupRotations bridge lost its bridge/synthetic flags");
        }

        MethodInsnNode forward = null;
        for (AbstractInsnNode instruction : bridgeOld.instructions) {
            if (!(instruction instanceof MethodInsnNode call)
                    || !call.owner.equals(node.name)
                    || !SETUP_ROTATIONS.equals(call.name)
                    || !OLD_TYPED_DESC.equals(call.desc)) {
                continue;
            }
            if (forward != null) {
                throw new IllegalStateException("Palladium SuitStandRenderer bridge calls its typed override more than once");
            }
            forward = call;
        }
        if (forward == null) {
            throw new IllegalStateException("Palladium SuitStandRenderer erased bridge no longer forwards to its typed override");
        }

        bridgeOld.instructions.insertBefore(forward, new InsnNode(FCONST_1));
        forward.desc = NEW_TYPED_DESC;
        typedOld.desc = NEW_TYPED_DESC;
        bridgeOld.desc = NEW_BASE_DESC;

        // The fourth float occupies local 6 in both instance methods. The old
        // implementation deliberately ignores it; the bridge forwards 1.0F.
        typedOld.maxLocals = Math.max(typedOld.maxLocals, 7);
        bridgeOld.maxLocals = Math.max(bridgeOld.maxLocals, 7);
        bridgeOld.maxStack = Math.max(bridgeOld.maxStack + 1, 7);
        return 3;
    }

    /**
     * TrailSegmentEntityRenderer calls its inherited base method through its
     * own symbolic owner. That call is INVOKEVIRTUAL (not the @special form
     * handled by the generic table), so append the same legacy default scale.
     */
    private static int migrateTrailSegmentCall(ClassNode node) {
        MethodInsnNode oldCall = null;
        MethodInsnNode newCall = null;
        MethodNode ownerMethod = null;

        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !node.name.equals(call.owner)
                        || !SETUP_ROTATIONS.equals(call.name)) {
                    continue;
                }
                if (OLD_BASE_DESC.equals(call.desc)) {
                    if (oldCall != null) {
                        throw new IllegalStateException(
                                "Palladium TrailSegmentEntityRenderer has multiple legacy setupRotations calls");
                    }
                    oldCall = call;
                    ownerMethod = method;
                } else if (NEW_BASE_DESC.equals(call.desc)) {
                    if (newCall != null) {
                        throw new IllegalStateException(
                                "Palladium TrailSegmentEntityRenderer has multiple modern setupRotations calls");
                    }
                    newCall = call;
                }
            }
        }

        if (oldCall == null && newCall != null) return 0;
        if (oldCall == null || newCall != null || ownerMethod == null) {
            throw new IllegalStateException(
                    "Palladium TrailSegmentEntityRenderer no longer has the inspected inherited setupRotations call");
        }

        ownerMethod.instructions.insertBefore(oldCall, new InsnNode(FCONST_1));
        oldCall.desc = NEW_BASE_DESC;
        ownerMethod.maxStack = Math.max(ownerMethod.maxStack + 1, 7);
        return 1;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
    }
}
