package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

/** 1.12.2 render layer: drawn after a living entity's main model, in the model's space (armour, held items, glowing eyes...). */
public interface LayerRenderer<E extends EntityLivingBase> {
    void doRenderLayer(E entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale);

    boolean shouldCombineTextures();
}
