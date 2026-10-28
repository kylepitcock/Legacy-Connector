package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelPig;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityPig;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 pig renderer. */
public class RenderPig extends RenderLiving<EntityPig> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/pig/pig.png");

    public RenderPig(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelPig(), 0.7F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPig entity) {
        return TEXTURE;
    }
}
