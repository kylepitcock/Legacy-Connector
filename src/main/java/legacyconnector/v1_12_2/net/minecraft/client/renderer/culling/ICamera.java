package legacyconnector.v1_12_2.net.minecraft.client.renderer.culling;

import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;

/** 1.12.2 view-frustum test for culling. */
public interface ICamera {
    boolean isBoundingBoxInFrustum(AxisAlignedBB box);

    void setPosition(double xIn, double yIn, double zIn);
}
