package legacyconnector.v1_7_10.org.lwjgl.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.slf4j.LoggerFactory;

/**
 * Legacy {@code org.lwjgl.opengl.GL11}. Extends LWJGL 3's GL11, so every constant and every core-profile function
 * resolves to the real one; the fixed-function calls that no longer exist on Minecraft 1.21's core profile (matrix
 * stack, current color, lighting, alpha test, immediate mode) are re-implemented here on top of
 * {@link RenderContext} instead of crashing the game.
 */
public class GL11 extends GL11Guard {

    protected GL11() {
    }

    // ------------------------------------------------------------------ texture queries

    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_TEXTURE_WIDTH = 4096;
    private static final int GL_TEXTURE_HEIGHT = 4097;

    /**
     * Legacy bind calls only tell the render context which texture is current, so the real GL binding is unrelated to it: width and
     * height of "the bound texture" (mods compute UVs from them, Custom Main Menu's images) are read from the texture the legacy code bound.
     */
    public static int glGetTexLevelParameteri(int target, int level, int pname) {
        if (target == org.lwjgl.opengl.GL11.GL_TEXTURE_2D && (pname == GL_TEXTURE_WIDTH || pname == GL_TEXTURE_HEIGHT) && RenderSystem.isOnRenderThread()) {
            var rl = RenderContext.texture();
            if (rl != null) {
                try {
                    int id = net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(rl).getId();
                    int previous = org.lwjgl.opengl.GL11.glGetInteger(GL_TEXTURE_BINDING_2D);
                    GlStateManager._bindTexture(id);
                    int value = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(target, level, pname);
                    GlStateManager._bindTexture(previous);
                    return value;
                } catch (RuntimeException | LinkageError e) {
                    // fall back to whatever is bound
                }
            }
        }
        return GL11Guard.glGetTexLevelParameteri(target, level, pname);
    }

    // ------------------------------------------------------------------ LWJGL 2 overloads without the "v" suffix

    private static final int GL_MODELVIEW_MATRIX = 0x0BA6;
    private static final int GL_PROJECTION_MATRIX = 0x0BA7;

    /** The matrix stack is emulated, so the current model-view and projection matrices come from the render context. */
    public static void glGetFloat(int pname, FloatBuffer params) {
        if (pname == GL_MODELVIEW_MATRIX && RenderContext.pose() != null) {
            RenderContext.pose().last().pose().get(params.position(), params);
        } else if (pname == GL_PROJECTION_MATRIX && RenderSystem.isOnRenderThread()) {
            RenderSystem.getProjectionMatrix().get(params.position(), params);
        } else {
            GL11Guard.glGetFloatv(pname, params);
        }
    }

    public static void glGetInteger(int pname, java.nio.IntBuffer params) {
        GL11Guard.glGetIntegerv(pname, params);
    }

    public static void glGetBoolean(int pname, java.nio.ByteBuffer params) {
        GL11Guard.glGetBooleanv(pname, params);
    }

    public static void glGetDouble(int pname, java.nio.DoubleBuffer params) {
        GL11Guard.glGetDoublev(pname, params);
    }

    public static void glTexParameter(int target, int pname, FloatBuffer params) {
        GL11Guard.glTexParameterfv(target, pname, params);
    }

    public static void glTexParameter(int target, int pname, java.nio.IntBuffer params) {
        GL11Guard.glTexParameteriv(target, pname, params);
    }

    public static void glTexGen(int coord, int pname, FloatBuffer params) {
    }

    public static void glTexEnv(int target, int pname, FloatBuffer params) {
    }

    public static void glTexEnv(int target, int pname, java.nio.IntBuffer params) {
    }

    public static void glLight(int light, int pname, FloatBuffer params) {
    }

    public static void glMaterial(int face, int pname, FloatBuffer params) {
    }

    public static void glFog(int pname, FloatBuffer params) {
    }

    public static void glLoadMatrix(FloatBuffer m) {
    }

    public static void glClipPlane(int plane, java.nio.DoubleBuffer equation) {
    }

