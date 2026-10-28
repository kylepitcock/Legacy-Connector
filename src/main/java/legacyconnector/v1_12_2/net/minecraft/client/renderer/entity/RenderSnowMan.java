package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntitySnowman;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 snow golem renderer. */
public class RenderSnowMan extends RenderLiving<EntitySnowman> {
    private static final ResourceLocation SNOW_MAN_TEXTURES = new ResourceLocation("textures/entity/snowman.png");

    public RenderSnowMan(RenderManager renderManagerIn) {
        super(renderManagerIn, new legacyconnector.v1_12_2.net.minecraft.client.model.ModelSnowMan(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySnowman entity) {
        return SNOW_MAN_TEXTURES;
    }
}
