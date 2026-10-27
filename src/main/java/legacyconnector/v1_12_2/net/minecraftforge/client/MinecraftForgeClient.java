package legacyconnector.v1_12_2.net.minecraftforge.client;

import java.util.Locale;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;

/** Forge's client render state: the block render layer being drawn (models give layer-specific quads) and the render pass. */
public class MinecraftForgeClient {
    static final ThreadLocal<BlockRenderLayer> RENDER_LAYER = new ThreadLocal<>();
    static final ThreadLocal<Integer> RENDER_PASS = ThreadLocal.withInitial(() -> -1);
    private static int stencilBits = 0xFF;

    public static BlockRenderLayer getRenderLayer() {
        return RENDER_LAYER.get();
    }

    public static int getRenderPass() {
        return RENDER_PASS.get();
    }

    public static Locale getLocale() {
        try {
            String code = net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected();
            String[] parts = code.split("_", 2);
            return parts.length == 2 ? new Locale(parts[0], parts[1].toUpperCase(Locale.ROOT)) : new Locale(code);
        } catch (RuntimeException | LinkageError e) {
            return Locale.getDefault();
        }
    }

    /** A free stencil bit (the main target has an 8-bit stencil buffer when one is enabled), or -1. */
    public static synchronized int reserveStencilBit() {
        for (int bit = 0; bit < 8; bit++) {
            if ((stencilBits & (1 << bit)) != 0) {
                stencilBits &= ~(1 << bit);
                return bit;
            }
        }
        return -1;
    }

    public static synchronized void releaseStencilBit(int bit) {
        if (bit >= 0 && bit < 8) stencilBits |= 1 << bit;
    }
}
