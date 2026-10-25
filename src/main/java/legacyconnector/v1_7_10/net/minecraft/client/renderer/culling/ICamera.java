package legacyconnector.v1_7_10.net.minecraft.client.renderer.culling;

import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;

/** A view volume that things are tested against before they are drawn. */
public interface ICamera {
    boolean isBoundingBoxInFrustum(AxisAlignedBB box);

    void setPosition(double x, double y, double z);
}
