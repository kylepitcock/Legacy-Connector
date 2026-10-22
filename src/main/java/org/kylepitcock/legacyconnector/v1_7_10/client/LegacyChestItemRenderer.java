package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntityRendererChestHelper;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Item form of blocks using legacy render type 22 ("chest"): the legacy {@link TileEntityRendererChestHelper}
 * draws them, exactly as 1.7.10's item renderer did.
 */
public class LegacyChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    public LegacyChestItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(net.minecraft.world.item.ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        if (view == null || !(view.getItem() instanceof ItemBlock ib)) return;
        Block block = ib.mcc$block();
        Item item = view.getItem();
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            TileEntityRendererChestHelper.instance.renderChest(block, item.getMetadata(view.getItemDamage()), 0.0F);
        } catch (RuntimeException | LinkageError e) {
            // keep the inventory usable if a legacy renderer breaks
        } finally {
            RenderContext.end();
        }
    }
}
