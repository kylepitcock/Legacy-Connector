package mccompat.v1_7_10.net.minecraft.client.model;

import mccompat.v1_7_10.net.minecraft.entity.Entity;

public class ModelSlime extends ModelBase {
    public ModelRenderer slimeBodies;
    public ModelRenderer slimeRightEye;
    public ModelRenderer slimeLeftEye;
    public ModelRenderer slimeMouth;

    public ModelSlime(int size) {
        slimeBodies = new ModelRenderer(this, 0, size);
        slimeBodies.addBox(-4.0F, 16.0F, -4.0F, 8, 8, 8);
        if (size > 0) {
            slimeBodies = new ModelRenderer(this, 0, size);
            slimeBodies.addBox(-3.0F, 17.0F, -3.0F, 6, 6, 6);
            slimeRightEye = new ModelRenderer(this, 32, 0);
            slimeRightEye.addBox(-3.25F, 18.0F, -3.5F, 2, 2, 2);
            slimeLeftEye = new ModelRenderer(this, 32, 4);
            slimeLeftEye.addBox(1.25F, 18.0F, -3.5F, 2, 2, 2);
            slimeMouth = new ModelRenderer(this, 32, 8);
            slimeMouth.addBox(0.0F, 21.0F, -3.5F, 1, 1, 1);
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        slimeBodies.render(scale);
        if (slimeRightEye != null) {
            slimeRightEye.render(scale);
            slimeLeftEye.render(scale);
            slimeMouth.render(scale);
        }
    }
}
