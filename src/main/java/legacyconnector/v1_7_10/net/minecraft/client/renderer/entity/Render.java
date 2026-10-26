package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations;

public abstract class Render {
    protected RenderManager renderManager;
    protected RenderBlocks field_147909_c = new RenderBlocks();
    protected float shadowSize;
    protected float shadowOpaque = 1.0F;

    public abstract void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks);

    protected abstract ResourceLocation getEntityTexture(Entity entity);

    protected void bindEntityTexture(Entity entity) {
        bindTexture(getEntityTexture(entity));
    }

    protected void bindTexture(ResourceLocation location) {
        if (location != null) RenderContext.bindTexture(TextureLocations.modern(location.getResourceDomain(), location.getResourcePath()));
    }

    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
    }

    protected void renderLivingLabel(Entity entity, String label, double x, double y, double z, int maxDistance) {
    }

    public boolean isStaticEntity() {
        return false;
    }

    public float mcc$shadowSize() {
        return shadowSize;
    }

    public void setRenderManager(RenderManager manager) {
        this.renderManager = manager;
    }

    public FontRenderer getFontRendererFromRenderManager() {
        return FontRenderer.INSTANCE;
    }

    /** Draws a box untextured and white, offset by (x, y, z) (debug boxes, RenderEntity's placeholder). */
    public static void renderOffsetAABB(legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box, double x, double y, double z) {
        if (box == null) return;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator t = Tessellator.instance;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        t.startDrawingQuads();
        t.setTranslation(x, y, z);
        t.setNormal(0.0F, 0.0F, -1.0F);
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.minZ);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertex(box.minX, box.minY, box.maxZ);
        t.addVertex(box.maxX, box.minY, box.maxZ);
        t.addVertex(box.maxX, box.maxY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.maxZ);
        t.setNormal(0.0F, -1.0F, 0.0F);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.maxZ);
        t.addVertex(box.minX, box.minY, box.maxZ);
        t.setNormal(0.0F, 1.0F, 0.0F);
        t.addVertex(box.minX, box.maxY, box.maxZ);
        t.addVertex(box.maxX, box.maxY, box.maxZ);
        t.addVertex(box.maxX, box.maxY, box.minZ);
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.setNormal(-1.0F, 0.0F, 0.0F);
        t.addVertex(box.minX, box.minY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.setNormal(1.0F, 0.0F, 0.0F);
        t.addVertex(box.maxX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.maxZ);
        t.addVertex(box.maxX, box.minY, box.maxZ);
        t.setTranslation(0.0D, 0.0D, 0.0D);
        t.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** Draws a box's six faces with the current texture and colour. */
    public static void renderAABB(legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box) {
        if (box == null) return;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.minZ);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.addVertex(box.minX, box.minY, box.maxZ);
        t.addVertex(box.maxX, box.minY, box.maxZ);
        t.addVertex(box.maxX, box.maxY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.maxZ);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.maxZ);
        t.addVertex(box.minX, box.minY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.maxZ);
        t.addVertex(box.maxX, box.maxY, box.maxZ);
        t.addVertex(box.maxX, box.maxY, box.minZ);
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.addVertex(box.minX, box.minY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.maxZ);
        t.addVertex(box.minX, box.maxY, box.minZ);
        t.addVertex(box.minX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.minY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.minZ);
        t.addVertex(box.maxX, box.maxY, box.maxZ);
        t.addVertex(box.maxX, box.minY, box.maxZ);
        t.draw();
    }
}
