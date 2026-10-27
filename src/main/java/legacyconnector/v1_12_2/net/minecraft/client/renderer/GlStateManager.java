package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import legacyconnector.v1_12_2.org.lwjgl.opengl.GL11;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Quaternion;

/**
 * 1.12.2 GlStateManager. Vanilla cached GL state in front of the fixed-function calls; here every call goes straight to the
 * shim's emulated GL11 (the 1.7.10 emulation on top of the render context), which already tracks what the modern pipeline needs:
 * the matrix stack, current colour, bound texture and the blend, depth, lighting and texture switches. No cache is kept, so code
 * that mixes GlStateManager with raw GL11 calls stays consistent.
 */
public class GlStateManager {
    private static final FloatBuffer BUF_FLOAT_16 = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer BUF_FLOAT_4 = GLAllocation.createDirectFloatBuffer(4);
    // The state vanilla cached. Kept up to date (mods read it, e.g. blendState.blend.currentState, to restore what they change), but
    // never used to skip a call.
    public static final AlphaState alphaState = new AlphaState();
    public static final BooleanState lightingState = new BooleanState(2896);
    public static final BooleanState[] lightState = new BooleanState[8];
    public static final ColorMaterialState colorMaterialState = new ColorMaterialState();
    public static final BlendState blendState = new BlendState();
    public static final DepthState depthState = new DepthState();
    public static final FogState fogState = new FogState();
    public static final CullState cullState = new CullState();
    public static final PolygonOffsetState polygonOffsetState = new PolygonOffsetState();
    public static final ColorLogicState colorLogicState = new ColorLogicState();
    public static final TexGenState texGenState = new TexGenState();
    public static final ClearState clearState = new ClearState();
    public static final StencilState stencilState = new StencilState();
    public static final BooleanState normalizeState = new BooleanState(2977);
    public static int activeTextureUnit;
    public static final TextureState[] textureState = new TextureState[8];
    public static int activeShadeModel = 7425;
    public static final BooleanState rescaleNormalState = new BooleanState(32826);
    public static final ColorMask colorMaskState = new ColorMask();
    public static final Color colorState = new Color();

    static {
        for (int i = 0; i < 8; ++i) lightState[i] = new BooleanState(16384 + i);
        for (int i = 0; i < 8; ++i) textureState[i] = new TextureState(i == 0);
    }

    public static void pushAttrib() {
        GL11.glPushAttrib(8256);
    }

    public static void popAttrib() {
        GL11.glPopAttrib();
    }

    public static void disableAlpha() {
        alphaState.alphaTest.setDisabled();
    }

    public static void enableAlpha() {
        alphaState.alphaTest.setEnabled();
    }

    public static void alphaFunc(int func, float ref) {
        alphaState.func = func;
        alphaState.ref = ref;
        GL11.glAlphaFunc(func, ref);
    }

    public static void enableLighting() {
        lightingState.setEnabled();
    }

    public static void disableLighting() {
        lightingState.setDisabled();
    }

    public static void enableLight(int light) {
        lightState[light & 7].setEnabled();
    }

    public static void disableLight(int light) {
        lightState[light & 7].setDisabled();
    }

    public static void enableColorMaterial() {
        colorMaterialState.colorMaterial.setEnabled();
    }

    public static void disableColorMaterial() {
        colorMaterialState.colorMaterial.setDisabled();
    }

    public static void colorMaterial(int face, int mode) {
        colorMaterialState.face = face;
        colorMaterialState.mode = mode;
        GL11.glColorMaterial(face, mode);
    }

    public static void glLight(int light, int pname, FloatBuffer params) {
        GL11.glLight(light, pname, params);
    }

    public static void glLightModel(int pname, FloatBuffer params) {
        GL11.glLightModel(pname, params);
    }

    public static void glNormal3f(float nx, float ny, float nz) {
        GL11.glNormal3f(nx, ny, nz);
    }

    public static void disableDepth() {
        depthState.depthTest.setDisabled();
    }

    public static void enableDepth() {
        depthState.depthTest.setEnabled();
    }

    public static void depthFunc(int depthFunc) {
        depthState.depthFunc = depthFunc;
        if (RenderSystem.isOnRenderThread()) RenderSystem.depthFunc(depthFunc);
    }

    public static void depthMask(boolean flagIn) {
        depthState.maskEnabled = flagIn;
        GL11.glDepthMask(flagIn);
    }

