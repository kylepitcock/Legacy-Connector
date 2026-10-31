package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/**
 * 1.7.10's squid model: a 12x16x12 body and eight tentacles swung by the age argument. Mods draw squids with it (Starminer's
 * Manbazooka rider is a squid drawn through {@code new ModelSquid()}; the synthesized stub had no boxes, so the rider was invisible).
 */
public class ModelSquid extends ModelBase {
    public ModelRenderer squidBody;
    public ModelRenderer[] squidTentacles = new ModelRenderer[8];

    public ModelSquid() {
        int offset = -16;
        squidBody = new ModelRenderer(this, 0, 0);
        squidBody.addBox(-6.0F, -8.0F, -6.0F, 12, 16, 12);
        squidBody.rotationPointY += 24 + offset;
        for (int i = 0; i < squidTentacles.length; i++) {
            squidTentacles[i] = new ModelRenderer(this, 48, 0);
            double angle = i * Math.PI * 2.0D / squidTentacles.length;
            squidTentacles[i].addBox(-1.0F, 0.0F, -1.0F, 2, 18, 2);
            squidTentacles[i].rotationPointX = (float) Math.cos(angle) * 5.0F;
            squidTentacles[i].rotationPointZ = (float) Math.sin(angle) * 5.0F;
            squidTentacles[i].rotationPointY = 31 + offset;
            squidTentacles[i].rotateAngleY = (float) (i * Math.PI * -2.0D / squidTentacles.length + Math.PI / 2.0D);
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        for (ModelRenderer tentacle : squidTentacles) tentacle.rotateAngleX = age;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        squidBody.render(scale);
        for (ModelRenderer tentacle : squidTentacles) tentacle.render(scale);
    }
}
