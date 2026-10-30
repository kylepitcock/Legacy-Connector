package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** 1.12.2 endermite: four body segments that wiggle side to side. */
public class ModelEnderMite extends ModelBase {
    private static final int[][] BODY_SIZES = new int[][] {{4, 3, 2}, {6, 4, 5}, {3, 3, 1}, {1, 2, 1}};
    private static final int[][] BODY_TEXS = new int[][] {{0, 0}, {0, 5}, {0, 14}, {0, 18}};
    private static final int BODY_COUNT = BODY_SIZES.length;
    private final ModelRenderer[] bodyParts;

    public ModelEnderMite() {
        bodyParts = new ModelRenderer[BODY_COUNT];
        float f = -3.5F;
        for (int i = 0; i < bodyParts.length; ++i) {
            bodyParts[i] = new ModelRenderer(this, BODY_TEXS[i][0], BODY_TEXS[i][1]);
            bodyParts[i].addBox((float) BODY_SIZES[i][0] * -0.5F, 0.0F, (float) BODY_SIZES[i][2] * -0.5F, BODY_SIZES[i][0], BODY_SIZES[i][1], BODY_SIZES[i][2]);
            bodyParts[i].setRotationPoint(0.0F, (float) (24 - BODY_SIZES[i][1]), f);
            if (i < bodyParts.length - 1) f += (float) (BODY_SIZES[i][2] + BODY_SIZES[i + 1][2]) * 0.5F;
        }
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        for (ModelRenderer part : bodyParts) part.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        for (int i = 0; i < bodyParts.length; ++i) {
            bodyParts[i].rotateAngleY = MathHelper.cos(ageInTicks * 0.9F + (float) i * 0.15F * (float) Math.PI) * (float) Math.PI * 0.01F * (float) (1 + Math.abs(i - 2));
            bodyParts[i].rotationPointX = MathHelper.sin(ageInTicks * 0.9F + (float) i * 0.15F * (float) Math.PI) * (float) Math.PI * 0.1F * (float) Math.abs(i - 2);
        }
    }
}
