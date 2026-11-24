package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Rewrites the other inspected AlienEvo consumers of the removed 1.20.1
 * RenderSystem inverse-view getter. BoneAnchors has its own coordinate-space
 * adapter; these additional paths share the same 1.21.1 camera-rotation bridge.
 */
final class AlienEvoCameraRotationCallers1201 implements Opcodes {
    static final String LEGACY_OWNER = "com/mojang/blaze3d/systems/RenderSystem";
    static final String LEGACY_NAME = "getInverseViewRotationMatrix";
    static final String HELPER_OWNER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoCameraRotation1201Runtime";
    static final String HELPER_NAME = "getInverseViewRotationMatrix";
    static final String DESCRIPTOR = "()Lorg/joml/Matrix3f;";

    static final String GRAPPLE_ABILITY = "net/habb/alienevo/ability/GrappleAbility";
    static final String GRAPPLE_METHOD = "resolveOrigin";
    static final String GRAPPLE_DESCRIPTOR =
            "(Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/threetag/palladium/power/ability/AbilityInstance;"
                    + "Lnet/minecraft/world/phys/Vec3;FZ)"
                    + "Lnet/habb/alienevo/ability/GrappleAbility$VisualOrigin;";

    static final String GRAB_RENDER_PLACEMENT = "net/habb/alienevo/client/GrabRenderPlacement";
    static final String GRAB_WORLD_ANCHOR = "worldAnchor";
    static final String GRAB_WORLD_ANCHOR_DESCRIPTOR =
            "(Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/habb/alienevo/aliens/provider/Alien$CarrySettings;DDD)"
                    + "Lnet/minecraft/world/phys/Vec3;";
    static final String CAPTURE_CARRIED_PLAYER_EYE = "captureCarriedPlayerEye";
    static final String CAPTURE_CARRIED_PLAYER_EYE_DESCRIPTOR =
            "(Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/entity/player/Player;"
                    + "Lorg/joml/Matrix4f;)V";

    static final String PLUMBER_SCAN_HOLOGRAM =
            "net/habb/alienevo/client/plumber/scan/PlumberScanHologram";
    static final String PLUMBER_RENDER_WORLD = "renderWorld";
    static final String PLUMBER_RENDER_WORLD_DESCRIPTOR =
            "(Lnet/minecraft/client/renderer/MultiBufferSource;F)V";

    private AlienEvoCameraRotationCallers1201() {}

    static int apply(ClassNode node) {
        List<MethodTarget> targets = targetsFor(node.name);
        if (targets.isEmpty()) return 0;

        List<MethodNode> methods = new ArrayList<>(targets.size());
        for (MethodTarget target : targets) {
            MethodNode method = findUniqueMethod(node, target.name(), target.descriptor());
            if (method == null || (method.access & ACC_STATIC) == 0) {
                throw new IllegalStateException(
                        "AlienEvo camera-rotation caller has an unexpected method shape: "
                                + node.name + "." + target.name() + target.descriptor());
            }
            methods.add(method);
        }

        int legacyCalls = 0;
        int helperCalls = 0;
        List<MethodInsnNode> references = new ArrayList<>(targets.size());

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;

                boolean legacyOwner = LEGACY_OWNER.equals(call.owner);
                boolean helperOwner = HELPER_OWNER.equals(call.owner);
                if (!legacyOwner && !helperOwner) {
                    if (LEGACY_NAME.equals(call.name) && DESCRIPTOR.equals(call.desc)) {
                        throw new IllegalStateException(
                                "AlienEvo camera-rotation call has an unknown owner in " + node.name);
                    }
                    continue;
                }

                if (!LEGACY_NAME.equals(call.name) || !DESCRIPTOR.equals(call.desc)
                        || call.getOpcode() != INVOKESTATIC || call.itf
                        || !methods.contains(method)) {
                    throw new IllegalStateException(
                            "AlienEvo camera-rotation call has an unexpected shape or location in " + node.name);
                }

                references.add(call);
                if (legacyOwner) legacyCalls++;
                else helperCalls++;
            }
        }

        if (references.size() != targets.size()) {
            throw new IllegalStateException(
                    "AlienEvo " + node.name + " expected " + targets.size()
                            + " inverse-view getter call(s); found " + references.size());
        }

        if (legacyCalls == targets.size() && helperCalls == 0) {
            for (MethodInsnNode call : references) call.owner = HELPER_OWNER;
            return references.size();
        }
        if (legacyCalls == 0 && helperCalls == targets.size()) return 0;

        throw new IllegalStateException(
                "AlienEvo " + node.name + " has a partial inverse-view migration: "
                        + legacyCalls + " legacy and " + helperCalls + " bridged call(s)");
    }

    private static List<MethodTarget> targetsFor(String className) {
        return switch (className) {
            case GRAPPLE_ABILITY ->
                    List.of(new MethodTarget(GRAPPLE_METHOD, GRAPPLE_DESCRIPTOR));
            case GRAB_RENDER_PLACEMENT ->
                    List.of(
                            new MethodTarget(GRAB_WORLD_ANCHOR, GRAB_WORLD_ANCHOR_DESCRIPTOR),
                            new MethodTarget(CAPTURE_CARRIED_PLAYER_EYE, CAPTURE_CARRIED_PLAYER_EYE_DESCRIPTOR));
            case PLUMBER_SCAN_HOLOGRAM ->
                    List.of(new MethodTarget(PLUMBER_RENDER_WORLD, PLUMBER_RENDER_WORLD_DESCRIPTOR));
            default -> List.of();
        };
    }

    private static MethodNode findUniqueMethod(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name) || !descriptor.equals(method.desc)) continue;
            if (found != null) {
                throw new IllegalStateException(
                        "Duplicate AlienEvo camera-rotation target " + node.name + "." + name + descriptor);
            }
            found = method;
        }
        return found;
    }

    private record MethodTarget(String name, String descriptor) {}
}
