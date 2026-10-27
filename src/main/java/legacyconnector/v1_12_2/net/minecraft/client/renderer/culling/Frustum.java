package legacyconnector.v1_12_2.net.minecraft.client.renderer.culling;

import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;

/** 1.12.2 frustum camera: boxes in world coordinates, tested against the clipping helper relative to the camera. */
public class Frustum implements ICamera {
    private final ClippingHelper clippingHelper;
    private double x;
    private double y;
    private double z;

    public Frustum() {
        this(ClippingHelperImpl.getInstance());
    }

    public Frustum(ClippingHelper clippingHelperIn) {
        this.clippingHelper = clippingHelperIn;
    }

    @Override
    public void setPosition(double xIn, double yIn, double zIn) {
        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
    }

    /** World box. 1.12 subtracted the position set (the camera); the modern frustum sits at the actual camera, so that is used. */
    public boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return clippingHelper.isBoxInFrustum(minX - cam.x, minY - cam.y, minZ - cam.z, maxX - cam.x, maxY - cam.y, maxZ - cam.z);
    }

    @Override
    public boolean isBoundingBoxInFrustum(AxisAlignedBB box) {
        return isBoxInFrustum(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}
