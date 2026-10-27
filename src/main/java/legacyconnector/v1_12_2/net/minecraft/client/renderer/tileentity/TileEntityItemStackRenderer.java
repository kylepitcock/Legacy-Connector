package legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Draws items whose model is "built in" (Item.setTileEntityItemStackRenderer); the base renderer draws nothing for mod items. */
public class TileEntityItemStackRenderer {
    public static TileEntityItemStackRenderer instance = new TileEntityItemStackRenderer();

    public void renderByItem(ItemStack itemStackIn) {
        renderByItem(itemStackIn, 1.0F);
    }

    public void renderByItem(ItemStack itemStackIn, float partialTicks) {
    }
}
