package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.OpenGlHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Draws the 1.12.2 items whose model is built in ({@code IBakedModel.isBuiltInRenderer}), as 1.12's RenderItem did: through the item's
 * {@code TileEntityItemStackRenderer} (Item.setTileEntityItemStackRenderer), else through the tile entity renderer bound to the class
 * the mod gave {@code ForgeHooksClient.registerTESRItemStack}, drawing a fresh tile entity of that class at the origin.
 */
public final class LegacyItemStackRenderer1122 extends BlockEntityWithoutLevelRenderer {
    private final java.util.Map<Class<?>, TileEntity> tiles = new java.util.HashMap<>();

    public LegacyItemStackRenderer1122() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(net.minecraft.world.item.ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        ItemStack view = Stacks1122.fromModern(stack);
        if (view == null || view.isEmpty()) return;
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            Tessellator.mcc$reset();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >>> 16);
            var item = view.getItem();
            // 1.12 asked the item for the stack's metadata when it looked the model up, just before drawing it; mods use that call to hand
            // the stack to their renderer (Lycanites' parts keep it in a static there)
            item.getMetadata(view);
            TileEntityItemStackRenderer teisr = item.getTileEntityItemStackRenderer();
            if (teisr != null && teisr != TileEntityItemStackRenderer.instance) {
                teisr.renderByItem(view, 1.0F);
                return;
            }
            Class<?> cls = legacyconnector.v1_12_2.net.minecraftforge.client.ForgeHooksClient.mcc$tesrItem(item, view.getMetadata());
            if (cls == null) return;
            TileEntity te = tiles.get(cls);
            if (te == null) {
                var ctor = cls.getDeclaredConstructor();
                ctor.setAccessible(true);
                te = (TileEntity) ctor.newInstance();
                tiles.put(cls, te);
            }
            TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
            if (dispatcher.renderEngine == null) dispatcher.renderEngine = TextureManager.mcc$instance();
            dispatcher.render(te, 0.0D, 0.0D, 0.0D, 0.0F, -1, 1.0F);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            RenderErrors1122.report("drawing a built-in item model", e);
        } finally {
            Tessellator.mcc$reset();
            RenderContext.end();
        }
    }
}
