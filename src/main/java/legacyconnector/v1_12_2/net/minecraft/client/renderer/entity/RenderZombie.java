package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelZombie;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityZombie;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 zombie renderer: the biped with arms held out, and zombie-shaped armour. */
public class RenderZombie extends RenderBiped<EntityZombie> {
    private static final ResourceLocation ZOMBIE_TEXTURES = new ResourceLocation("textures/entity/zombie/zombie.png");

    public RenderZombie(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelZombie(), 0.5F);
        addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelZombie(0.5F, true);
                this.modelArmor = new ModelZombie(1.0F, true);
            }
        });
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombie entity) {
        return ZOMBIE_TEXTURES;
    }
}