    public static void disableBlend() {
        blendState.blend.setDisabled();
    }

    public static void enableBlend() {
        blendState.blend.setEnabled();
    }

    public static void blendFunc(SourceFactor srcFactor, DestFactor dstFactor) {
        blendFunc(srcFactor.factor, dstFactor.factor);
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        blendState.srcFactor = srcFactor;
        blendState.dstFactor = dstFactor;
        GL11.glBlendFunc(srcFactor, dstFactor);
    }

    public static void tryBlendFuncSeparate(SourceFactor srcFactor, DestFactor dstFactor, SourceFactor srcFactorAlpha, DestFactor dstFactorAlpha) {
        tryBlendFuncSeparate(srcFactor.factor, dstFactor.factor, srcFactorAlpha.factor, dstFactorAlpha.factor);
    }

    public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
        blendState.srcFactor = srcFactor;
        blendState.dstFactor = dstFactor;
        blendState.srcFactorAlpha = srcFactorAlpha;
        blendState.dstFactorAlpha = dstFactorAlpha;
        if (!RenderSystem.isOnRenderThread()) return;
        OpenGlHelper.glBlendFunc(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
    }

    public static void glBlendEquation(int blendEquation) {
        if (RenderSystem.isOnRenderThread()) RenderSystem.blendEquation(blendEquation);
    }

    /** Outline (glowing entity) pass colour: the modern outline pass colours entities itself. */
    public static void enableOutlineMode(int color) {
    }

    public static void disableOutlineMode() {
    }

    public static void enableFog() {
        fogState.fog.setEnabled();
    }

    public static void disableFog() {
        fogState.fog.setDisabled();
    }

    public static void setFog(FogMode fogMode) {
        setFog(fogMode.capabilityId);
    }

    private static void setFog(int param) {
        fogState.mode = param;
        GL11.glFogi(GL11.GL_FOG_MODE, param);
    }

    public static void setFogDensity(float param) {
        fogState.density = param;
        GL11.glFogf(GL11.GL_FOG_DENSITY, param);
    }

    public static void setFogStart(float param) {
        fogState.start = param;
        GL11.glFogf(GL11.GL_FOG_START, param);
    }

    public static void setFogEnd(float param) {
        fogState.end = param;
        GL11.glFogf(GL11.GL_FOG_END, param);
    }

    public static void glFog(int pname, FloatBuffer param) {
        GL11.glFog(pname, param);
    }

    public static void glFogi(int pname, int param) {
        GL11.glFogi(pname, param);
    }

    public static void enableCull() {
        cullState.cullFace.setEnabled();
    }

    public static void disableCull() {
        cullState.cullFace.setDisabled();
    }

    public static void cullFace(CullFace cullFace) {
        cullFace(cullFace.mode);
    }

    private static void cullFace(int mode) {
        cullState.mode = mode;
        GL11.glCullFace(mode);
    }

    public static void glPolygonMode(int face, int mode) {
        GL11.glPolygonMode(face, mode);
    }

    public static void enablePolygonOffset() {
        polygonOffsetState.polygonOffsetFill.setEnabled();
    }

    public static void disablePolygonOffset() {
        polygonOffsetState.polygonOffsetFill.setDisabled();
    }

    public static void doPolygonOffset(float factor, float units) {
        polygonOffsetState.factor = factor;
        polygonOffsetState.units = units;
        if (RenderSystem.isOnRenderThread()) RenderSystem.polygonOffset(factor, units);
    }

    public static void enableColorLogic() {
        colorLogicState.colorLogicOp.setEnabled();
    }

    public static void disableColorLogic() {
        colorLogicState.colorLogicOp.setDisabled();
    }

    public static void colorLogicOp(LogicOp logicOperation) {
        colorLogicOp(logicOperation.opcode);
    }

    public static void colorLogicOp(int opcode) {
        colorLogicState.opcode = opcode;
        GL11.glLogicOp(opcode);
    }

    /** Texture coordinate generation is fixed-function only (the End portal's old effect); the modern shaders ignore it. */
    public static void enableTexGenCoord(TexGen texGen) {
    }

    public static void disableTexGenCoord(TexGen texGen) {
    }

    public static void texGen(TexGen texGen, int param) {
    }

    public static void texGen(TexGen texGen, int pname, FloatBuffer params) {
    }

