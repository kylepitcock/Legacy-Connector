package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/**
 * The camera's orientation as billboard rotation components (particles, labels and overlays that always face the viewer).
 * Updated once per frame from the modern camera (see RenderEvents1710).
 */
public class ActiveRenderInfo {
    public static float objectX;
    public static float objectY;
    public static float objectZ;
    public static float rotationX;
    public static float rotationXZ;
    public static float rotationZ;
    public static float rotationYZ;
    public static float rotationXY;

    public static void mcc$update(float yawDegrees, float pitchDegrees) {
        float yaw = yawDegrees * (float) Math.PI / 180.0F;
        float pitch = pitchDegrees * (float) Math.PI / 180.0F;
        rotationX = (float) Math.cos(yaw);
        rotationZ = (float) Math.sin(yaw);
        rotationYZ = -rotationZ * (float) Math.sin(pitch);
        rotationXY = rotationX * (float) Math.sin(pitch);
        rotationXZ = (float) Math.cos(pitch);
    }
}
