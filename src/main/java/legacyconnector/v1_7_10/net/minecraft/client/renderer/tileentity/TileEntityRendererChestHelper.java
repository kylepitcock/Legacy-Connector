package legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity;

import legacyconnector.v1_7_10.net.minecraft.block.Block;

/**
 * Renders chest-style blocks in item form. Mods (like Iron Chest) replace {@link #instance} with a subclass that
 * renders their own tile entity; the compatibility layer calls it from the item renderer of render-type-22 blocks.
 */
public class TileEntityRendererChestHelper {
    public static TileEntityRendererChestHelper instance = new TileEntityRendererChestHelper();

    public void renderChest(Block block, int meta, float partialTicks) {
    }
}
