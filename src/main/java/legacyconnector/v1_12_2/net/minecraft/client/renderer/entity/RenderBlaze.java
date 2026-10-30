package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBlaze;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityBlaze;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 blaze renderer. */
public class RenderBlaze extends RenderLiving<EntityBlaze> {
    private static final ResourceLocation BLAZE_TEXTURES = new ResourceLocation("textures/entity/blaze.png");

    public RenderBlaze(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelBlaze(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBlaze entity) {
        return BLAZE_TEXTURES;
    }
}
