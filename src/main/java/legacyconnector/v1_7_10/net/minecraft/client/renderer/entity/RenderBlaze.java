package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBlaze;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** 1.7.10's blaze renderer (also the base of mod fire mobs): the blaze model and texture, a 0.5 shadow. */
public class RenderBlaze extends RenderLiving {
    private static final ResourceLocation blazeTextures = new ResourceLocation("textures/entity/blaze.png");

    public RenderBlaze() {
        super(new ModelBlaze(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return blazeTextures;
    }
}
