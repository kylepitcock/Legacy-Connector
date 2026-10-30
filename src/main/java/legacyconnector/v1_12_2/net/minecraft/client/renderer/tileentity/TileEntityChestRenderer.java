package legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelChest;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelLargeChest;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityChest;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 chest renderer. A vanilla chest in the world is drawn by the modern chest renderer at the legacy offset; a mod's chest (or a
 * detached tile entity a mod draws, as chest mimics do) the 1.12 way: the chest model facing the block's metadata, its lid at the tile
 * entity's lid angle.
 */
public class TileEntityChestRenderer extends TileEntitySpecialRenderer<TileEntityChest> {
    private static final ResourceLocation TEXTURE_TRAPPED_DOUBLE = new ResourceLocation("textures/entity/chest/trapped_double.png");
    private static final ResourceLocation TEXTURE_CHRISTMAS_DOUBLE = new ResourceLocation("textures/entity/chest/christmas_double.png");
    private static final ResourceLocation TEXTURE_NORMAL_DOUBLE = new ResourceLocation("textures/entity/chest/normal_double.png");
    private static final ResourceLocation TEXTURE_TRAPPED = new ResourceLocation("textures/entity/chest/trapped.png");
    private static final ResourceLocation TEXTURE_CHRISTMAS = new ResourceLocation("textures/entity/chest/christmas.png");
    private static final ResourceLocation TEXTURE_NORMAL = new ResourceLocation("textures/entity/chest/normal.png");
    private final ModelChest simpleChest = new ModelChest();
    private final ModelChest largeChest = new ModelLargeChest();
    private final boolean isChristmas;

    public TileEntityChestRenderer() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        isChristmas = calendar.get(java.util.Calendar.MONTH) + 1 == 12 && calendar.get(java.util.Calendar.DAY_OF_MONTH) >= 24
                && calendar.get(java.util.Calendar.DAY_OF_MONTH) <= 26;
    }

    @Override
    public void render(TileEntityChest te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (mcc$drawModern(te, x, y, z, partialTicks)) return;
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        int meta = 0;
        if (te.hasWorld()) {
            try {
                meta = te.getBlockMetadata();
                te.checkForAdjacentChests();
            } catch (RuntimeException e) {
                meta = 0;
            }
        }
        if (te.adjacentChestZNeg != null || te.adjacentChestXNeg != null) return; // the other half draws the double chest
        boolean single = te.adjacentChestXPos == null && te.adjacentChestZPos == null;
        ModelChest model = single ? simpleChest : largeChest;
        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(single ? 4.0F : 8.0F, 4.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        } else if (isChristmas) {
            bindTexture(single ? TEXTURE_CHRISTMAS : TEXTURE_CHRISTMAS_DOUBLE);
        } else if (mcc$trapped(te)) {
            bindTexture(single ? TEXTURE_TRAPPED : TEXTURE_TRAPPED_DOUBLE);
        } else {
            bindTexture(single ? TEXTURE_NORMAL : TEXTURE_NORMAL_DOUBLE);
        }
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        if (destroyStage < 0) GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        GlStateManager.translate((float) x, (float) y + 1.0F, (float) z + 1.0F);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        int angle = switch (meta) {
            case 2 -> 180;
            case 4 -> 90;
            case 5 -> -90;
            default -> 0;
        };
        if (meta == 2 && te.adjacentChestXPos != null) GlStateManager.translate(1.0F, 0.0F, 0.0F);
        if (meta == 5 && te.adjacentChestZPos != null) GlStateManager.translate(0.0F, 0.0F, -1.0F);
        GlStateManager.rotate((float) angle, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        float lid = te.prevLidAngle + (te.lidAngle - te.prevLidAngle) * partialTicks;
        lid = 1.0F - lid;
        lid = 1.0F - lid * lid * lid;
        model.chestLid.rotateAngleX = -(lid * ((float) Math.PI / 2F));
        model.renderAll();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (destroyStage >= 0) {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }

    private static boolean mcc$trapped(TileEntityChest te) {
        try {
            var block = te.getBlockType();
            var modern = block == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.BlockBridge1122.modern(block);
            return modern == net.minecraft.world.level.block.Blocks.TRAPPED_CHEST;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** A vanilla chest block entity at the tile entity's position: drawn by its modern renderer at (x, y, z) of the legacy matrix. */
    private static boolean mcc$drawModern(TileEntityChest te, double x, double y, double z, float partialTicks) {
        if (te.getWorld() == null || te.getPos() == null || te.mcc$host instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity) return false;
        var level = te.getWorld().mcc$level();
        if (level == null) return false;
        var p = te.getPos();
        var be = level.getBlockEntity(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()));
        if (!(be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)) return false;
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        if (pose == null || buffers == null) return true;
        var renderer = net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        if (renderer == null) return false;
        pose.pushPose();
        try {
            pose.translate(x, y, z);
            renderer.render(be, partialTicks, pose, buffers, RenderContext.vertexLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing a chest", e);
        } finally {
            pose.popPose();
        }
        return true;
    }
}
