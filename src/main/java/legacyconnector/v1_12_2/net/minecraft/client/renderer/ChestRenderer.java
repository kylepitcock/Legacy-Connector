package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/**
 * 1.12.2's chest drawn as a block (block renderers and previews of chests): the block's item, built-in models included (the modern
 * renderer draws chests through their block entity renderer), in the block's unit cube turned a quarter.
 */
public class ChestRenderer {
    public ChestRenderer() {
    }

    public void renderChestBrightness(Block blockIn, float color) {
        GlStateManager.color(color, color, color, 1.0F);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.pushMatrix();
        // 1.12 called the item's built-in renderer on the unit cube; the modern renderer moves the model back by half a block itself
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        RenderItem.mcc$instance().renderItem(new ItemStack(blockIn), ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
    }
}
