package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** A model with a second render pass (the boat's water-hiding plane, drawn with colour writes off). */
public interface IMultipassModel {
    void renderMultipass(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale);
}
