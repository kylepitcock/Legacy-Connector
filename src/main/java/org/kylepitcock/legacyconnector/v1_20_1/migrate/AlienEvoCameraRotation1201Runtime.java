package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix3f;

/**
 * Runtime bridge for AlienEvo's 1.20.1 RenderSystem inverse-view rotation getter.
 *
 * <p>In 1.21.1 the active camera owns its rotation directly. The inverse view
 * rotation is exactly that camera orientation, so return a detached JOML
 * matrix rather than relying on the removed RenderSystem global.</p>
 */
public final class AlienEvoCameraRotation1201Runtime {
    private AlienEvoCameraRotation1201Runtime() {}

    public static Matrix3f getInverseViewRotationMatrix() {
        return new Matrix3f().rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
    }

    /**
     * AlienEvo stores first-person named-bone matrices under {@code boneName + arm.name()}.
     * This mirrors its arm-key naming convention for the ability-side lookup.
     */
    public static String firstPersonArmBoneKey(String boneName) {
        if (boneName == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        return firstPersonArmBoneKey(
                boneName, minecraft.player == null ? null : minecraft.player.getMainArm());
    }

    /**
     * AlienEvo renders and stores the two first-person arm poses separately.
     * Side-qualified source bones (for example beamRight / beamLeft) must keep
     * their own side even when that side is not the player's main hand. This is
     * important for abilities that average two anchors into a single midpoint.
     */
    static String firstPersonArmBoneKey(String boneName, HumanoidArm mainArm) {
        if (boneName == null) return null;
        HumanoidArm resolvedArm = armFromBoneName(boneName);
        if (resolvedArm == null) resolvedArm = mainArm;
        return resolvedArm == null ? boneName : boneName + resolvedArm.name();
    }

    private static HumanoidArm armFromBoneName(String boneName) {
        String normalized = boneName.toLowerCase(Locale.ROOT);
        if (normalized.endsWith("right")) return HumanoidArm.RIGHT;
        if (normalized.endsWith("left")) return HumanoidArm.LEFT;
        return null;
    }
}
