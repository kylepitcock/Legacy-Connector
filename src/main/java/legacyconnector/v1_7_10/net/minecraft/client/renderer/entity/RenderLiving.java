package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;

public class RenderLiving extends RendererLivingEntity {
    public RenderLiving(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    /** Minecraft 1.5.2's {@code RenderLiving} was concrete: it draws the entity's string texture ({@code EntityLiving.texture}). */
    @Override
    protected legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation getEntityTexture(Entity entity) {
        String path = entity instanceof EntityLivingBase living ? living.getTexture() : null;
        return org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152.location(path);
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
