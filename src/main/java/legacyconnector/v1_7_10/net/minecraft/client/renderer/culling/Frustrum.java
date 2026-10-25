package legacyconnector.v1_7_10.net.minecraft.client.renderer.culling;

import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;

/**
 * The camera frustum. The modern renderer culls by itself, so everything counts as visible here: legacy renderers that test their
 * boxes still draw (the modern renderer decides what reaches the screen).
 */
public class Frustrum implements ICamera {
    private double x;
    private double y;
    private double z;

    @Override
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isBoundingBoxInFrustum(AxisAlignedBB box) {
        return true;
    }

    public boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return true;
    }
}
