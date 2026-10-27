package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * 1.12.2 OpenGlHelper: the 1.7.10 shim's texture units, lightmap coordinates, separate blending and GLSL wrappers, plus the
 * names 1.12.2 gave the framebuffer and buffer-object functions. The context is always modern GL, so every path is the core one.
 */
public class OpenGlHelper extends legacyconnector.v1_7_10.net.minecraft.client.renderer.OpenGlHelper {
    public static boolean nvidia, ati, arbMultitexture = false, arbTextureEnvCombine = false, arbShaders = false, arbVbo = false,
            extBlendFuncSeparate = false, vboSupportedAti = false;
    public static boolean openGL14 = true, vboSupported = true, shadersAvailable = false;
    public static int GL_FRAMEBUFFER = 36160, GL_RENDERBUFFER = 36161, GL_COLOR_ATTACHMENT0 = 36064, GL_DEPTH_ATTACHMENT = 36096,
            GL_FRAMEBUFFER_COMPLETE = 36053, GL_FB_INCOMPLETE_ATTACHMENT = 36054, GL_FB_INCOMPLETE_MISS_ATTACH = 36055,
            GL_FB_INCOMPLETE_DRAW_BUFFER = 36059, GL_FB_INCOMPLETE_READ_BUFFER = 36060;
    public static int GL_LINK_STATUS = 35714, GL_COMPILE_STATUS = 35713, GL_VERTEX_SHADER = 35633, GL_FRAGMENT_SHADER = 35632;
    public static int GL_TEXTURE2 = 33986, GL_COMBINE = 34160, GL_INTERPOLATE = 34165, GL_PRIMARY_COLOR = 34167, GL_CONSTANT = 34166,
            GL_PREVIOUS = 34168, GL_COMBINE_RGB = 34161, GL_SOURCE0_RGB = 34176, GL_SOURCE1_RGB = 34177, GL_SOURCE2_RGB = 34178,
            GL_OPERAND0_RGB = 34192, GL_OPERAND1_RGB = 34193, GL_OPERAND2_RGB = 34194, GL_COMBINE_ALPHA = 34162,
            GL_SOURCE0_ALPHA = 34184, GL_SOURCE1_ALPHA = 34185, GL_SOURCE2_ALPHA = 34186, GL_OPERAND0_ALPHA = 34200,
            GL_OPERAND1_ALPHA = 34201, GL_OPERAND2_ALPHA = 34202;
    public static int GL_ARRAY_BUFFER = 34962, GL_STATIC_DRAW = 35044;
    public static FboMode framebufferType = FboMode.BASE;
    private static String logText = "Using framebuffer objects because OpenGL 3.0 is supported.\nUsing GL 1.3 multitexturing.\nUsing GL 1.3 texture combiners.\nVBOs are available because OpenGL 1.5 is supported.\n";

    public static void initializeTextures() {
    }

    public static boolean areShadersSupported() {
        return shadersSupported;
    }

    public static String getLogText() {
        return logText;
    }

    public static String getCpu() {
        return System.getProperty("os.arch");
    }

    public static boolean useVbo() {
        return true;
    }

    /** Debug arrows the F3 screen drew: the modern debug screen draws its own. */
    public static void renderDirections(int size) {
    }

    public static void openFile(java.io.File file) {
        net.minecraft.Util.getPlatform().openFile(file);
    }

    // ------------------------------------------------------------------ uniforms (real programs only; see glGetUniformLocation)

    public static void glUniform1i(int location, int v0) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform1i(location, v0);
    }

    public static void glUniform1(int location, IntBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform1iv(location, values);
    }

    public static void glUniform1(int location, FloatBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform1fv(location, values);
    }

    public static void glUniform2(int location, IntBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform2iv(location, values);
    }

    public static void glUniform2(int location, FloatBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform2fv(location, values);
    }

    public static void glUniform3(int location, IntBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform3iv(location, values);
    }

    public static void glUniform3(int location, FloatBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform3fv(location, values);
    }

    public static void glUniform4(int location, IntBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform4iv(location, values);
    }

    public static void glUniform4(int location, FloatBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniform4fv(location, values);
    }

    public static void glUniformMatrix2(int location, boolean transpose, FloatBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniformMatrix2fv(location, transpose, values);
    }

    public static void glUniformMatrix3(int location, boolean transpose, FloatBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniformMatrix3fv(location, transpose, values);
    }

    public static void glUniformMatrix4(int location, boolean transpose, FloatBuffer values) {
        if (RenderSystem.isOnRenderThread() && location >= 0) org.lwjgl.opengl.GL20.glUniformMatrix4fv(location, transpose, values);
    }

    public static int glGetAttribLocation(int program, CharSequence name) {
        if (!RenderSystem.isOnRenderThread() || !org.lwjgl.opengl.GL20.glIsProgram(program)) return -1;
        return org.lwjgl.opengl.GL20.glGetAttribLocation(program, name);
    }

    // ------------------------------------------------------------------ buffer objects (the emulated ones of the GL15 shim)

    public static int glGenBuffers() {
        return legacyconnector.v1_12_2.org.lwjgl.opengl.GL15.glGenBuffers();
    }

    public static void glBindBuffer(int target, int buffer) {
        legacyconnector.v1_12_2.org.lwjgl.opengl.GL15.glBindBuffer(target, buffer);
    }

    public static void glBufferData(int target, ByteBuffer data, int usage) {
        legacyconnector.v1_12_2.org.lwjgl.opengl.GL15.glBufferData(target, data, usage);
    }

    public static void glDeleteBuffers(int buffer) {
        legacyconnector.v1_12_2.org.lwjgl.opengl.GL15.glDeleteBuffers(buffer);
    }

    // ------------------------------------------------------------------ framebuffer objects (1.12.2 names of the 1.7.10 helpers)

    public static void glBindFramebuffer(int target, int framebuffer) {
        func_153171_g(target, framebuffer);
    }

    public static void glBindRenderbuffer(int target, int renderbuffer) {
        func_153176_h(target, renderbuffer);
    }

    public static void glDeleteRenderbuffers(int renderbuffer) {
        func_153184_g(renderbuffer);
    }

    public static void glDeleteFramebuffers(int framebuffer) {
        func_153174_h(framebuffer);
    }

    public static int glGenFramebuffers() {
        return func_153165_e();
    }

    public static int glGenRenderbuffers() {
        return func_153185_f();
    }

    public static void glRenderbufferStorage(int target, int internalFormat, int width, int height) {
        func_153186_a(target, internalFormat, width, height);
    }

    public static void glFramebufferRenderbuffer(int target, int attachment, int renderBufferTarget, int renderBuffer) {
        func_153190_b(target, attachment, renderBufferTarget, renderBuffer);
    }

    public static int glCheckFramebufferStatus(int target) {
        return func_153167_i(target);
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        func_153188_a(target, attachment, textarget, texture, level);
    }

    public enum FboMode {
        BASE,
        ARB,
        EXT
    }
}
