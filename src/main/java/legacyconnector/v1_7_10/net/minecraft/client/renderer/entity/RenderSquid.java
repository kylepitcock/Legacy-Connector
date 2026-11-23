package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntitySquid;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** 1.7.10's squid renderer (also the base of mod sea creatures). */
public class RenderSquid extends RenderLiving {
    private static final ResourceLocation squidTextures = new ResourceLocation("textures/entity/squid.png");

    public RenderSquid(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    public void doRender(EntitySquid squid, double x, double y, double z, float yaw, float partialTicks) {
        super.doRender(squid, x, y, z, yaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntitySquid squid) {
        return squidTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntitySquid squid ? getEntityTexture(squid) : squidTextures;
    }
}
