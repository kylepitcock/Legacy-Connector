package legacyconnector.v1_7_10.net.minecraft.client.model;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

public class ModelGhast extends ModelBase {
    ModelRenderer body;
    ModelRenderer[] tentacles = new ModelRenderer[9];

    public ModelGhast() {
        byte b0 = -16;
        this.body = new ModelRenderer(this, 0, 0);
        this.body.addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
        this.body.rotationPointY += (float)(24 + b0);
        Random random = new Random(1660L);

        for (int i = 0; i < this.tentacles.length; i++) {
            this.tentacles[i] = new ModelRenderer(this, 0, 0);
            float f = (((float)(i % 3) - (float)(i / 3 % 2) * 0.5F + 0.25F) / 2.0F * 2.0F - 1.0F) * 5.0F;
            float f1 = ((float)(i / 3) / 2.0F * 2.0F - 1.0F) * 5.0F;
            int j = random.nextInt(7) + 8;
            this.tentacles[i].addBox(-1.0F, 0.0F, -1.0F, 2, j, 2);
            this.tentacles[i].rotationPointX = f;
            this.tentacles[i].rotationPointZ = f1;
            this.tentacles[i].rotationPointY = (float)(31 + b0);
        }
    }

    @Override
    public void setRotationAngles(float f1, float f2, float f, float f3, float f4, float f5, Entity entity) {
        for (int i = 0; i < this.tentacles.length; i++) {
            this.tentacles[i].rotateAngleX = 0.2F * MathHelper.sin(f * 0.3F + (float)i) + 0.4F;
        }
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.6F, 0.0F);
        this.body.render(f5);

        for (ModelRenderer modelrenderer : this.tentacles) {
            modelrenderer.render(f5);
        }

        GL11.glPopMatrix();
    }
}
