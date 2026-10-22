package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** Pig renderer (also the base of mod pigs). */
public class RenderPig extends RenderLiving {
    private static final ResourceLocation PIG = new ResourceLocation("textures/entity/pig/pig.png");

    public RenderPig(ModelBase model, ModelBase saddle, float shadowSize) {
        super(model, shadowSize);
        setRenderPassModel(saddle);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return PIG;
    }
}
