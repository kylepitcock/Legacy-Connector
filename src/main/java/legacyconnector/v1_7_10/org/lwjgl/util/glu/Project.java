package legacyconnector.v1_7_10.org.lwjgl.util.glu;

import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** LWJGL 2's GLU projection helpers. Matrix state is not emulated yet (see the GL projection issue), so they set up the frustum only. */
public class Project {
    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        float height = (float) Math.tan(Math.toRadians(fovy) / 2.0D) * zNear;
        float width = height * aspect;
        GL11.glFrustum(-width, width, -height, height, zNear, zFar);
    }
}
