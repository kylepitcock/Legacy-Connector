package mccompat.v1_7_10.net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.kylepitcock.mccompat.v1_7_10.client.RenderContext;

/** 1.7.10 OpenGlHelper: texture units, separate blending, and the lightmap coordinates used as vertex light. */
public class OpenGlHelper {
    public static int defaultTexUnit = 33984;
    public static int lightmapTexUnit = 33985;
    public static float lastBrightnessX;
    public static float lastBrightnessY;
    public static boolean shadersSupported = true;
    public static boolean openGL21 = true;
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
}
