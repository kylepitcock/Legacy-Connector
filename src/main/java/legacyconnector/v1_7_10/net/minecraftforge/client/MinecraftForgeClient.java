package legacyconnector.v1_7_10.net.minecraftforge.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Forge client hooks: custom item renderers (drawn through the item's modern model, see LegacyItemRenderer). */
public class MinecraftForgeClient {
    private static final Map<Item, IItemRenderer> RENDERERS = new ConcurrentHashMap<>();

    public static void registerItemRenderer(Item item, IItemRenderer renderer) {
        if (item != null && renderer != null) RENDERERS.put(item, renderer);
    }

    public static IItemRenderer getItemRenderer(ItemStack item, IItemRenderer.ItemRenderType type) {
        if (item == null || item.getItem() == null) return null;
        IItemRenderer r = RENDERERS.get(item.getItem());
        try {
            return r != null && r.handleRenderType(item, type) ? r : null;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    public static boolean mcc$hasRenderer(Item item) {
        return RENDERERS.containsKey(item);
    }

    public static IItemRenderer mcc$renderer(Item item) {
        return RENDERERS.get(item);
    }

    public static int getRenderPass() {
        // while a chunk is meshed, block renderers (Reika's pipes) ask for the pass of the chunk render, as 1.7.10's shared variable gave
        int pass = ForgeHooksClient.worldRenderPass >= 0 ? ForgeHooksClient.worldRenderPass : ForgeHooksClient.renderPass;
        return pass < 0 ? 0 : pass;
    }

    // Forge's private pool of free stencil bits (CoFH Core replaces it reflectively).
    private static java.util.BitSet stencilBits = new java.util.BitSet(8);

    static {
        stencilBits.set(0, 8);
    }

    public static int getStencilBits() {
        return 8;
    }

    public static int reserveStencilBit() {
        int bit = stencilBits.nextSetBit(0);
        if (bit >= 0) stencilBits.clear(bit);
        return bit;
    }

    public static void releaseStencilBit(int bit) {
        if (bit >= 0 && bit < 8) stencilBits.set(bit);
    }
}
