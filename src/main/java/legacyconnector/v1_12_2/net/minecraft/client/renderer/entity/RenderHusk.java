package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityZombie;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 husk renderer: the zombie, with the husk texture. */
public class RenderHusk extends RenderZombie {
    private static final ResourceLocation HUSK_TEXTURES = new ResourceLocation("textures/entity/zombie/husk.png");

    public RenderHusk(RenderManager renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombie entity) {
        return HUSK_TEXTURES;
    }
}
