package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.boss.EntityDragon;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

public class ModelDragon extends ModelBase {
    private ModelRenderer head;
    private ModelRenderer spine;
    private ModelRenderer jaw;
    private ModelRenderer body;
    private ModelRenderer rearLeg;
    private ModelRenderer frontLeg;
    private ModelRenderer rearLegTip;
    private ModelRenderer frontLegTip;
    private ModelRenderer rearFoot;
    private ModelRenderer frontFoot;
    private ModelRenderer wing;
    private ModelRenderer wingTip;
    private float partialTicks;

    public ModelDragon(float f1) {
        this.textureWidth = 256;
        this.textureHeight = 256;
        this.setTextureOffset("body.body", 0, 0);
        this.setTextureOffset("wing.skin", -56, 88);
        this.setTextureOffset("wingtip.skin", -56, 144);
        this.setTextureOffset("rearleg.main", 0, 0);
        this.setTextureOffset("rearfoot.main", 112, 0);
        this.setTextureOffset("rearlegtip.main", 196, 0);
        this.setTextureOffset("head.upperhead", 112, 30);
        this.setTextureOffset("wing.bone", 112, 88);
        this.setTextureOffset("head.upperlip", 176, 44);
        this.setTextureOffset("jaw.jaw", 176, 65);
        this.setTextureOffset("frontleg.main", 112, 104);
        this.setTextureOffset("wingtip.bone", 112, 136);
        this.setTextureOffset("frontfoot.main", 144, 104);
        this.setTextureOffset("neck.box", 192, 104);
        this.setTextureOffset("frontlegtip.main", 226, 138);
        this.setTextureOffset("body.scale", 220, 53);
        this.setTextureOffset("head.scale", 0, 0);
        this.setTextureOffset("neck.scale", 48, 0);
        this.setTextureOffset("head.nostril", 112, 0);
        float f = -16.0F;
        this.head = new ModelRenderer(this, "head");
        this.head.addBox("upperlip", -6.0F, -1.0F, -8.0F + f, 12, 5, 16);
        this.head.addBox("upperhead", -8.0F, -8.0F, 6.0F + f, 16, 16, 16);
        this.head.mirror = true;
        this.head.addBox("scale", -5.0F, -12.0F, 12.0F + f, 2, 4, 6);
        this.head.addBox("nostril", -5.0F, -3.0F, -6.0F + f, 2, 2, 4);
        this.head.mirror = false;
        this.head.addBox("scale", 3.0F, -12.0F, 12.0F + f, 2, 4, 6);
        this.head.addBox("nostril", 3.0F, -3.0F, -6.0F + f, 2, 2, 4);
        this.jaw = new ModelRenderer(this, "jaw");
        this.jaw.setRotationPoint(0.0F, 4.0F, 8.0F + f);
        this.jaw.addBox("jaw", -6.0F, 0.0F, -16.0F, 12, 4, 16);
        this.head.addChild(this.jaw);
        this.spine = new ModelRenderer(this, "neck");
        this.spine.addBox("box", -5.0F, -5.0F, -5.0F, 10, 10, 10);
        this.spine.addBox("scale", -1.0F, -9.0F, -3.0F, 2, 4, 6);
        this.body = new ModelRenderer(this, "body");
        this.body.setRotationPoint(0.0F, 4.0F, 8.0F);
        this.body.addBox("body", -12.0F, 0.0F, -16.0F, 24, 24, 64);
        this.body.addBox("scale", -1.0F, -6.0F, -10.0F, 2, 6, 12);
        this.body.addBox("scale", -1.0F, -6.0F, 10.0F, 2, 6, 12);
        this.body.addBox("scale", -1.0F, -6.0F, 30.0F, 2, 6, 12);
        this.wing = new ModelRenderer(this, "wing");
        this.wing.setRotationPoint(-12.0F, 5.0F, 2.0F);
        this.wing.addBox("bone", -56.0F, -4.0F, -4.0F, 56, 8, 8);
        this.wing.addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56);
        this.wingTip = new ModelRenderer(this, "wingtip");
        this.wingTip.setRotationPoint(-56.0F, 0.0F, 0.0F);
        this.wingTip.addBox("bone", -56.0F, -2.0F, -2.0F, 56, 4, 4);
        this.wingTip.addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56);
        this.wing.addChild(this.wingTip);
        this.frontLeg = new ModelRenderer(this, "frontleg");
        this.frontLeg.setRotationPoint(-12.0F, 20.0F, 2.0F);
        this.frontLeg.addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8);
        this.frontLegTip = new ModelRenderer(this, "frontlegtip");
        this.frontLegTip.setRotationPoint(0.0F, 20.0F, -1.0F);
        this.frontLegTip.addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6);
        this.frontLeg.addChild(this.frontLegTip);
        this.frontFoot = new ModelRenderer(this, "frontfoot");
        this.frontFoot.setRotationPoint(0.0F, 23.0F, 0.0F);
        this.frontFoot.addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16);
        this.frontLegTip.addChild(this.frontFoot);
        this.rearLeg = new ModelRenderer(this, "rearleg");
        this.rearLeg.setRotationPoint(-16.0F, 16.0F, 42.0F);
        this.rearLeg.addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16);
        this.rearLegTip = new ModelRenderer(this, "rearlegtip");
        this.rearLegTip.setRotationPoint(0.0F, 32.0F, -4.0F);
        this.rearLegTip.addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12);
        this.rearLeg.addChild(this.rearLegTip);
        this.rearFoot = new ModelRenderer(this, "rearfoot");
        this.rearFoot.setRotationPoint(0.0F, 31.0F, 4.0F);
        this.rearFoot.addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24);
        this.rearLegTip.addChild(this.rearFoot);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entitylivingbase, float f1, float f2, float f) {
        this.partialTicks = f;
    }

    @Override
    public void render(Entity entity, float f13, float f14, float f15, float f16, float f17, float f) {
        GL11.glPushMatrix();
        EntityDragon entitydragon = (EntityDragon)entity;
        float f1 = entitydragon.prevAnimTime + (entitydragon.animTime - entitydragon.prevAnimTime) * this.partialTicks;
        this.jaw.rotateAngleX = (float)(Math.sin((double)(f1 * (float) Math.PI * 2.0F)) + 1.0) * 0.2F;
        float f2 = (float)(Math.sin((double)(f1 * (float) Math.PI * 2.0F - 1.0F)) + 1.0);
        f2 = (f2 * f2 * 1.0F + f2 * 2.0F) * 0.05F;
        GL11.glTranslatef(0.0F, f2 - 2.0F, -3.0F);
        GL11.glRotatef(f2 * 2.0F, 1.0F, 0.0F, 0.0F);
        float f3 = -30.0F;
        float f5 = 0.0F;
        float f6 = 1.5F;
        double[] adouble = entitydragon.getMovementOffsets(6, this.partialTicks);
        float f7 = this.updateRotations(entitydragon.getMovementOffsets(5, this.partialTicks)[0] - entitydragon.getMovementOffsets(10, this.partialTicks)[0]);
        float f8 = this.updateRotations(entitydragon.getMovementOffsets(5, this.partialTicks)[0] + (double)(f7 / 2.0F));
        f3 += 2.0F;
        float f9 = f1 * (float) Math.PI * 2.0F;
        f3 = 20.0F;
        float f4 = -12.0F;

        for (int i = 0; i < 5; i++) {
            double[] adouble1 = entitydragon.getMovementOffsets(5 - i, this.partialTicks);
            float f10 = (float)Math.cos((double)((float)i * 0.45F + f9)) * 0.15F;
            this.spine.rotateAngleY = this.updateRotations(adouble1[0] - adouble[0]) * (float) Math.PI / 180.0F * f6;
            this.spine.rotateAngleX = f10 + (float)(adouble1[1] - adouble[1]) * (float) Math.PI / 180.0F * f6 * 5.0F;
            this.spine.rotateAngleZ = -this.updateRotations(adouble1[0] - (double)f8) * (float) Math.PI / 180.0F * f6;
            this.spine.rotationPointY = f3;
            this.spine.rotationPointZ = f4;
            this.spine.rotationPointX = f5;
            f3 = (float)((double)f3 + Math.sin((double)this.spine.rotateAngleX) * 10.0);
            f4 = (float)((double)f4 - Math.cos((double)this.spine.rotateAngleY) * Math.cos((double)this.spine.rotateAngleX) * 10.0);
            f5 = (float)((double)f5 - Math.sin((double)this.spine.rotateAngleY) * Math.cos((double)this.spine.rotateAngleX) * 10.0);
            this.spine.render(f);
        }

        this.head.rotationPointY = f3;
        this.head.rotationPointZ = f4;
        this.head.rotationPointX = f5;
        double[] adouble2 = entitydragon.getMovementOffsets(0, this.partialTicks);
        this.head.rotateAngleY = this.updateRotations(adouble2[0] - adouble[0]) * (float) Math.PI / 180.0F * 1.0F;
        this.head.rotateAngleZ = -this.updateRotations(adouble2[0] - (double)f8) * (float) Math.PI / 180.0F * 1.0F;
        this.head.render(f);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-f7 * f6 * 1.0F, 0.0F, 0.0F, 1.0F);
        GL11.glTranslatef(0.0F, -1.0F, 0.0F);
        this.body.rotateAngleZ = 0.0F;
        this.body.render(f);

        for (int j = 0; j < 2; j++) {
            GL11.glEnable(2884);
            float f12 = f1 * (float) Math.PI * 2.0F;
            this.wing.rotateAngleX = 0.125F - (float)Math.cos((double)f12) * 0.2F;
            this.wing.rotateAngleY = 0.25F;
            this.wing.rotateAngleZ = (float)(Math.sin((double)f12) + 0.125) * 0.8F;
            this.wingTip.rotateAngleZ = -((float)(Math.sin((double)(f12 + 2.0F)) + 0.5)) * 0.75F;
            this.rearLeg.rotateAngleX = 1.0F + f2 * 0.1F;
            this.rearLegTip.rotateAngleX = 0.5F + f2 * 0.1F;
            this.rearFoot.rotateAngleX = 0.75F + f2 * 0.1F;
            this.frontLeg.rotateAngleX = 1.3F + f2 * 0.1F;
            this.frontLegTip.rotateAngleX = -0.5F - f2 * 0.1F;
            this.frontFoot.rotateAngleX = 0.75F + f2 * 0.1F;
            this.wing.render(f);
            this.frontLeg.render(f);
            this.rearLeg.render(f);
            GL11.glScalef(-1.0F, 1.0F, 1.0F);
            if (j == 0) {
                GL11.glCullFace(1028);
            }
        }

        GL11.glPopMatrix();
        GL11.glCullFace(1029);
        GL11.glDisable(2884);
        float f11 = -((float)Math.sin((double)(f1 * (float) Math.PI * 2.0F))) * 0.0F;
        f9 = f1 * (float) Math.PI * 2.0F;
        f3 = 10.0F;
        f4 = 60.0F;
        f5 = 0.0F;
        adouble = entitydragon.getMovementOffsets(11, this.partialTicks);

        for (int k = 0; k < 12; k++) {
            adouble2 = entitydragon.getMovementOffsets(12 + k, this.partialTicks);
            f11 = (float)((double)f11 + Math.sin((double)((float)k * 0.45F + f9)) * 0.05F);
            this.spine.rotateAngleY = (this.updateRotations(adouble2[0] - adouble[0]) * f6 + 180.0F) * (float) Math.PI / 180.0F;
            this.spine.rotateAngleX = f11 + (float)(adouble2[1] - adouble[1]) * (float) Math.PI / 180.0F * f6 * 5.0F;
            this.spine.rotateAngleZ = this.updateRotations(adouble2[0] - (double)f8) * (float) Math.PI / 180.0F * f6;
            this.spine.rotationPointY = f3;
            this.spine.rotationPointZ = f4;
            this.spine.rotationPointX = f5;
            f3 = (float)((double)f3 + Math.sin((double)this.spine.rotateAngleX) * 10.0);
            f4 = (float)((double)f4 - Math.cos((double)this.spine.rotateAngleY) * Math.cos((double)this.spine.rotateAngleX) * 10.0);
            f5 = (float)((double)f5 - Math.sin((double)this.spine.rotateAngleY) * Math.cos((double)this.spine.rotateAngleX) * 10.0);
            this.spine.render(f);
        }

        GL11.glPopMatrix();
    }

    private float updateRotations(double d0) {
        while (d0 >= 180.0) {
            d0 -= 360.0;
        }

        while (d0 < -180.0) {
            d0 += 360.0;
        }

        return (float)d0;
    }
}
