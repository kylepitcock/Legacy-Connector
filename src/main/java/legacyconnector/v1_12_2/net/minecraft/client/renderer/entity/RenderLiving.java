package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.OpenGlHelper;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.culling.ICamera;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

/** 1.12.2 renderer of mobs: the living renderer plus the leash and the name of mobs with a custom name under the crosshair. */
public abstract class RenderLiving<T extends EntityLiving> extends RenderLivingBase<T> {
    public RenderLiving(RenderManager rendermanagerIn, ModelBase modelbaseIn, float shadowsizeIn) {
        super(rendermanagerIn, modelbaseIn, shadowsizeIn);
    }

    @Override
    protected boolean canRenderName(T entity) {
        return super.canRenderName(entity) && (entity.getAlwaysRenderNameTagForRender() || entity.hasCustomName() && entity == renderManager.pointedEntity);
    }

    @Override
    public boolean shouldRender(T livingEntity, ICamera camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntity, camera, camX, camY, camZ)) return true;
        if (livingEntity.getLeashed() && livingEntity.getLeashHolder() != null) {
            Entity holder = livingEntity.getLeashHolder();
            return camera == null || camera.isBoundingBoxInFrustum(holder.getRenderBoundingBox());
        }
        return false;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        if (!renderOutlines) renderLeash(entity, x, y, z, entityYaw, partialTicks);
    }

    public void setLightmap(T entityLivingIn) {
        int light = entityLivingIn.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (light % 65536), (float) (light / 65536));
    }

    private double interpolateValue(double start, double end, double pct) {
        return start + (end - start) * pct;
    }

    /** The sagging leash from the mob to its holder, as 1.12 drew it (24 segments of a two-sided strip). */
    protected void renderLeash(T entityLivingIn, double x, double y, double z, float entityYaw, float partialTicks) {
        Entity holder = entityLivingIn.getLeashHolder();
        if (holder == null) return;
        y = y - (1.6D - (double) Entity.mcc$get$height(entityLivingIn)) * 0.5D;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double holderYaw = this.interpolateValue(Entity.mcc$get$prevRotationYaw(holder), Entity.mcc$get$rotationYaw(holder), partialTicks * 0.5F) * 0.01745329238474369D;
        double holderPitch = this.interpolateValue(Entity.mcc$get$prevRotationPitch(holder), Entity.mcc$get$rotationPitch(holder), partialTicks * 0.5F) * 0.01745329238474369D;
        double cos = Math.cos(holderYaw);
        double sin = Math.sin(holderYaw);
        double pitchSin = Math.sin(holderPitch);
        if (holder.mcc$entity instanceof net.minecraft.world.entity.decoration.BlockAttachedEntity) {
            cos = 0.0D;
            sin = 0.0D;
            pitchSin = -1.0D;
        }
        double pitchCos = Math.cos(holderPitch);
        double hx = this.interpolateValue(Entity.mcc$get$prevPosX(holder), Entity.mcc$get$posX(holder), partialTicks) - cos * 0.7D - sin * 0.5D * pitchCos;
        double hy = this.interpolateValue(Entity.mcc$get$prevPosY(holder) + (double) holder.getEyeHeight() * 0.7D, Entity.mcc$get$posY(holder) + (double) holder.getEyeHeight() * 0.7D, partialTicks)
                - pitchSin * 0.5D - 0.25D;
        double hz = this.interpolateValue(Entity.mcc$get$prevPosZ(holder), Entity.mcc$get$posZ(holder), partialTicks) - sin * 0.7D + cos * 0.5D * pitchCos;
        double bodyYaw = this.interpolateValue(EntityLivingBase.mcc$get$prevRenderYawOffset(entityLivingIn), EntityLivingBase.mcc$get$renderYawOffset(entityLivingIn), partialTicks)
                * 0.01745329238474369D + (Math.PI / 2D);
        cos = Math.cos(bodyYaw) * (double) Entity.mcc$get$width(entityLivingIn) * 0.4D;
        sin = Math.sin(bodyYaw) * (double) Entity.mcc$get$width(entityLivingIn) * 0.4D;
        double ex = this.interpolateValue(Entity.mcc$get$prevPosX(entityLivingIn), Entity.mcc$get$posX(entityLivingIn), partialTicks) + cos;
        double ey = this.interpolateValue(Entity.mcc$get$prevPosY(entityLivingIn), Entity.mcc$get$posY(entityLivingIn), partialTicks);
        double ez = this.interpolateValue(Entity.mcc$get$prevPosZ(entityLivingIn), Entity.mcc$get$posZ(entityLivingIn), partialTicks) + sin;
        x = x + cos;
        z = z + sin;
        double dx = (double) ((float) (hx - ex));
        double dy = (double) ((float) (hy - ey));
        double dz = (double) ((float) (hz - ez));
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        buffer.begin(5, DefaultVertexFormats.POSITION_COLOR);
        for (int j = 0; j <= 24; ++j) {
            float r = 0.5F, g = 0.4F, b = 0.3F;
            if (j % 2 == 0) {
                r *= 0.7F;
                g *= 0.7F;
                b *= 0.7F;
            }
            float t = (float) j / 24.0F;
            buffer.pos(x + dx * (double) t + 0.0D, y + dy * (double) (t * t + t) * 0.5D + (double) ((24.0F - (float) j) / 18.0F + 0.125F), z + dz * (double) t).color(r, g, b, 1.0F).endVertex();
            buffer.pos(x + dx * (double) t + 0.025D, y + dy * (double) (t * t + t) * 0.5D + (double) ((24.0F - (float) j) / 18.0F + 0.125F) + 0.025D, z + dz * (double) t).color(r, g, b, 1.0F).endVertex();
        }
        tessellator.draw();
        buffer.begin(5, DefaultVertexFormats.POSITION_COLOR);
        for (int k = 0; k <= 24; ++k) {
            float r = 0.5F, g = 0.4F, b = 0.3F;
            if (k % 2 == 0) {
                r *= 0.7F;
                g *= 0.7F;
                b *= 0.7F;
            }
            float t = (float) k / 24.0F;
            buffer.pos(x + dx * (double) t + 0.0D, y + dy * (double) (t * t + t) * 0.5D + (double) ((24.0F - (float) k) / 18.0F + 0.125F) + 0.025D, z + dz * (double) t).color(r, g, b, 1.0F).endVertex();
            buffer.pos(x + dx * (double) t + 0.025D, y + dy * (double) (t * t + t) * 0.5D + (double) ((24.0F - (float) k) / 18.0F + 0.125F), z + dz * (double) t + 0.025D).color(r, g, b, 1.0F).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
    }
}
