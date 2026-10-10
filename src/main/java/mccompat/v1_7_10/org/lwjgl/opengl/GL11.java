package mccompat.v1_7_10.org.lwjgl.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import org.kylepitcock.mccompat.v1_7_10.client.RenderContext;
import org.slf4j.LoggerFactory;

/**
 * Legacy {@code org.lwjgl.opengl.GL11}. Extends LWJGL 3's GL11, so every constant and every core-profile function
 * resolves to the real one; the fixed-function calls that no longer exist on Minecraft 1.21's core profile (matrix
 * stack, current color, lighting, alpha test, immediate mode) are re-implemented here on top of
 * {@link RenderContext} instead of crashing the game.
 */
public class GL11 extends org.lwjgl.opengl.GL11 {
    private static boolean warnedImmediate;

    protected GL11() {
    }

    // ------------------------------------------------------------------ matrix stack

    public static void glPushMatrix() {
        RenderContext.push();
    }

    public static void glPopMatrix() {
        RenderContext.pop();
    }

    public static void glTranslatef(float x, float y, float z) {
        RenderContext.translate(x, y, z);
    }

    public static void glTranslated(double x, double y, double z) {
        RenderContext.translate(x, y, z);
    }

    public static void glRotatef(float angle, float x, float y, float z) {
        RenderContext.rotate(angle, x, y, z);
    }

    public static void glRotated(double angle, double x, double y, double z) {
        RenderContext.rotate((float) angle, (float) x, (float) y, (float) z);
    }

    public static void glScalef(float x, float y, float z) {
        RenderContext.scale(x, y, z);
    }

    public static void glScaled(double x, double y, double z) {
        RenderContext.scale((float) x, (float) y, (float) z);
    }

    public static void glLoadIdentity() {
    }

    public static void glMatrixMode(int mode) {
    }

    public static void glMultMatrix(FloatBuffer m) {
    }

    // ------------------------------------------------------------------ color

    public static void glColor4f(float r, float g, float b, float a) {
        RenderContext.color(r, g, b, a);
    }

    public static void glColor3f(float r, float g, float b) {
        RenderContext.color(r, g, b, 1.0F);
    }

    public static void glColor4d(double r, double g, double b, double a) {
        RenderContext.color((float) r, (float) g, (float) b, (float) a);
    }

    public static void glColor3d(double r, double g, double b) {
        RenderContext.color((float) r, (float) g, (float) b, 1.0F);
    }

    public static void glColor4ub(byte r, byte g, byte b, byte a) {
        RenderContext.color((r & 255) / 255F, (g & 255) / 255F, (b & 255) / 255F, (a & 255) / 255F);
    }

    public static void glColor3ub(byte r, byte g, byte b) {
        RenderContext.color((r & 255) / 255F, (g & 255) / 255F, (b & 255) / 255F, 1.0F);
    }

    // ------------------------------------------------------------------ capabilities

    public static void glEnable(int cap) {
        switch (cap) {
            case GL_BLEND -> RenderSystem.enableBlend();
            case GL_DEPTH_TEST -> RenderSystem.enableDepthTest();
            case GL_CULL_FACE -> RenderSystem.enableCull();
            case GL_SCISSOR_TEST -> GlStateManager._enableScissorTest();
            case GL_POLYGON_OFFSET_FILL -> RenderSystem.enablePolygonOffset();
            case GL_COLOR_LOGIC_OP -> RenderSystem.enableColorLogicOp();
            default -> {
                // GL_LIGHTING, GL_ALPHA_TEST, GL_TEXTURE_2D, GL_RESCALE_NORMAL, GL_COLOR_MATERIAL, GL_FOG, light sources:
                // fixed-function state with no core-profile equivalent; modern shaders handle these.
            }
        }
    }

    public static void glDisable(int cap) {
        switch (cap) {
            case GL_BLEND -> RenderSystem.disableBlend();
            case GL_DEPTH_TEST -> RenderSystem.disableDepthTest();
            case GL_CULL_FACE -> RenderSystem.disableCull();
            case GL_SCISSOR_TEST -> GlStateManager._disableScissorTest();
            case GL_POLYGON_OFFSET_FILL -> RenderSystem.disablePolygonOffset();
            case GL_COLOR_LOGIC_OP -> RenderSystem.disableColorLogicOp();
            default -> {
            }
        }
    }

    public static void glBlendFunc(int src, int dst) {
        RenderSystem.blendFunc(src, dst);
    }

    public static void glDepthMask(boolean flag) {
        RenderSystem.depthMask(flag);
    }

    public static void glAlphaFunc(int func, float ref) {
    }

    public static void glShadeModel(int mode) {
    }

    public static void glNormal3f(float x, float y, float z) {
    }

    public static void glLightModel(int pname, FloatBuffer params) {
    }

    public static void glPushAttrib(int mask) {
    }

    public static void glPopAttrib() {
    }

    public static void glCallList(int list) {
    }

    public static int glGenLists(int range) {
        return 0;
    }

    public static void glNewList(int list, int mode) {
    }

    public static void glEndList() {
    }

    public static void glDeleteLists(int list, int range) {
    }

    // ------------------------------------------------------------------ immediate mode (not emulated yet)

    private static void immediate() {
        if (!warnedImmediate) {
            warnedImmediate = true;
            LoggerFactory.getLogger("MCCompat/GL11").warn("A legacy mod uses immediate-mode OpenGL (glBegin/glVertex); this geometry is not drawn yet");
        }
    }

    public static void glBegin(int mode) {
        immediate();
    }

    public static void glEnd() {
    }

    public static void glVertex3f(float x, float y, float z) {
    }

    public static void glVertex3d(double x, double y, double z) {
    }

    public static void glVertex2f(float x, float y) {
    }

    public static void glVertex2i(int x, int y) {
    }

    public static void glTexCoord2f(float u, float v) {
    }

    public static void glTexCoord2d(double u, double v) {
    }
}
