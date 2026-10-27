package legacyconnector.v1_12_2.org.lwjgl.util.glu;

import legacyconnector.v1_12_2.org.lwjgl.opengl.GL11;

/** LWJGL 2's GLU projection helpers. Matrix state is not emulated yet (see the GL projection issue), so they set up the frustum only. */
public class Project {
    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        float height = (float) Math.tan(Math.toRadians(fovy) / 2.0D) * zNear;
        float width = height * aspect;
        GL11.glFrustum(-width, width, -height, height, zNear, zFar);
    }

    /**
     * Maps object coordinates to window coordinates through the given (column-major) model-view and projection matrices and
     * viewport (x, y, width, height); writes x, y, depth into {@code winPos}. False when the point is at infinity.
     */
    public static boolean gluProject(float objx, float objy, float objz, java.nio.FloatBuffer modelMatrix, java.nio.FloatBuffer projMatrix,
            java.nio.IntBuffer viewport, java.nio.FloatBuffer winPos) {
        org.joml.Matrix4f model = new org.joml.Matrix4f().set(modelMatrix.position(), modelMatrix);
        org.joml.Matrix4f proj = new org.joml.Matrix4f().set(projMatrix.position(), projMatrix);
        org.joml.Vector4f v = new org.joml.Vector4f(objx, objy, objz, 1.0F);
        model.transform(v);
        proj.transform(v);
        if (v.w == 0.0F) return false;
        float half = 1.0F / v.w * 0.5F;
        float x = v.x * half + 0.5F, y = v.y * half + 0.5F, z = v.z * half + 0.5F;
        int vp = viewport.position();
        int p = winPos.position();
        winPos.put(p, x * viewport.get(vp + 2) + viewport.get(vp));
        winPos.put(p + 1, y * viewport.get(vp + 3) + viewport.get(vp + 1));
        winPos.put(p + 2, z);
        return true;
    }

    /** The inverse of {@link #gluProject}: window coordinates and depth back to object coordinates, written into {@code objPos}. */
    public static boolean gluUnProject(float winx, float winy, float winz, java.nio.FloatBuffer modelMatrix, java.nio.FloatBuffer projMatrix,
            java.nio.IntBuffer viewport, java.nio.FloatBuffer objPos) {
        org.joml.Matrix4f model = new org.joml.Matrix4f().set(modelMatrix.position(), modelMatrix);
        org.joml.Matrix4f m = new org.joml.Matrix4f().set(projMatrix.position(), projMatrix).mul(model);
        if (Math.abs(m.determinant()) < 1.0E-30F) return false;
        m.invert();
        int vp = viewport.position();
        float x = (winx - viewport.get(vp)) / viewport.get(vp + 2);
        float y = (winy - viewport.get(vp + 1)) / viewport.get(vp + 3);
        org.joml.Vector4f v = new org.joml.Vector4f(x * 2.0F - 1.0F, y * 2.0F - 1.0F, winz * 2.0F - 1.0F, 1.0F);
        m.transform(v);
        if (v.w == 0.0F) return false;
        float inv = 1.0F / v.w;
        int p = objPos.position();
        objPos.put(p, v.x * inv);
        objPos.put(p + 1, v.y * inv);
        objPos.put(p + 2, v.z * inv);
        return true;
    }
}
