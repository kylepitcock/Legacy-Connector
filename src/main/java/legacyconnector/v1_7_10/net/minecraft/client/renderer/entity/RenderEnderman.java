package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelEnderman;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** 1.7.10's enderman renderer (also the base of mod endermen): the enderman model and texture; the carried block and eyes are not drawn. */
public class RenderEnderman extends RenderLiving {
    private static final ResourceLocation endermanTextures = new ResourceLocation("textures/entity/enderman/enderman.png");

    public RenderEnderman() {
        super(new ModelEnderman(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return endermanTextures;
    }
}
