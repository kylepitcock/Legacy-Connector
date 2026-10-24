package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelCreeper;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** Creeper renderer (also the base of mod creepers). */
public class RenderCreeper extends RenderLiving {
    private static final ResourceLocation CREEPER = new ResourceLocation("textures/entity/creeper/creeper.png");

    public RenderCreeper() {
        super(new ModelCreeper(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return CREEPER;
    }
}
