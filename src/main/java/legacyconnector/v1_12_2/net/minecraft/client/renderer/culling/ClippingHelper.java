package legacyconnector.v1_12_2.net.minecraft.client.renderer.culling;

/**
 * 1.12.2 frustum planes. The matrices are kept for mods that read them; the test itself is the modern level renderer's culling
 * frustum (prepared for the current frame at the camera).
 */
public class ClippingHelper {
    public float[][] frustum = new float[6][4];
    public float[] projectionMatrix = new float[16];
    public float[] modelviewMatrix = new float[16];
    public float[] clippingMatrix = new float[16];

    /** Whether the box, in coordinates relative to the camera, is (partly) in view. */
    public boolean isBoxInFrustum(double x1, double y1, double z1, double x2, double y2, double z2) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var frustum = mc.levelRenderer == null ? null : mc.levelRenderer.getFrustum();
        if (frustum == null) return true;
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        return frustum.isVisible(new net.minecraft.world.phys.AABB(x1 + cam.x, y1 + cam.y, z1 + cam.z, x2 + cam.x, y2 + cam.y, z2 + cam.z));
    }
}
