package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelSheep2;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerSheepWool;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntitySheep;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 sheep renderer: the shorn body with the dyed wool layer on top. */
public class RenderSheep extends RenderLiving<EntitySheep> {
    private static final ResourceLocation SHEARED_SHEEP_TEXTURES = new ResourceLocation("textures/entity/sheep/sheep.png");

    public RenderSheep(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelSheep2(), 0.7F);
        addLayer(new LayerSheepWool(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySheep entity) {
        return SHEARED_SHEEP_TEXTURES;
    }
}
