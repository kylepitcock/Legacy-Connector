package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.culling.ICamera;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;

/**
 * 1.12.2 entity renderer. Mod renderers are created by their {@code IRenderFactory} with the shim's {@link RenderManager} and run by
 * the modern renderer of the hosting entity type ({@code LegacyEntityRenderer1122}) inside a world render context: the matrix stack is
 * at the camera, so the legacy {@code translate(x, y, z)} lands on the entity. The modern renderer draws the shadow and the fire; name
 * tags are drawn here ({@link #renderName}), as 1.12 did.
 */
public abstract class Render<T extends Entity> {
    private static final ResourceLocation SHADOW_TEXTURES = new ResourceLocation("textures/misc/shadow.png");
    protected final RenderManager renderManager;
    protected float shadowSize;
    protected float shadowOpaque = 1.0F;
    protected boolean renderOutlines;

    protected Render(RenderManager renderManager) {
        this.renderManager = renderManager;
    }

    public void setRenderOutlines(boolean renderOutlinesIn) {
        this.renderOutlines = renderOutlinesIn;
    }

    public boolean shouldRender(T livingEntity, ICamera camera, double camX, double camY, double camZ) {
        AxisAlignedBB box = livingEntity.getRenderBoundingBox().grow(0.5D);
        if (box.hasNaN() || box.getAverageEdgeLength() == 0.0D) {
            double x = Entity.mcc$get$posX(livingEntity), y = Entity.mcc$get$posY(livingEntity), z = Entity.mcc$get$posZ(livingEntity);
            box = new AxisAlignedBB(x - 2.0D, y - 2.0D, z - 2.0D, x + 2.0D, y + 2.0D, z + 2.0D);
        }
        return livingEntity.isInRangeToRender3d(camX, camY, camZ) && (livingEntity.ignoreFrustumCheck || camera == null || camera.isBoundingBoxInFrustum(box));
    }

    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!renderOutlines) renderName(entity, x, y, z);
    }

    /** The team colour of the glowing outline (white without a team). */
    protected int getTeamColor(T entityIn) {
        return 16777215;
    }

    protected void renderName(T entity, double x, double y, double z) {
        if (canRenderName(entity)) renderLivingLabel(entity, entity.getDisplayName().getFormattedText(), x, y, z, 64);
    }

    protected boolean canRenderName(T entity) {
        return entity.getAlwaysRenderNameTagForRender() && entity.hasCustomName();
    }

    protected void renderEntityName(T entityIn, double x, double y, double z, String name, double distanceSq) {
        renderLivingLabel(entityIn, name, x, y, z, 64);
    }

    protected abstract ResourceLocation getEntityTexture(T entity);

    protected boolean bindEntityTexture(T entity) {
        ResourceLocation location = getEntityTexture(entity);
        if (location == null) return false;
        bindTexture(location);
        return true;
    }

    public void bindTexture(ResourceLocation location) {
        if (renderManager != null && renderManager.renderEngine != null) renderManager.renderEngine.bindTexture(location);
        else legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager.mcc$instance().bindTexture(location);
    }

    /** Draws a box untextured and white, offset by (x, y, z) (debug boxes, RenderEntity's placeholder). */
    public static void renderOffsetAABB(AxisAlignedBB boundingBox, double x, double y, double z) {
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        buffer.setTranslation(x, y, z);
        buffer.begin(7, DefaultVertexFormats.POSITION_NORMAL);
        buffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).normal(0.0F, 0.0F, -1.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).normal(0.0F, 0.0F, -1.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).normal(0.0F, 0.0F, -1.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).normal(0.0F, 0.0F, -1.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).normal(0.0F, 0.0F, 1.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).normal(0.0F, 0.0F, 1.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).normal(0.0F, 0.0F, 1.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).normal(0.0F, 0.0F, 1.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).normal(0.0F, -1.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).normal(0.0F, -1.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).normal(0.0F, -1.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).normal(0.0F, -1.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).normal(-1.0F, 0.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).normal(-1.0F, 0.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).normal(-1.0F, 0.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).normal(-1.0F, 0.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).normal(1.0F, 0.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).normal(1.0F, 0.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).normal(1.0F, 0.0F, 0.0F).endVertex();
        buffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).normal(1.0F, 0.0F, 0.0F).endVertex();
        tessellator.draw();
        buffer.setTranslation(0.0D, 0.0D, 0.0D);
        GlStateManager.enableTexture2D();
    }

    /** Shadow and fire are drawn by the modern entity renderer around the legacy one (the shadow takes {@link #shadowSize}). */
    public void doRenderShadowAndFire(Entity entityIn, double x, double y, double z, float yaw, float partialTicks) {
    }

    public FontRenderer getFontRendererFromRenderManager() {
        return renderManager != null ? renderManager.getFontRenderer() : FontRenderer.mcc$INSTANCE;
    }

    protected void renderLivingLabel(T entityIn, String str, double x, double y, double z, int maxDistance) {
        if (str == null || renderManager == null) return;
        Entity viewer = renderManager.renderViewEntity;
        double distance = viewer == null ? 0.0D : entityIn.getDistanceSq(viewer);
        if (distance > (double) (maxDistance * maxDistance)) return;
        boolean sneaking = entityIn.isSneaking();
        float yaw = renderManager.playerViewY;
        float pitch = renderManager.playerViewX;
        GameSettings options = renderManager.options;
        boolean frontal = options != null && GameSettings.mcc$get$thirdPersonView(options) == 2;
        float height = Entity.mcc$get$height(entityIn) + 0.5F - (sneaking ? 0.25F : 0.0F);
        int shift = "deadmau5".equals(str) ? -10 : 0;
        EntityRenderer.drawNameplate(getFontRendererFromRenderManager(), str, (float) x, (float) y + height, (float) z, shift, yaw, pitch, frontal, sneaking);
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }

    public boolean isMultipass() {
        return false;
    }

    public void renderMultipass(T entityIn, double x, double y, double z, float entityYaw, float partialTicks) {
    }

    /** The shadow radius the modern renderer gives the entity. */
    public float mcc$shadowSize() {
        return shadowSize;
    }
}