    /** Binds a GL texture the mod created itself, and makes the emulated renderer draw with it. */
    public static void glBindTexture(int target, int texture) {
        if (DisplayLists.record(() -> glBindTexture(target, texture))) return;
        if (!RenderSystem.isOnRenderThread()) return;
        if (target == org.lwjgl.opengl.GL11.GL_TEXTURE_2D) {
            GlStateManager._bindTexture(texture);
            if (texture != 0) RenderContext.bindTexture(org.kylepitcock.legacyconnector.v1_7_10.client.RawGlTextures.location(texture));
        } else {
            GL11Guard.glBindTexture(target, texture);
        }
    }

    // ------------------------------------------------------------------ matrix stack

    public static void glPushMatrix() {
        if (DisplayLists.record(() -> glPushMatrix())) return;
        RenderContext.push();
    }

    public static void glPopMatrix() {
        if (DisplayLists.record(() -> glPopMatrix())) return;
        RenderContext.pop();
    }

    public static void glTranslatef(float x, float y, float z) {
        if (DisplayLists.record(() -> glTranslatef(x, y, z))) return;
        RenderContext.translate(x, y, z);
    }

    public static void glTranslated(double x, double y, double z) {
        if (DisplayLists.record(() -> glTranslated(x, y, z))) return;
        RenderContext.translate(x, y, z);
    }

    public static void glRotatef(float angle, float x, float y, float z) {
        if (DisplayLists.record(() -> glRotatef(angle, x, y, z))) return;
        RenderContext.rotate(angle, x, y, z);
    }

    public static void glRotated(double angle, double x, double y, double z) {
        if (DisplayLists.record(() -> glRotated(angle, x, y, z))) return;
        RenderContext.rotate((float) angle, (float) x, (float) y, (float) z);
    }

    public static void glScalef(float x, float y, float z) {
        if (DisplayLists.record(() -> glScalef(x, y, z))) return;
        RenderContext.scale(x, y, z);
    }

    public static void glScaled(double x, double y, double z) {
        if (DisplayLists.record(() -> glScaled(x, y, z))) return;
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
        if (DisplayLists.record(() -> glColor4f(r, g, b, a))) return;
        RenderContext.color(r, g, b, a);
    }

    public static void glColor3f(float r, float g, float b) {
        if (DisplayLists.record(() -> glColor3f(r, g, b))) return;
        RenderContext.color(r, g, b, 1.0F);
    }

    public static void glColor4d(double r, double g, double b, double a) {
        if (DisplayLists.record(() -> glColor4d(r, g, b, a))) return;
        RenderContext.color((float) r, (float) g, (float) b, (float) a);
    }

    public static void glColor3d(double r, double g, double b) {
        if (DisplayLists.record(() -> glColor3d(r, g, b))) return;
        RenderContext.color((float) r, (float) g, (float) b, 1.0F);
    }

    public static void glColor4ub(byte r, byte g, byte b, byte a) {
        if (DisplayLists.record(() -> glColor4ub(r, g, b, a))) return;
        RenderContext.color((r & 255) / 255F, (g & 255) / 255F, (b & 255) / 255F, (a & 255) / 255F);
    }

    public static void glColor3ub(byte r, byte g, byte b) {
        if (DisplayLists.record(() -> glColor3ub(r, g, b))) return;
        RenderContext.color((r & 255) / 255F, (g & 255) / 255F, (b & 255) / 255F, 1.0F);
    }

    // ------------------------------------------------------------------ capabilities

