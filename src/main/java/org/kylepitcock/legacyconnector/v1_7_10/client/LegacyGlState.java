package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.GlStateManager;

/**
 * The real GL state legacy code changed inside a render context, put back when the outermost context ends. Legacy
 * {@code glDisable(GL_DEPTH_TEST)}, {@code glDepthMask(false)}, {@code glDisable(GL_CULL_FACE)}, {@code glColorMask}, blending
 * and polygon offset go straight to {@code RenderSystem}; modern render types only set the parts they differ in from the
 * defaults, so a leftover depth mask or colour mask made later modern drawing (and Iris's passes) skip depth or colour
 * writes. Each part is saved the first time legacy code changes it, from GlStateManager's own cache (no GL query: a
 * {@code glGet} can stall a threaded driver, and a context is entered for every legacy tile entity and entity drawn).
 */
final class LegacyGlState {
    static final int DEPTH_TEST = 1, BLEND = 2, CULL = 4, DEPTH_MASK = 8, COLOR_MASK = 16, BLEND_FUNC = 32, POLYGON_OFFSET = 64,
            LOGIC_OP = 128, DEPTH_FUNC = 256, SCISSOR = 512, BLEND_EQUATION = 1024;

    private int saved;
    private boolean depthTest, blend, cull, depthMask, polygonOffset, logicOp, scissor, red, green, blue, alpha;
    private int depthFunc, srcRgb, dstRgb, srcAlpha, dstAlpha;

    /** Remembers the current value of a part before legacy code changes it (once per context). */
    void save(int part) {
        if ((saved & part) != 0) return;
        saved |= part;
        switch (part) {
            case DEPTH_TEST -> depthTest = GlStateManager.DEPTH.mode.enabled;
            case BLEND -> blend = GlStateManager.BLEND.mode.enabled;
            case CULL -> cull = GlStateManager.CULL.enable.enabled;
            case DEPTH_MASK -> depthMask = GlStateManager.DEPTH.mask;
            case COLOR_MASK -> {
                red = GlStateManager.COLOR_MASK.red;
                green = GlStateManager.COLOR_MASK.green;
                blue = GlStateManager.COLOR_MASK.blue;
                alpha = GlStateManager.COLOR_MASK.alpha;
            }
            case BLEND_FUNC -> {
                srcRgb = GlStateManager.BLEND.srcRgb;
                dstRgb = GlStateManager.BLEND.dstRgb;
                srcAlpha = GlStateManager.BLEND.srcAlpha;
                dstAlpha = GlStateManager.BLEND.dstAlpha;
            }
            case POLYGON_OFFSET -> polygonOffset = GlStateManager.POLY_OFFSET.fill.enabled;
            case LOGIC_OP -> logicOp = GlStateManager.COLOR_LOGIC.enable.enabled;
            case DEPTH_FUNC -> depthFunc = GlStateManager.DEPTH.func;
            case SCISSOR -> scissor = GlStateManager.SCISSOR.mode.enabled;
            case BLEND_EQUATION -> {
                // not cached by GlStateManager: modern code draws with GL_FUNC_ADD and only changes it around its own draws
            }
            default -> saved &= ~part;
        }
    }

    /** Puts every saved part back (render thread only) and forgets them. */
    void restore() {
        int s = saved;
        if (s == 0) return;
        saved = 0;
        if ((s & DEPTH_TEST) != 0) {
            if (depthTest) GlStateManager._enableDepthTest();
            else GlStateManager._disableDepthTest();
        }
        if ((s & BLEND) != 0) {
            if (blend) GlStateManager._enableBlend();
            else GlStateManager._disableBlend();
        }
        if ((s & CULL) != 0) {
            if (cull) GlStateManager._enableCull();
            else GlStateManager._disableCull();
        }
        if ((s & DEPTH_MASK) != 0) GlStateManager._depthMask(depthMask);
        if ((s & COLOR_MASK) != 0) GlStateManager._colorMask(red, green, blue, alpha);
        if ((s & BLEND_FUNC) != 0) GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        if ((s & POLYGON_OFFSET) != 0) {
            if (polygonOffset) GlStateManager._enablePolygonOffset();
            else GlStateManager._disablePolygonOffset();
        }
        if ((s & LOGIC_OP) != 0) {
            if (logicOp) GlStateManager._enableColorLogicOp();
            else GlStateManager._disableColorLogicOp();
        }
        if ((s & DEPTH_FUNC) != 0) GlStateManager._depthFunc(depthFunc);
        if ((s & SCISSOR) != 0) {
            if (scissor) GlStateManager._enableScissorTest();
            else GlStateManager._disableScissorTest();
        }
        if ((s & BLEND_EQUATION) != 0) GlStateManager._blendEquation(32774); // GL_FUNC_ADD
    }

    /** Forgets what was saved without restoring (a context left on another thread). */
    void clear() {
        saved = 0;
    }
}
