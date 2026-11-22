package org.kylepitcock.legacyconnector.loader.transform.compat;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Replaces AlienEvo's removed RenderSystem inverse-view getter with the 1.21.1 camera rotation bridge. */
public final class AlienEvoBoneAnchors1201 implements Opcodes {
    public static final String TARGET = "net/habb/alienevo/client/BoneAnchors";
    public static final String RENDER_SYSTEM = "com/mojang/blaze3d/systems/RenderSystem";
    public static final String HELPER = "org/kylepitcock/legacyconnector/v1_20_1/compat/AlienEvoCameraRotation1201Runtime";
    public static final String DESC = "()Lorg/joml/Matrix3f;";
    public static final String LEGACY_NAME = "getInverseViewRotationMatrix";
    public static final String HELPER_NAME = "getInverseViewRotationMatrix";

    private AlienEvoBoneAnchors1201() {}

    public static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        int legacyCalls = 0;
        int helperCalls = 0;
        MethodInsnNode legacy = null;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (var instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.owner.equals(RENDER_SYSTEM) && call.name.equals(LEGACY_NAME)) {
                    legacyCalls++;
                    if (call.getOpcode() != INVOKESTATIC || !call.desc.equals(DESC) || call.itf) {
                        throw new IllegalStateException("AlienEvo BoneAnchors inverse-view call has an unknown bytecode shape");
                    }
                    legacy = call;
                } else if (call.owner.equals(HELPER) && call.name.equals(HELPER_NAME)
                        && call.getOpcode() == INVOKESTATIC && call.desc.equals(DESC) && !call.itf) {
                    helperCalls++;
                }
            }
        }

        if (legacyCalls == 0 && helperCalls == 1) return 0;
        if (legacyCalls != 1 || helperCalls != 0) {
            throw new IllegalStateException("AlienEvo BoneAnchors expected exactly one unmigrated inverse-view call; found "
                    + legacyCalls + " legacy and " + helperCalls + " migrated");
        }

        legacy.owner = HELPER;
        legacy.name = HELPER_NAME;
        legacy.itf = false;
        return 1;
    }
}