    public static void glEnable(int cap) {
        if (DisplayLists.record(() -> glEnable(cap))) return;
        mcc$track(cap, true);
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

    private static void mcc$track(int cap, boolean on) {
        switch (cap) {
            case 3553 -> org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.setTexture2d(on); // GL_TEXTURE_2D
            case 3042 -> org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.setBlend(on); // GL_BLEND
            case 2896 -> org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.setLighting(on); // GL_LIGHTING
            default -> {
            }
        }
    }

    public static void glDisable(int cap) {
        if (DisplayLists.record(() -> glDisable(cap))) return;
        mcc$track(cap, false);
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
        if (DisplayLists.record(() -> glBlendFunc(src, dst))) return;
        RenderSystem.blendFunc(src, dst);
        org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.setBlendFunc(src, dst);
    }

    public static void glDepthMask(boolean flag) {
        if (DisplayLists.record(() -> glDepthMask(flag))) return;
        RenderSystem.depthMask(flag);
    }

    public static void glAlphaFunc(int func, float ref) {
    }

    public static void glShadeModel(int mode) {
    }

    public static void glNormal3f(float x, float y, float z) {
        if (DisplayLists.record(() -> glNormal3f(x, y, z))) return;
        IMMEDIATE.normal(x, y, z);
    }

    public static void glLightModel(int pname, FloatBuffer params) {
    }

    public static void glPushAttrib(int mask) {
    }

    public static void glPopAttrib() {
    }

    public static void glCallList(int list) {
        if (DisplayLists.record(() -> glCallList(list))) return;
        DisplayLists.call(list);
    }

    public static int glGenLists(int range) {
        return DisplayLists.generate(range);
    }

    public static void glNewList(int list, int mode) {
        DisplayLists.begin(list, mode);
    }

    public static void glEndList() {
        DisplayLists.end();
    }

    public static void glDeleteLists(int list, int range) {
        DisplayLists.delete(list, range);
    }

    // ------------------------------------------------------------------ immediate mode

    private static final org.kylepitcock.legacyconnector.v1_7_10.client.LegacyVertices IMMEDIATE = new org.kylepitcock.legacyconnector.v1_7_10.client.LegacyVertices();

    public static void glBegin(int mode) {
        if (DisplayLists.record(() -> glBegin(mode))) return;
        IMMEDIATE.begin(mode);
    }

    public static void glEnd() {
        if (DisplayLists.record(() -> glEnd())) return;
        if (IMMEDIATE.isDrawing()) IMMEDIATE.end();
    }

    public static void glVertex3f(float x, float y, float z) {
        if (DisplayLists.record(() -> glVertex3f(x, y, z))) return;
        IMMEDIATE.vertex(x, y, z);
    }

    public static void glVertex3d(double x, double y, double z) {
        if (DisplayLists.record(() -> glVertex3d(x, y, z))) return;
        IMMEDIATE.vertex(x, y, z);
    }

    public static void glVertex2f(float x, float y) {
        if (DisplayLists.record(() -> glVertex2f(x, y))) return;
        IMMEDIATE.vertex(x, y, 0);
    }

    public static void glVertex2i(int x, int y) {
        if (DisplayLists.record(() -> glVertex2i(x, y))) return;
        IMMEDIATE.vertex(x, y, 0);
    }

    public static void glTexCoord2f(float u, float v) {
        if (DisplayLists.record(() -> glTexCoord2f(u, v))) return;
        IMMEDIATE.uv(u, v);
    }

    public static void glTexCoord2d(double u, double v) {
        if (DisplayLists.record(() -> glTexCoord2d(u, v))) return;
        IMMEDIATE.uv(u, v);
    }

    // ------------------------------------------------------------------ fixed-function vertex arrays

    public static void glEnableClientState(int array) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.enable(array, true);
    }

    public static void glDisableClientState(int array) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.enable(array, false);
    }

    public static void glVertexPointer(int size, int type, int stride, long offset) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.setPointer(0x8074, size, type, stride, offset);
    }

    public static void glNormalPointer(int type, int stride, long offset) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.setPointer(0x8075, 3, type, stride, offset);
    }

    public static void glTexCoordPointer(int size, int type, int stride, long offset) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.setPointer(0x8078, size, type, stride, offset);
    }

    public static void glColorPointer(int size, int type, int stride, long offset) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.setPointer(0x8076, size, type, stride, offset);
    }

    public static void glVertexPointer(int size, int stride, FloatBuffer data) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.setPointer(0x8074, size, 0x1406, stride, data);
    }

    public static void glNormalPointer(int stride, FloatBuffer data) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.setPointer(0x8075, 3, 0x1406, stride, data);
    }

    public static void glTexCoordPointer(int size, int stride, FloatBuffer data) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.setPointer(0x8078, size, 0x1406, stride, data);
    }

    public static void glDrawArrays(int mode, int first, int count) {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.draw(mode, first, count, new org.kylepitcock.legacyconnector.v1_7_10.client.ClientArrays.Emitter() {
            @Override
            public void begin(int m) {
                glBegin(m);
            }

            @Override
            public void texCoord(float u, float v) {
                glTexCoord2f(u, v);
            }

            @Override
            public void normal(float x, float y, float z) {
                glNormal3f(x, y, z);
            }

            @Override
            public void vertex(float x, float y, float z) {
                glVertex3f(x, y, z);
            }

            @Override
            public void end() {
                glEnd();
            }
        });
    }
}
