package org.kylepitcock.legacyconnector.v1_20_1.migrate;

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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return boneName;
        return firstPersonArmBoneKey(boneName, minecraft.player.getMainArm());
    }

    static String firstPersonArmBoneKey(String boneName, HumanoidArm arm) {
        if (boneName == null || arm == null) return boneName;
        return boneName + arm.name();
    }
}
