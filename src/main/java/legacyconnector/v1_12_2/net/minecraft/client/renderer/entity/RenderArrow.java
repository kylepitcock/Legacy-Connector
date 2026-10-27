package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2 arrow renderer: the two crossed fletching planes and the back cap of the arrow texture, shaking after a hit. */
public abstract class RenderArrow<T extends EntityArrow> extends Render<T> {
    public RenderArrow(RenderManager renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        bindEntityTexture(entity);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(Entity.mcc$get$prevRotationYaw(entity) + (Entity.mcc$get$rotationYaw(entity) - Entity.mcc$get$prevRotationYaw(entity)) * partialTicks - 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Entity.mcc$get$prevRotationPitch(entity) + (Entity.mcc$get$rotationPitch(entity) - Entity.mcc$get$prevRotationPitch(entity)) * partialTicks, 0.0F, 0.0F, 1.0F);
        mcc$drawArrow((float) entity.arrowShake - partialTicks);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /** The arrow geometry at the current matrix, pointing along +X (also drawn by LayerArrow for arrows stuck in entities). */
    public static void mcc$drawArrow(float shake) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.enableRescaleNormal();
        if (shake > 0.0F) GlStateManager.rotate(-MathHelper.sin(shake * 3.0F) * shake, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(45.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.05625F, 0.05625F, 0.05625F);
        GlStateManager.translate(-4.0F, 0.0F, 0.0F);
        GlStateManager.glNormal3f(0.05625F, 0.0F, 0.0F);
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-7.0D, -2.0D, -2.0D).tex(0.0D, 0.15625D).endVertex();
        buffer.pos(-7.0D, -2.0D, 2.0D).tex(0.15625D, 0.15625D).endVertex();
        buffer.pos(-7.0D, 2.0D, 2.0D).tex(0.15625D, 0.3125D).endVertex();
        buffer.pos(-7.0D, 2.0D, -2.0D).tex(0.0D, 0.3125D).endVertex();
        tessellator.draw();
        GlStateManager.glNormal3f(-0.05625F, 0.0F, 0.0F);
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-7.0D, 2.0D, -2.0D).tex(0.0D, 0.15625D).endVertex();
        buffer.pos(-7.0D, 2.0D, 2.0D).tex(0.15625D, 0.15625D).endVertex();
        buffer.pos(-7.0D, -2.0D, 2.0D).tex(0.15625D, 0.3125D).endVertex();
        buffer.pos(-7.0D, -2.0D, -2.0D).tex(0.0D, 0.3125D).endVertex();
        tessellator.draw();
        for (int j = 0; j < 4; ++j) {
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.glNormal3f(0.0F, 0.0F, 0.05625F);
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(-8.0D, -2.0D, 0.0D).tex(0.0D, 0.0D).endVertex();
            buffer.pos(8.0D, -2.0D, 0.0D).tex(0.5D, 0.0D).endVertex();
            buffer.pos(8.0D, 2.0D, 0.0D).tex(0.5D, 0.15625D).endVertex();
            buffer.pos(-8.0D, 2.0D, 0.0D).tex(0.0D, 0.15625D).endVertex();
            tessellator.draw();
        }
        GlStateManager.disableRescaleNormal();
    }
}
