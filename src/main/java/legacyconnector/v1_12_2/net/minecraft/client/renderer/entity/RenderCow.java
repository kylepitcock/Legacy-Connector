package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelCow;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityCow;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 cow renderer. */
public class RenderCow extends RenderLiving<EntityCow> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/cow/cow.png");

    public RenderCow(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelCow(), 0.7F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCow entity) {
        return TEXTURE;
    }
}
