package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** 1.7.10 OpenGlHelper: texture units, separate blending, and the lightmap coordinates used as vertex light. */
public class OpenGlHelper {
    public static int defaultTexUnit = 33984;
    public static int lightmapTexUnit = 33985;
    public static float lastBrightnessX;
    public static float lastBrightnessY;
    public static boolean shadersSupported = false;
    public static boolean openGL21 = false;
    public static boolean framebufferSupported = true;
    public static boolean anisotropicFilteringSupported = true;

    public static void setActiveTexture(int unit) {
    }

    public static void setClientActiveTexture(int unit) {
    }

    /** Legacy code sets the lightmap (block, sky) coordinates for full-bright or custom lighting. */
    public static void setLightmapTextureCoords(int unit, float blockLight, float skyLight) {
        if (unit == lightmapTexUnit) {
            lastBrightnessX = blockLight;
            lastBrightnessY = skyLight;
            RenderContext.setForcedLight((int) blockLight & 0xFFFF | ((int) skyLight & 0xFFFF) << 16);
        }
    }

    public static void glBlendFunc(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        RenderContext.setBlendFunc(srcRgb, dstRgb);
    }

    public static boolean isFramebufferEnabled() {
        return true;
    }

    /** Whether framebuffers are available (always on a modern GL context). */
    public static boolean func_153193_b() {
        return true;
    }

    // ------------------------------------------------------------------ GLSL programs (GL 2.0; 1.7.10 fell back to the ARB extension)
    // GL calls are only made on the render thread; elsewhere they do nothing and creation returns 0 (no object).

    public static int glCreateProgram() {
        return RenderSystem.isOnRenderThread() ? org.lwjgl.opengl.GL20.glCreateProgram() : 0;
    }

    public static int glCreateShader(int type) {
        return RenderSystem.isOnRenderThread() ? org.lwjgl.opengl.GL20.glCreateShader(type) : 0;
    }

    /** The source is the buffer's remaining bytes (1.7.10's ShaderLoader passes the file's bytes). */
    public static void glShaderSource(int shader, java.nio.ByteBuffer source) {
        if (!RenderSystem.isOnRenderThread() || source == null) return;
        byte[] bytes = new byte[source.remaining()];
        source.duplicate().get(bytes);
        org.lwjgl.opengl.GL20.glShaderSource(shader, new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void glCompileShader(int shader) {
        if (RenderSystem.isOnRenderThread()) org.lwjgl.opengl.GL20.glCompileShader(shader);
    }

    public static void glAttachShader(int program, int shader) {
        if (RenderSystem.isOnRenderThread()) org.lwjgl.opengl.GL20.glAttachShader(program, shader);
    }

    public static void glLinkProgram(int program) {
        if (RenderSystem.isOnRenderThread()) org.lwjgl.opengl.GL20.glLinkProgram(program);
    }

    public static int glGetShaderi(int shader, int pname) {
        return RenderSystem.isOnRenderThread() ? org.lwjgl.opengl.GL20.glGetShaderi(shader, pname) : 0;
    }

    public static String glGetShaderInfoLog(int shader, int maxLength) {
        return RenderSystem.isOnRenderThread() ? org.lwjgl.opengl.GL20.glGetShaderInfoLog(shader, maxLength) : "";
    }

    public static int glGetProgrami(int program, int pname) {
        return RenderSystem.isOnRenderThread() ? org.lwjgl.opengl.GL20.glGetProgrami(program, pname) : 0;
    }

    public static String glGetProgramInfoLog(int program, int maxLength) {
        return RenderSystem.isOnRenderThread() ? org.lwjgl.opengl.GL20.glGetProgramInfoLog(program, maxLength) : "";
    }
}