    public static void setActiveTexture(int texture) {
        activeTextureUnit = Math.max(0, Math.min(7, texture - OpenGlHelper.defaultTexUnit));
        OpenGlHelper.setActiveTexture(texture);
    }

    /**
     * The emulation tracks texturing for the default unit only: switching texturing on the lightmap unit (code that brackets a
     * draw with lightmap off/on) must not turn the main texture off.
     */
    public static void enableTexture2D() {
        textureState[activeTextureUnit].texture2DState.setEnabled();
    }

    public static void disableTexture2D() {
        textureState[activeTextureUnit].texture2DState.setDisabled();
    }

    public static void glTexEnv(int target, int parameterName, FloatBuffer parameters) {
        GL11.glTexEnv(target, parameterName, parameters);
    }

    public static void glTexEnvi(int target, int parameterName, int parameter) {
        GL11.glTexEnvi(target, parameterName, parameter);
    }

    public static void glTexEnvf(int target, int parameterName, float parameter) {
        GL11.glTexEnvf(target, parameterName, parameter);
    }

    public static void glTexParameterf(int target, int parameterName, float parameter) {
        GL11.glTexParameterf(target, parameterName, parameter);
    }

    public static void glTexParameteri(int target, int parameterName, int parameter) {
        GL11.glTexParameteri(target, parameterName, parameter);
    }

    public static int glGetTexLevelParameteri(int target, int level, int parameterName) {
        return GL11.glGetTexLevelParameteri(target, level, parameterName);
    }

    public static int generateTexture() {
        return GL11.glGenTextures();
    }

