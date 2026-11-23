package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** 1.7.10's mooshroom renderer (also the base of mod cow variants); the mushrooms on its back are not drawn. */
public class RenderMooshroom extends RenderLiving {
    private static final ResourceLocation mooshroomTextures = new ResourceLocation("textures/entity/cow/mooshroom.png");

    public RenderMooshroom(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return mooshroomTextures;
    }
}
