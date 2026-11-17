package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** 1.12.2 squid model (64x32 texture): a 12x16x12 body over eight tentacles in a ring that sway with the tentacle angle. */
public class ModelSquid extends ModelBase {
    public ModelRenderer squidBody;
    public ModelRenderer[] squidTentacles = new ModelRenderer[8];

    public ModelSquid() {
        int offset = -16;
        this.squidBody = new ModelRenderer(this, 0, 0);
        this.squidBody.addBox(-6.0F, -8.0F, -6.0F, 12, 16, 12);
        this.squidBody.rotationPointY += (float) (24 + offset);
        for (int i = 0; i < this.squidTentacles.length; ++i) {
            this.squidTentacles[i] = new ModelRenderer(this, 48, 0);
            double angle = (double) i * Math.PI * 2.0D / (double) this.squidTentacles.length;
            float x = (float) Math.cos(angle) * 5.0F;
            float z = (float) Math.sin(angle) * 5.0F;
            this.squidTentacles[i].addBox(-1.0F, 0.0F, -1.0F, 2, 18, 2);
            this.squidTentacles[i].rotationPointX = x;
            this.squidTentacles[i].rotationPointZ = z;
            this.squidTentacles[i].rotationPointY = (float) (31 + offset);
            angle = (double) i * Math.PI * -2.0D / (double) this.squidTentacles.length + (Math.PI / 2D);
            this.squidTentacles[i].rotateAngleY = (float) angle;
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        for (ModelRenderer tentacle : this.squidTentacles) tentacle.rotateAngleX = ageInTicks;
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        this.squidBody.render(scale);
        for (ModelRenderer tentacle : this.squidTentacles) tentacle.render(scale);
    }
}
