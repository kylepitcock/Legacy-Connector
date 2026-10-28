package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelSkeleton;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.AbstractSkeleton;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 skeleton renderer: the thin-limbed biped with skeleton-shaped armour. */
public class RenderSkeleton extends RenderBiped<AbstractSkeleton> {
    private static final ResourceLocation SKELETON_TEXTURES = new ResourceLocation("textures/entity/skeleton/skeleton.png");

    public RenderSkeleton(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelSkeleton(), 0.5F);
        addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelSkeleton(0.5F, true);
                this.modelArmor = new ModelSkeleton(1.0F, true);
            }
        });
    }

    @Override
    public void transformHeldFull3DItemLayer() {
        GlStateManager.translate(0.09375F, 0.1875F, 0.0F);
    }

    @Override
    protected ResourceLocation getEntityTexture(AbstractSkeleton entity) {
        return SKELETON_TEXTURES;
    }
}
