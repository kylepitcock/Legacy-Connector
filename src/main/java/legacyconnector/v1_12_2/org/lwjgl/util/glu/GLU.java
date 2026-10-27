package legacyconnector.v1_12_2.org.lwjgl.util.glu;

import com.mojang.blaze3d.systems.RenderSystem;
import legacyconnector.v1_12_2.org.lwjgl.opengl.GL11;

/** LWJGL 2's GLU utility functions that still mean something on a core-profile context. */
public class GLU {
    public static final int GLU_INVALID_ENUM = 100900;
    public static final int GLU_INVALID_VALUE = 100901;
    public static final int GLU_OUT_OF_MEMORY = 100902;

    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        Project.gluPerspective(fovy, aspect, zNear, zFar);
    }

    public static boolean gluProject(float objx, float objy, float objz, java.nio.FloatBuffer modelMatrix, java.nio.FloatBuffer projMatrix,
            java.nio.IntBuffer viewport, java.nio.FloatBuffer winPos) {
        return Project.gluProject(objx, objy, objz, modelMatrix, projMatrix, viewport, winPos);
    }

    public static boolean gluUnProject(float winx, float winy, float winz, java.nio.FloatBuffer modelMatrix, java.nio.FloatBuffer projMatrix,
            java.nio.IntBuffer viewport, java.nio.FloatBuffer objPos) {
        return Project.gluUnProject(winx, winy, winz, modelMatrix, projMatrix, viewport, objPos);
    }

    public static void gluOrtho2D(float left, float right, float bottom, float top) {
        GL11.glOrtho(left, right, bottom, top, -1.0D, 1.0D);
    }

    /**
     * Uploads the image as level 0 of the bound texture and lets the driver build the smaller levels (GLU scaled and filtered
     * each level itself, to power-of-two sizes; modern GL takes any size). The legacy component counts 1-4 become the matching
     * base formats, since a core-profile context rejects bare counts. Returns 0, or a GLU error code.
     */
    public static int gluBuild2DMipmaps(int target, int components, int width, int height, int format, int type, java.nio.ByteBuffer data) {
        if (width < 1 || height < 1) return GLU_INVALID_VALUE;
        if (!RenderSystem.isOnRenderThread()) return 0;
        int internalFormat = switch (components) {
            case 1 -> org.lwjgl.opengl.GL11.GL_RED;
            case 2 -> org.lwjgl.opengl.GL30.GL_RG;
            case 3 -> org.lwjgl.opengl.GL11.GL_RGB;
            case 4 -> org.lwjgl.opengl.GL11.GL_RGBA;
            default -> components;
        };
        GL11.glTexImage2D(target, 0, internalFormat, width, height, 0, format, type, data);
        org.lwjgl.opengl.GL30.glGenerateMipmap(target);
        return 0;
    }

    public static String gluErrorString(int error) {
        return switch (error) {
            case 0 -> "No error";
            case 0x0500 -> "Invalid enum";
            case 0x0501 -> "Invalid value";
            case 0x0502 -> "Invalid operation";
            case 0x0503 -> "Stack overflow";
            case 0x0504 -> "Stack underflow";
            case 0x0505 -> "Out of memory";
            case GLU_INVALID_ENUM -> "Invalid enum (GLU)";
            case GLU_INVALID_VALUE -> "Invalid value (GLU)";
            case GLU_OUT_OF_MEMORY -> "Out of memory (GLU)";
            default -> null;
        };
    }
}
