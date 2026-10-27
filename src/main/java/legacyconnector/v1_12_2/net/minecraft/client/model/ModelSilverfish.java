package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

public class ModelSilverfish extends ModelBase {
    private ModelRenderer[] silverfishBodyParts;
    private ModelRenderer[] silverfishWings;
    private float[] zPlacement = new float[7];
    private static final int[][] silverfishBoxLength = new int[][]{{3, 2, 2}, {4, 3, 2}, {6, 4, 3}, {3, 3, 3}, {2, 2, 3}, {2, 1, 2}, {1, 1, 2}};
    private static final int[][] silverfishTexturePositions = new int[][]{{0, 0}, {0, 4}, {0, 9}, {0, 16}, {0, 22}, {11, 0}, {13, 4}};

    public ModelSilverfish() {
        this.silverfishBodyParts = new ModelRenderer[7];
        float f = -3.5F;

        for (int i = 0; i < this.silverfishBodyParts.length; i++) {
            this.silverfishBodyParts[i] = new ModelRenderer(this, silverfishTexturePositions[i][0], silverfishTexturePositions[i][1]);
            this.silverfishBodyParts[i]
                .addBox(
                    (float)silverfishBoxLength[i][0] * -0.5F,
                    0.0F,
                    (float)silverfishBoxLength[i][2] * -0.5F,
                    silverfishBoxLength[i][0],
                    silverfishBoxLength[i][1],
                    silverfishBoxLength[i][2]
                );
            this.silverfishBodyParts[i].setRotationPoint(0.0F, (float)(24 - silverfishBoxLength[i][1]), f);
            this.zPlacement[i] = f;
            if (i < this.silverfishBodyParts.length - 1) {
                f += (float)(silverfishBoxLength[i][2] + silverfishBoxLength[i + 1][2]) * 0.5F;
            }
        }

        this.silverfishWings = new ModelRenderer[3];
        this.silverfishWings[0] = new ModelRenderer(this, 20, 0);
        this.silverfishWings[0].addBox(-5.0F, 0.0F, (float)silverfishBoxLength[2][2] * -0.5F, 10, 8, silverfishBoxLength[2][2]);
        this.silverfishWings[0].setRotationPoint(0.0F, 16.0F, this.zPlacement[2]);
        this.silverfishWings[1] = new ModelRenderer(this, 20, 11);
        this.silverfishWings[1].addBox(-3.0F, 0.0F, (float)silverfishBoxLength[4][2] * -0.5F, 6, 4, silverfishBoxLength[4][2]);
        this.silverfishWings[1].setRotationPoint(0.0F, 20.0F, this.zPlacement[4]);
        this.silverfishWings[2] = new ModelRenderer(this, 20, 18);
        this.silverfishWings[2].addBox(-3.0F, 0.0F, (float)silverfishBoxLength[4][2] * -0.5F, 6, 5, silverfishBoxLength[1][2]);
        this.silverfishWings[2].setRotationPoint(0.0F, 19.0F, this.zPlacement[1]);
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);

        for (int i = 0; i < this.silverfishBodyParts.length; i++) {
            this.silverfishBodyParts[i].render(f5);
        }

        for (int j = 0; j < this.silverfishWings.length; j++) {
            this.silverfishWings[j].render(f5);
        }
    }

    @Override
    public void setRotationAngles(float f1, float f2, float f, float f3, float f4, float f5, Entity entity) {
        for (int i = 0; i < this.silverfishBodyParts.length; i++) {
            this.silverfishBodyParts[i].rotateAngleY = MathHelper.cos(f * 0.9F + (float)i * 0.15F * (float) Math.PI)
                * (float) Math.PI
                * 0.05F
                * (float)(1 + Math.abs(i - 2));
            this.silverfishBodyParts[i].rotationPointX = MathHelper.sin(f * 0.9F + (float)i * 0.15F * (float) Math.PI)
                * (float) Math.PI
                * 0.2F
                * (float)Math.abs(i - 2);
        }

        this.silverfishWings[0].rotateAngleY = this.silverfishBodyParts[2].rotateAngleY;
        this.silverfishWings[1].rotateAngleY = this.silverfishBodyParts[4].rotateAngleY;
        this.silverfishWings[1].rotationPointX = this.silverfishBodyParts[4].rotationPointX;
        this.silverfishWings[2].rotateAngleY = this.silverfishBodyParts[1].rotateAngleY;
        this.silverfishWings[2].rotationPointX = this.silverfishBodyParts[1].rotationPointX;
    }
}
