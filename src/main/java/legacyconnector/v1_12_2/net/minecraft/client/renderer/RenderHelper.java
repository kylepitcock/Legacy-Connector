package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.nio.FloatBuffer;

/**
 * 1.12.2 RenderHelper: the fixed-function item lighting switches of the 1.7.10 shim (the modern shaders light items and entities, so
 * these keep the emulated lighting flag in step).
 */
public class RenderHelper extends legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderHelper {
    private static final FloatBuffer COLOR_BUFFER = GLAllocation.createDirectFloatBuffer(4);

    public static FloatBuffer setColorBuffer(double r, double g, double b, double a) {
        return setColorBuffer((float) r, (float) g, (float) b, (float) a);
    }

    public static FloatBuffer setColorBuffer(float r, float g, float b, float a) {
        COLOR_BUFFER.clear();
        COLOR_BUFFER.put(r).put(g).put(b).put(a);
        COLOR_BUFFER.flip();
        return COLOR_BUFFER;
    }
}
