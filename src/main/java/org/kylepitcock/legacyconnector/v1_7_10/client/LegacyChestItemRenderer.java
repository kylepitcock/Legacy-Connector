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
        if (TileEntityRendererChestHelper.instance.getClass() == TileEntityRendererChestHelper.class) {
            // no mod installed its own chest helper: 1.7.10's default helper drew a vanilla chest here (blocks that extend BlockChest or
            // BlockEnderChest and bring no renderer of their own, such as Witchery's refilling chest)
            renderVanillaChest(block, pose, buffers, light, overlay);
            return;
        }
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            TileEntityRendererChestHelper.instance.renderChest(block, view.getItemDamage(), 0.0F);
        } catch (RuntimeException | LinkageError e) {
            // keep the inventory usable if a legacy renderer breaks
        } finally {
            RenderContext.end();
        }
    }

    private static void renderVanillaChest(Block block, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var pos = net.minecraft.core.BlockPos.ZERO;
        net.minecraft.world.level.block.entity.BlockEntity be;
        if (block.mcc$vanilla != null && "ender_chest".equals(block.mcc$vanilla.legacyName())) {
            be = new net.minecraft.world.level.block.entity.EnderChestBlockEntity(pos, net.minecraft.world.level.block.Blocks.ENDER_CHEST.defaultBlockState());
        } else if (block.mcc$vanilla != null && "trapped_chest".equals(block.mcc$vanilla.legacyName())) {
            be = new net.minecraft.world.level.block.entity.TrappedChestBlockEntity(pos, net.minecraft.world.level.block.Blocks.TRAPPED_CHEST.defaultBlockState());
        } else {
            be = new net.minecraft.world.level.block.entity.ChestBlockEntity(pos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        }
        try {
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(be, pose, buffers, light, overlay);
        } catch (RuntimeException e) {
            // keep the inventory usable
        }
    }
}
