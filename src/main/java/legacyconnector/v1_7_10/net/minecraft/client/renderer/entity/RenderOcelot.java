package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** 1.7.10's ocelot renderer (also the base of mod cats): the skin by tame type, tamed cats at 0.8 size. */
public class RenderOcelot extends RenderLiving {
    private static final ResourceLocation blackOcelotTextures = new ResourceLocation("textures/entity/cat/black.png");
    private static final ResourceLocation ocelotTextures = new ResourceLocation("textures/entity/cat/ocelot.png");
    private static final ResourceLocation redOcelotTextures = new ResourceLocation("textures/entity/cat/red.png");
    private static final ResourceLocation siameseOcelotTextures = new ResourceLocation("textures/entity/cat/siamese.png");

    public RenderOcelot(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    public void doRender(EntityOcelot ocelot, double x, double y, double z, float yaw, float partialTicks) {
        super.doRender((EntityLiving) ocelot, x, y, z, yaw, partialTicks);
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityOcelot ocelot) doRender(ocelot, x, y, z, yaw, partialTicks);
        else super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntityOcelot ocelot) {
        return switch (ocelot.getTameSkin()) {
            case 1 -> blackOcelotTextures;
            case 2 -> redOcelotTextures;
            case 3 -> siameseOcelotTextures;
            default -> ocelotTextures;
        };
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityOcelot ocelot ? getEntityTexture(ocelot) : ocelotTextures;
    }

    protected void preRenderCallback(EntityOcelot ocelot, float partialTicks) {
        super.preRenderCallback(ocelot, partialTicks);
        if (ocelot.isTamed()) GL11.glScalef(0.8F, 0.8F, 0.8F);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        if (entity instanceof EntityOcelot ocelot) preRenderCallback(ocelot, partialTicks);
        else super.preRenderCallback(entity, partialTicks);
    }
}
