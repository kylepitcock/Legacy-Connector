package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.BlockRenderB173;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityRenderingB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockItem;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/**
 * Draws a block item of a mod's own render type (BuildCraft's pipes and engines) in the inventory, hand and on the ground by running
 * the mod's original {@code RenderInvBlock}, which draws around the origin as Beta's item renderer left it.
 */
public final class LegacyB173InventoryBlockRendererB173 extends BlockEntityWithoutLevelRenderer {
    private static final java.util.Set<Class<?>> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public LegacyB173InventoryBlockRendererB173() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof LegacyB173BlockItem item) || !(item.getBlock() instanceof LegacyB173Block hosted)) return;
        var block = hosted.legacy();
        var mod = BlockRenderB173.inventoryRenderer(block);
        if (mod == null || Minecraft.getInstance().level == null) return;
        int meta = LegacyB173BlockItem.meta(stack);
        var owner = ContentB173.ownerOf(block.getClass());
        RenderContext.beginWorld(pose, buffers, light, overlay);
        pose.pushPose();
        // The item pipeline hands over the block's corner; Beta's inventory drawing is around the block's centre.
        pose.translate(0.5, 0.5, 0.5);
        try (var readOnly = EntityViewsB173.readOnly()) {
            synchronized (mod) {
                try (var scope = RendererContextB173.open(path -> EntityRenderingB173.texture(owner, path), (part, texture) -> {
                    NativeRendererExtrasB173.applyStyle(RendererContextB173.style());
                    int color = RendererContextB173.argb();
                    RenderContext.color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
                    RenderContext.bindTexture(texture);
                    ModelPartsB173.draw(part);
                }, EntityRenderingB173.sheets(NativeRendererExtrasB173.create(Minecraft.getInstance().level, 0, pose, buffers)))) {
                    // Forge binds the block's own sheet before the render callback (ForgeHooksClient.renderInventoryBlock).
                    if (block instanceof legacyconnector.vb1_7_3.net.minecraft.src.forge.ITextureProvider provider) RendererContextB173.bind(provider.getTextureFile());
                    mod.RenderInvBlock(new legacyconnector.vb1_7_3.net.minecraft.client.render.block.BlockRenderManager(), block, meta, block.getRenderType());
                }
            }
        } catch (RuntimeException | LinkageError error) {
            if (REPORTED.add(block.getClass()))
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Original Beta inventory block renderer failed for {}", block.getClass().getName(), error);
        } finally {
            pose.popPose();
            RenderContext.end();
        }
    }
}
