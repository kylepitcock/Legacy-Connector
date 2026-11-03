package legacyconnector.v1_7_10.net.minecraft.client.renderer.culling;

/**
 * 1.7.10's frustum planes. The arrays are kept for mods that read or fill them (a subclass may compute its own planes); the test
 * uses the planes when something filled them and otherwise asks the modern level renderer's culling frustum.
 */
public class ClippingHelper {
    public float[][] frustum = new float[6][4];
    public float[] projectionMatrix = new float[16];
    public float[] modelviewMatrix = new float[16];
    public float[] clippingMatrix = new float[16];

    /** Whether any plane is set (an all-zero helper would otherwise cull everything). */
    private boolean mcc$hasPlanes() {
        for (float[] plane : frustum) {
            if (plane == null) continue;
            for (float v : plane) if (v != 0.0F) return true;
        }
        return false;
    }

    /** Whether the box is (partly) in view. */
    public boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (mcc$hasPlanes()) {
            for (int i = 0; i < 6; i++) {
                float[] p = frustum[i];
                boolean inside = false;
                for (int c = 0; c < 8 && !inside; c++) {
                    double x = (c & 1) == 0 ? minX : maxX;
                    double y = (c & 2) == 0 ? minY : maxY;
                    double z = (c & 4) == 0 ? minZ : maxZ;
                    inside = p[0] * x + p[1] * y + p[2] * z + p[3] > 0.0D;
                }
                if (!inside) return false;
            }
            return true;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        var f = mc.levelRenderer == null ? null : mc.levelRenderer.getFrustum();
        if (f == null) return true;
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        return f.isVisible(new net.minecraft.world.phys.AABB(minX + cam.x, minY + cam.y, minZ + cam.z, maxX + cam.x, maxY + cam.y, maxZ + cam.z));
    }

    /** Whether the box is entirely inside every plane (vanilla's {@code isBoxInFrustumFully}). */
    public boolean isBoxInFrustumFully(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!mcc$hasPlanes()) return isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
        for (int i = 0; i < 6; i++) {
            float[] p = frustum[i];
            for (int c = 0; c < 8; c++) {
                double x = (c & 1) == 0 ? minX : maxX;
                double y = (c & 2) == 0 ? minY : maxY;
                double z = (c & 4) == 0 ? minZ : maxZ;
                if (p[0] * x + p[1] * y + p[2] * z + p[3] <= 0.0D) return false;
            }
        }
        return true;
    }
}
