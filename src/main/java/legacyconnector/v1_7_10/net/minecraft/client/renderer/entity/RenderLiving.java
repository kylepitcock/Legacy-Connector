package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;

public abstract class RenderLiving extends RendererLivingEntity {
    public RenderLiving(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        super.doRender((EntityLivingBase) entity, x, y, z, yaw, partialTicks);
    }

    @Override
    public void doRender(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityLiving living) doRender(living, x, y, z, yaw, partialTicks);
        else super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        doRender((EntityLivingBase) entity, x, y, z, yaw, partialTicks);
    }
}
