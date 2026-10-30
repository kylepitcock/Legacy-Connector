package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItemFrame;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderItemInFrameEvent;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 item frame renderer. Vanilla item frames (modern entities) are drawn, frame, item and map, by the modern item frame renderer
 * at the legacy matrix. A mod's frame entity is drawn the 1.12 way: the vanilla frame model, the item ({@link #renderItem}, a map
 * through the modern map renderer) and the item's custom name ({@link #renderName}), which subclasses also call on their own.
 */
public class RenderItemFrame extends Render<EntityItemFrame> {
    private static final net.minecraft.client.resources.model.ModelResourceLocation FRAME_LOCATION =
            net.minecraft.client.resources.model.ModelResourceLocation.vanilla("item_frame", "map=false");
    private static final net.minecraft.client.resources.model.ModelResourceLocation MAP_FRAME_LOCATION =
            net.minecraft.client.resources.model.ModelResourceLocation.vanilla("item_frame", "map=true");
    protected final legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRenderer;

    public RenderItemFrame(RenderManager renderManager, legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRenderer) {
        super(renderManager);
        this.itemRenderer = itemRenderer != null ? itemRenderer : legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem.mcc$instance();
    }

    @Override
    public void doRender(EntityItemFrame entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null && !(entity.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122)) {
            LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
            return;
        }
        GlStateManager.pushMatrix();
        BlockPos pos = entity.getHangingPosition();
        double dx = pos.getX() - Entity.mcc$get$posX(entity) + x;
        double dy = pos.getY() - Entity.mcc$get$posY(entity) + y;
        double dz = pos.getZ() - Entity.mcc$get$posZ(entity) + z;
        GlStateManager.translate(dx + 0.5D, dy + 0.5D, dz + 0.5D);
        GlStateManager.rotate(180.0F - Entity.mcc$get$rotationYaw(entity), 0.0F, 1.0F, 0.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        mcc$renderFrame(mcc$isMap(entity.getDisplayedItem()));
        GlStateManager.popMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.4375F);
        renderItem(entity);
        GlStateManager.popMatrix();
        EnumFacing facing = EntityItemFrame.mcc$get$facingDirection(entity);
        if (facing != null) renderName(entity, x + facing.getXOffset() * 0.3F, y - 0.25D, z + facing.getZOffset() * 0.3F);
        else renderName(entity, x, y - 0.25D, z);
    }

    private static boolean mcc$isMap(ItemStack stack) {
        var modern = Stacks1122.toModern(stack);
        return modern != null && modern.getItem() instanceof net.minecraft.world.item.MapItem;
    }

    /** The vanilla frame's block model at the legacy matrix (the frame of a mod's item frame entity). */
    private static void mcc$renderFrame(boolean map) {
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        if (pose == null || buffers == null) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        var model = mc.getModelManager().getModel(map ? MAP_FRAME_LOCATION : FRAME_LOCATION);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(net.minecraft.client.renderer.Sheets.solidBlockSheet()), null,
                model, 1.0F, 1.0F, 1.0F, RenderContext.vertexLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    protected void renderItem(EntityItemFrame itemFrame) {
        ItemStack stack = itemFrame.getDisplayedItem();
        if (stack == null || stack.isEmpty()) return;
        var modern = Stacks1122.toModern(stack);
        boolean map = modern != null && modern.getItem() instanceof net.minecraft.world.item.MapItem;
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        int rotation = map ? itemFrame.getRotation() % 4 * 2 : itemFrame.getRotation();
        GlStateManager.rotate((float) rotation * 360.0F / 8.0F, 0.0F, 0.0F, 1.0F);
        if (!MinecraftForge.EVENT_BUS.post(new RenderItemInFrameEvent(itemFrame, this))) {
            if (map) {
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.scale(0.0078125F, 0.0078125F, 0.0078125F);
                GlStateManager.translate(-64.0F, -64.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.0F, -1.0F);
                mcc$renderMap(modern);
            } else {
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
                GlStateManager.pushAttrib();
                RenderHelper.enableStandardItemLighting();
                itemRenderer.renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.popAttrib();
            }
        }
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    /** A map's contents through the modern map renderer (a 128x128 square at the legacy matrix). */
    private static void mcc$renderMap(net.minecraft.world.item.ItemStack modern) {
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (pose == null || buffers == null || mc.level == null) return;
        var id = modern.get(net.minecraft.core.component.DataComponents.MAP_ID);
        var data = net.minecraft.world.item.MapItem.getSavedData(modern, mc.level);
        if (id == null || data == null) return;
        mc.gameRenderer.getMapRenderer().render(pose, buffers, id, data, true, RenderContext.vertexLight());
    }

    protected void renderName(EntityItemFrame entity, double x, double y, double z) {
        ItemStack stack = entity.getDisplayedItem();
        if (!Minecraft.isGuiEnabled() || stack == null || stack.isEmpty() || !stack.hasDisplayName() || renderManager.pointedEntity != entity) return;
        double distance = renderManager.renderViewEntity == null ? 0.0D : entity.getDistanceSq(renderManager.renderViewEntity);
        float range = entity.isSneaking() ? 32.0F : 64.0F;
        if (distance < (double) (range * range)) renderLivingLabel(entity, stack.getDisplayName(), x, y, z, 64);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityItemFrame entity) {
        return null;
    }
}
