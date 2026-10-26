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

    /**
     * Deletes a shader program. Legacy GLSL programs never reach the driver on a core profile (see GlCompat), so only a real
     * program is deleted.
     */
    public static void glDeleteProgram(int program) {
        if (RenderSystem.isOnRenderThread() && org.lwjgl.opengl.GL20.glIsProgram(program)) org.lwjgl.opengl.GL20.glDeleteProgram(program);
    }

    public static void glDeleteShader(int shader) {
        if (RenderSystem.isOnRenderThread() && org.lwjgl.opengl.GL20.glIsShader(shader)) org.lwjgl.opengl.GL20.glDeleteShader(shader);
    }

    /** The uniform's location in a real linked program; -1 otherwise (setting -1 is ignored, as in GL). */
    public static int glGetUniformLocation(int program, CharSequence name) {
        if (!RenderSystem.isOnRenderThread() || !org.lwjgl.opengl.GL20.glIsProgram(program)) return -1;
        return org.lwjgl.opengl.GL20.glGetUniformLocation(program, name);
    }

    /**
     * Binding a program does not affect the emulated pipeline: legacy geometry is batched into the modern renderer's buffers and
     * drawn later with its own shaders, so switching programs here would only desynchronise the modern renderer's state.
     */
    public static void glUseProgram(int program) {
    }

    // ------------------------------------------------------------------ framebuffer objects (GL 3.0 on every modern context)

    public static int func_153165_e() {
        return RenderSystem.isOnRenderThread() ? com.mojang.blaze3d.platform.GlStateManager.glGenFramebuffers() : -1;
    }

    public static int func_153185_f() {
        return RenderSystem.isOnRenderThread() ? com.mojang.blaze3d.platform.GlStateManager.glGenRenderbuffers() : -1;
    }

    public static int func_153167_i(int target) {
        return RenderSystem.isOnRenderThread() ? com.mojang.blaze3d.platform.GlStateManager.glCheckFramebufferStatus(target) : -1;
    }

    /**
     * Binds a framebuffer. Framebuffer 0 was the screen in 1.7.10; the modern game draws into its main render target instead, so
     * a mod "returning to the screen" gets that target.
     */
    public static void func_153171_g(int target, int framebuffer) {
        if (!RenderSystem.isOnRenderThread()) return;
        if (framebuffer == 0) {
            var main = net.minecraft.client.Minecraft.getInstance().getMainRenderTarget();
            if (main != null) framebuffer = main.frameBufferId;
        }
        com.mojang.blaze3d.platform.GlStateManager._glBindFramebuffer(target, framebuffer);
    }

    public static void func_153174_h(int framebuffer) {
        if (RenderSystem.isOnRenderThread()) com.mojang.blaze3d.platform.GlStateManager._glDeleteFramebuffers(framebuffer);
    }

    public static void func_153176_h(int target, int renderbuffer) {
        if (RenderSystem.isOnRenderThread()) com.mojang.blaze3d.platform.GlStateManager._glBindRenderbuffer(target, renderbuffer);
    }

    public static void func_153184_g(int renderbuffer) {
        if (RenderSystem.isOnRenderThread()) com.mojang.blaze3d.platform.GlStateManager._glDeleteRenderbuffers(renderbuffer);
    }

    public static void func_153186_a(int target, int internalFormat, int width, int height) {
        if (RenderSystem.isOnRenderThread()) com.mojang.blaze3d.platform.GlStateManager._glRenderbufferStorage(target, internalFormat, width, height);
    }

    public static void func_153188_a(int target, int attachment, int textureTarget, int texture, int level) {
        if (RenderSystem.isOnRenderThread()) com.mojang.blaze3d.platform.GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
    }

    public static void func_153190_b(int target, int attachment, int renderbufferTarget, int renderbuffer) {
        if (RenderSystem.isOnRenderThread()) {
            com.mojang.blaze3d.platform.GlStateManager._glFramebufferRenderbuffer(target, attachment, renderbufferTarget, renderbuffer);
        }
    }
}