    public static void deleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
    }

    public static void bindTexture(int texture) {
        textureState[activeTextureUnit].textureName = texture;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels) {
        GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }

    public static void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, IntBuffer pixels) {
        GL11.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, pixels);
    }

    public static void glCopyTexSubImage2D(int target, int level, int xOffset, int yOffset, int x, int y, int width, int height) {
        GL11.glCopyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height);
    }

    public static void glGetTexImage(int tex, int level, int format, int type, IntBuffer pixels) {
        GL11.glGetTexImage(tex, level, format, type, pixels);
    }

    public static void enableNormalize() {
        normalizeState.setEnabled();
    }

    public static void disableNormalize() {
        normalizeState.setDisabled();
    }

    public static void shadeModel(int mode) {
        activeShadeModel = mode;
        GL11.glShadeModel(mode);
    }

    public static void enableRescaleNormal() {
        rescaleNormalState.setEnabled();
    }

    public static void disableRescaleNormal() {
        rescaleNormalState.setDisabled();
    }

    public static void viewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        colorMaskState.red = red;
        colorMaskState.green = green;
        colorMaskState.blue = blue;
        colorMaskState.alpha = alpha;
        if (RenderSystem.isOnRenderThread()) RenderSystem.colorMask(red, green, blue, alpha);
    }

    public static void clearDepth(double depth) {
        clearState.depth = depth;
        GL11.glClearDepth(depth);
    }

    public static void clearColor(float red, float green, float blue, float alpha) {
        clearState.color.red = red;
        clearState.color.green = green;
        clearState.color.blue = blue;
        clearState.color.alpha = alpha;
        GL11.glClearColor(red, green, blue, alpha);
    }

    public static void clear(int mask) {
        GL11.glClear(mask);
    }

    public static void matrixMode(int mode) {
        GL11.glMatrixMode(mode);
    }

    public static void loadIdentity() {
        GL11.glLoadIdentity();
    }

    public static void pushMatrix() {
        GL11.glPushMatrix();
    }

    public static void popMatrix() {
        GL11.glPopMatrix();
    }

    public static void getFloat(int pname, FloatBuffer params) {
        GL11.glGetFloat(pname, params);
    }

    public static void ortho(double left, double right, double bottom, double top, double zNear, double zFar) {
        GL11.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    public static void rotate(float angle, float x, float y, float z) {
        GL11.glRotatef(angle, x, y, z);
    }

    public static void scale(float x, float y, float z) {
        GL11.glScalef(x, y, z);
    }

    public static void scale(double x, double y, double z) {
        GL11.glScaled(x, y, z);
    }

    public static void translate(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    public static void translate(double x, double y, double z) {
        GL11.glTranslated(x, y, z);
    }

    public static void multMatrix(FloatBuffer matrix) {
        GL11.glMultMatrix(matrix);
    }

    public static void rotate(Quaternion quaternionIn) {
        multMatrix(quatToGlMatrix(BUF_FLOAT_16, quaternionIn));
    }

    public static FloatBuffer quatToGlMatrix(FloatBuffer buffer, Quaternion quaternionIn) {
        buffer.clear();
        float x = quaternionIn.x, y = quaternionIn.y, z = quaternionIn.z, w = quaternionIn.w;
        float f = x * x, f1 = x * y, f2 = x * z, f3 = x * w, f4 = y * y, f5 = y * z, f6 = y * w, f7 = z * z, f8 = z * w;
        buffer.put(1.0F - 2.0F * (f4 + f7));
        buffer.put(2.0F * (f1 + f8));
        buffer.put(2.0F * (f2 - f6));
        buffer.put(0.0F);
        buffer.put(2.0F * (f1 - f8));
        buffer.put(1.0F - 2.0F * (f + f7));
        buffer.put(2.0F * (f5 + f3));
        buffer.put(0.0F);
        buffer.put(2.0F * (f2 + f6));
        buffer.put(2.0F * (f5 - f3));
        buffer.put(1.0F - 2.0F * (f + f4));
        buffer.put(0.0F);
        buffer.put(0.0F);
        buffer.put(0.0F);
        buffer.put(0.0F);
        buffer.put(1.0F);
        buffer.rewind();
        return buffer;
    }

    public static void color(float colorRed, float colorGreen, float colorBlue, float colorAlpha) {
        colorState.red = colorRed;
        colorState.green = colorGreen;
        colorState.blue = colorBlue;
        colorState.alpha = colorAlpha;
        GL11.glColor4f(colorRed, colorGreen, colorBlue, colorAlpha);
    }

    public static void color(float colorRed, float colorGreen, float colorBlue) {
        color(colorRed, colorGreen, colorBlue, 1.0F);
    }

    /** The last colour set, {r, g, b, a}. */
    public static float[] mcc$color() {
        return new float[] {colorState.red, colorState.green, colorState.blue, colorState.alpha};
    }

    public static void glTexCoord2f(float sCoord, float tCoord) {
        GL11.glTexCoord2f(sCoord, tCoord);
    }

    public static void glVertex3f(float x, float y, float z) {
        GL11.glVertex3f(x, y, z);
    }

    /** Vanilla only invalidated its colour cache here; the GL colour stays what it was. */
    public static void resetColor() {
    }

    public static void glNormalPointer(int type, int stride, ByteBuffer buffer) {
        GL11.glNormalPointer(type, stride, buffer);
    }

    public static void glTexCoordPointer(int size, int type, int stride, int bufferOffset) {
        GL11.glTexCoordPointer(size, type, stride, (long) bufferOffset);
    }

    public static void glTexCoordPointer(int size, int type, int stride, ByteBuffer buffer) {
        GL11.glTexCoordPointer(size, type, stride, buffer);
    }

    public static void glVertexPointer(int size, int type, int stride, int bufferOffset) {
        GL11.glVertexPointer(size, type, stride, (long) bufferOffset);
    }

    public static void glVertexPointer(int size, int type, int stride, ByteBuffer buffer) {
        GL11.glVertexPointer(size, type, stride, buffer);
    }

    public static void glColorPointer(int size, int type, int stride, int bufferOffset) {
        GL11.glColorPointer(size, type, stride, (long) bufferOffset);
    }

    public static void glColorPointer(int size, int type, int stride, ByteBuffer buffer) {
        GL11.glColorPointer(size, type, stride, buffer);
    }

    public static void glDisableClientState(int cap) {
        GL11.glDisableClientState(cap);
    }

    public static void glEnableClientState(int cap) {
        GL11.glEnableClientState(cap);
    }

    public static void glBegin(int mode) {
        GL11.glBegin(mode);
    }

    public static void glEnd() {
        GL11.glEnd();
    }

    public static void glDrawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
    }

    /** Through the modern state (a core profile rejects widths above 1 when set directly). */
    public static void glLineWidth(float width) {
        if (RenderSystem.isOnRenderThread()) RenderSystem.lineWidth(width);
    }

    public static void callList(int list) {
        GL11.glCallList(list);
    }

    public static void glDeleteLists(int list, int range) {
        GL11.glDeleteLists(list, range);
    }

    public static void glNewList(int list, int mode) {
        GL11.glNewList(list, mode);
    }

    public static void glEndList() {
        GL11.glEndList();
    }

    public static int glGenLists(int range) {
        return GL11.glGenLists(range);
    }

    public static void glPixelStorei(int parameterName, int param) {
        GL11.glPixelStorei(parameterName, param);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type, IntBuffer pixels) {
        GL11.glReadPixels(x, y, width, height, format, type, pixels);
    }

    public static int glGetError() {
        return GL11.glGetError();
    }

    public static String glGetString(int name) {
        return GL11.glGetString(name);
    }

    public static void glGetInteger(int parameterName, IntBuffer parameters) {
        GL11.glGetInteger(parameterName, parameters);
    }

    public static int glGetInteger(int parameterName) {
        return GL11.glGetInteger(parameterName);
    }

    public static void enableBlendProfile(Profile profile) {
        profile.apply();
    }

    public static void disableBlendProfile(Profile profile) {
        profile.clean();
    }

    /** Forge: the current colour as a float buffer (some mods save it around their own colour changes). */
    public static FloatBuffer mcc$colorBuffer() {
        BUF_FLOAT_4.clear();
        BUF_FLOAT_4.put(colorState.red).put(colorState.green).put(colorState.blue).put(colorState.alpha).flip();
        return BUF_FLOAT_4;
    }

    /** A GL capability switch; changing it changes the emulated GL state (vanilla's semantics). */
    public static class BooleanState {
        public final int capability;
        public boolean currentState;
        /** Texture units other than the default one: texturing there must not switch off the emulated main texture. */
        private final boolean applies;

        public BooleanState(int capabilityIn) {
            this(capabilityIn, true);
        }

        BooleanState(int capabilityIn, boolean applies) {
            capability = capabilityIn;
            this.applies = applies;
        }

        public void setDisabled() {
            setState(false);
        }

        public void setEnabled() {
            setState(true);
        }

        public void setState(boolean state) {
            currentState = state;
            if (!applies) return;
            if (state) GL11.glEnable(capability);
            else GL11.glDisable(capability);
        }
    }

    public static class AlphaState {
        public BooleanState alphaTest = new BooleanState(3008);
        public int func = 519;
        public float ref = -1.0F;
    }

    public static class BlendState {
        public BooleanState blend = new BooleanState(3042);
        public int srcFactor = 1;
        public int dstFactor = 0;
        public int srcFactorAlpha = 1;
        public int dstFactorAlpha = 0;
    }

    public static class ClearState {
        public double depth = 1.0D;
        public Color color = new Color(0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static class Color {
        public float red;
        public float green;
        public float blue;
        public float alpha;

        public Color() {
            this(1.0F, 1.0F, 1.0F, 1.0F);
        }

        public Color(float redIn, float greenIn, float blueIn, float alphaIn) {
            red = redIn;
            green = greenIn;
            blue = blueIn;
            alpha = alphaIn;
        }
    }

    public static class ColorLogicState {
        public BooleanState colorLogicOp = new BooleanState(3058);
        public int opcode = 5379;
    }

    public static class ColorMask {
        public boolean red = true;
        public boolean green = true;
        public boolean blue = true;
        public boolean alpha = true;
    }

    public static class ColorMaterialState {
        public BooleanState colorMaterial = new BooleanState(2903);
        public int face = 1032;
        public int mode = 5634;
    }

    public static class CullState {
        public BooleanState cullFace = new BooleanState(2884);
        public int mode = 1029;
    }

    public static class DepthState {
        public BooleanState depthTest = new BooleanState(2929);
        public boolean maskEnabled = true;
        public int depthFunc = 513;
    }

    public static class FogState {
        public BooleanState fog = new BooleanState(2912);
        public int mode = 2048;
        public float density = 1.0F;
        public float start;
        public float end = 1.0F;
    }

    public static class PolygonOffsetState {
        public BooleanState polygonOffsetFill = new BooleanState(32823);
        public BooleanState polygonOffsetLine = new BooleanState(10754);
        public float factor;
        public float units;
    }

    public static class StencilFunc {
        public int func = 519;
        public int mask = -1;
    }

    public static class StencilState {
        public StencilFunc func = new StencilFunc();
        public int mask = -1;
        public int fail = 7680;
        public int zfail = 7680;
        public int zpass = 7680;
    }

    public static class TexGenCoord {
        public BooleanState textureGen;
        public int coord;
        public int param = -1;

        public TexGenCoord(int coordIn, int capabilityIn) {
            coord = coordIn;
            // fixed-function only: tracked, never applied
            textureGen = new BooleanState(capabilityIn, false);
        }
    }

    public static class TexGenState {
        public TexGenCoord s = new TexGenCoord(8192, 3168);
        public TexGenCoord t = new TexGenCoord(8193, 3169);
        public TexGenCoord r = new TexGenCoord(8194, 3170);
        public TexGenCoord q = new TexGenCoord(8195, 3171);
    }

    public static class TextureState {
        public BooleanState texture2DState;
        public int textureName;

        public TextureState() {
            this(true);
        }

        TextureState(boolean defaultUnit) {
            texture2DState = new BooleanState(3553, defaultUnit);
        }
    }

    public enum CullFace {
        FRONT(1028),
        BACK(1029),
        FRONT_AND_BACK(1032);

        public final int mode;

        CullFace(int modeIn) {
            mode = modeIn;
        }
    }

    public enum DestFactor {
        CONSTANT_ALPHA(32771),
        CONSTANT_COLOR(32769),
        DST_ALPHA(772),
        DST_COLOR(774),
        ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(32772),
        ONE_MINUS_CONSTANT_COLOR(32770),
        ONE_MINUS_DST_ALPHA(773),
        ONE_MINUS_DST_COLOR(775),
        ONE_MINUS_SRC_ALPHA(771),
        ONE_MINUS_SRC_COLOR(769),
        SRC_ALPHA(770),
        SRC_COLOR(768),
        ZERO(0);

        public final int factor;

        DestFactor(int factorIn) {
            factor = factorIn;
        }
    }

    public enum SourceFactor {
        CONSTANT_ALPHA(32771),
        CONSTANT_COLOR(32769),
        DST_ALPHA(772),
        DST_COLOR(774),
        ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(32772),
        ONE_MINUS_CONSTANT_COLOR(32770),
        ONE_MINUS_DST_ALPHA(773),
        ONE_MINUS_DST_COLOR(775),
        ONE_MINUS_SRC_ALPHA(771),
        ONE_MINUS_SRC_COLOR(769),
        SRC_ALPHA(770),
        SRC_ALPHA_SATURATE(776),
        SRC_COLOR(768),
        ZERO(0);

        public final int factor;

        SourceFactor(int factorIn) {
            factor = factorIn;
        }
    }

    public enum FogMode {
        LINEAR(9729),
        EXP(2048),
        EXP2(2049);

        public final int capabilityId;

        FogMode(int capabilityIn) {
            capabilityId = capabilityIn;
        }
    }

    public enum LogicOp {
        AND(5377),
        AND_INVERTED(5380),
        AND_REVERSE(5378),
        CLEAR(5376),
        COPY(5379),
        COPY_INVERTED(5388),
        EQUIV(5385),
        INVERT(5386),
        NAND(5390),
        NOOP(5381),
        NOR(5384),
        OR(5383),
        OR_INVERTED(5389),
        OR_REVERSE(5387),
        SET(5391),
        XOR(5382);

        public final int opcode;

        LogicOp(int opcodeIn) {
            opcode = opcodeIn;
        }
    }

    public enum TexGen {
        S,
        T,
        R,
        Q
    }

    public enum Profile {
        DEFAULT {
            @Override
            public void apply() {
                disableAlpha();
                alphaFunc(519, 0.0F);
                disableLighting();
                glLightModel(2899, RenderHelper.setColorBuffer(0.2F, 0.2F, 0.2F, 1.0F));
                for (int i = 0; i < 8; ++i) disableLight(i);
                disableColorMaterial();
                enableBlend();
                tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
                enableDepth();
                depthFunc(513);
                depthMask(true);
                disableCull();
                enableTexture2D();
                resetColor();
                color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            @Override
            public void clean() {
            }
        },
        PLAYER_SKIN {
            @Override
            public void apply() {
                enableBlend();
                tryBlendFuncSeparate(770, 771, 1, 0);
            }

            @Override
            public void clean() {
                disableBlend();
            }
        },
        TRANSPARENT_MODEL {
            @Override
            public void apply() {
                color(1.0F, 1.0F, 1.0F, 0.15F);
                depthMask(false);
                enableBlend();
                blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                alphaFunc(516, 0.003921569F);
            }

            @Override
            public void clean() {
                disableBlend();
                alphaFunc(516, 0.1F);
                depthMask(true);
            }
        };

        public abstract void apply();

        public abstract void clean();
    }
}
