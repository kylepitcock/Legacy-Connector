package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** Cow renderer (also the base of mod cows). */
public class RenderCow extends RenderLiving {
    private static final ResourceLocation COW = new ResourceLocation("textures/entity/cow/cow.png");

    public RenderCow(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return COW;
    }
}
