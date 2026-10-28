package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityGhast;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 ghast (4.5 times the model, open-eyed while shooting) renderer. */
public class RenderGhast extends RenderLiving<EntityGhast> {
    private static final ResourceLocation GHAST_TEXTURES = new ResourceLocation("textures/entity/ghast/ghast.png");
    private static final ResourceLocation GHAST_SHOOTING_TEXTURES = new ResourceLocation("textures/entity/ghast/ghast_shooting.png");

    public RenderGhast(RenderManager renderManagerIn) {
        super(renderManagerIn, new legacyconnector.v1_12_2.net.minecraft.client.model.ModelGhast(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityGhast entity) {
        return entity.isAttacking() ? GHAST_SHOOTING_TEXTURES : GHAST_TEXTURES;
    }

    @Override
    protected void preRenderCallback(EntityGhast entitylivingbaseIn, float partialTickTime) {
        GlStateManager.scale(4.5F, 4.5F, 4.5F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
