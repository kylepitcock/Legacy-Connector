package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Fixed-function item lighting switches. The modern pipeline lights items and entities in its shaders, so these only keep
 * the emulated lighting flag in step for code that reads it.
 */
public class RenderHelper {
    public static void enableStandardItemLighting() {
        RenderContext.setLighting(true);
    }

    public static void disableStandardItemLighting() {
        RenderContext.setLighting(false);
    }

    public static void enableGUIStandardItemLighting() {
        RenderContext.setLighting(true);
    }
}
