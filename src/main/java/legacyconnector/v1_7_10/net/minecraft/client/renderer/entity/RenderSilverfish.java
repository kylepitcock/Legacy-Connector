package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelSilverfish;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySilverfish;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** 1.7.10's silverfish renderer (also the base of mod silverfish): rolls over 180 degrees when it dies. */
public class RenderSilverfish extends RenderLiving {
    private static final ResourceLocation silverfishTextures = new ResourceLocation("textures/entity/silverfish.png");

    public RenderSilverfish() {
        super(new ModelSilverfish(), 0.3F);
    }

    protected float getDeathMaxRotation(EntitySilverfish silverfish) {
        return 180.0F;
    }

    @Override
    protected float getDeathMaxRotation(EntityLivingBase entity) {
        return 180.0F;
    }

    protected int shouldRenderPass(EntitySilverfish silverfish, int pass, float partialTicks) {
        return -1;
    }

    protected ResourceLocation getEntityTexture(EntitySilverfish silverfish) {
        return silverfishTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntitySilverfish s ? getEntityTexture(s) : silverfishTextures;
    }
}
