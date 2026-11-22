package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.Minecraft;
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
}
