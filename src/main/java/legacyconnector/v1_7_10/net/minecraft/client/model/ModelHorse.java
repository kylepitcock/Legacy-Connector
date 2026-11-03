package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityHorse;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's horse model (also donkey, mule, zombie and skeleton horse; 128x128 texture): the box layout, texture offsets and rotation
 * points below are recalled from vanilla 1.7.10 and not verified against the original class. The pose math in
 * {@link #setLivingAnimations} follows vanilla's structure (rearing, head lowered to eat, open mouth, walk swing, head look) but its
 * constants are partly reconstructed; the tilt of rearing is applied geometrically (parts attached to the body are rotated about the
 * body pivot). Legacy fields of vanilla classes are read through the modern entity (phantom fields are not accessible here).
 */
public class ModelHorse extends ModelBase {
    private ModelRenderer head;
    private ModelRenderer mouthTop;
    private ModelRenderer mouthBottom;
    private ModelRenderer horseLeftEar;
    private ModelRenderer horseRightEar;
    private ModelRenderer muleLeftEar;
    private ModelRenderer muleRightEar;
    private ModelRenderer neck;
    private ModelRenderer horseFaceRopes;
    private ModelRenderer mane;
    private ModelRenderer body;
    private ModelRenderer tailBase;
    private ModelRenderer tailMiddle;
    private ModelRenderer tailTip;
    private ModelRenderer backLeftLeg;
    private ModelRenderer backLeftShin;
    private ModelRenderer backLeftHoof;
    private ModelRenderer backRightLeg;
    private ModelRenderer backRightShin;
    private ModelRenderer backRightHoof;
    private ModelRenderer frontLeftLeg;
    private ModelRenderer frontLeftShin;
    private ModelRenderer frontLeftHoof;
    private ModelRenderer frontRightLeg;
    private ModelRenderer frontRightShin;
    private ModelRenderer frontRightHoof;
    private ModelRenderer muleLeftChest;
    private ModelRenderer muleRightChest;
    private ModelRenderer horseSaddleBottom;
    private ModelRenderer horseSaddleFront;
    private ModelRenderer horseSaddleBack;
    private ModelRenderer horseLeftSaddleRope;
    private ModelRenderer horseLeftSaddleMetal;
    private ModelRenderer horseRightSaddleRope;
    private ModelRenderer horseRightSaddleMetal;
    private ModelRenderer horseLeftFaceMetal;
    private ModelRenderer horseRightFaceMetal;
    private ModelRenderer horseLeftRein;
    private ModelRenderer horseRightRein;

    public ModelHorse() {
        this.textureWidth = 128;
        this.textureHeight = 128;
        this.body = new ModelRenderer(this, 0, 34);
        this.body.addBox(-5.0F, -8.0F, -19.0F, 10, 10, 24);
        this.body.setRotationPoint(0.0F, 11.0F, 9.0F);
        this.tailBase = new ModelRenderer(this, 44, 0);
        this.tailBase.addBox(-1.0F, -1.0F, 0.0F, 2, 2, 3);
        this.tailBase.setRotationPoint(0.0F, 3.0F, 14.0F);
        setBoxRotation(this.tailBase, -1.134464F, 0.0F, 0.0F);
        this.tailMiddle = new ModelRenderer(this, 38, 7);
        this.tailMiddle.addBox(-1.5F, -2.0F, 3.0F, 3, 4, 7);
        this.tailMiddle.setRotationPoint(0.0F, 3.0F, 14.0F);
        setBoxRotation(this.tailMiddle, -1.134464F, 0.0F, 0.0F);
        this.tailTip = new ModelRenderer(this, 24, 3);
        this.tailTip.addBox(-1.5F, -4.5F, 9.0F, 3, 4, 7);
        this.tailTip.setRotationPoint(0.0F, 3.0F, 14.0F);
        setBoxRotation(this.tailTip, -1.40215F, 0.0F, 0.0F);
        this.backLeftLeg = new ModelRenderer(this, 78, 29);
        this.backLeftLeg.addBox(-2.5F, -2.0F, -2.5F, 4, 9, 5);
        this.backLeftLeg.setRotationPoint(4.0F, 9.0F, 11.0F);
        this.backLeftShin = new ModelRenderer(this, 78, 43);
        this.backLeftShin.addBox(-2.0F, 0.0F, -1.5F, 3, 5, 3);
        this.backLeftShin.setRotationPoint(4.0F, 16.0F, 11.0F);
        this.backLeftHoof = new ModelRenderer(this, 78, 51);
        this.backLeftHoof.addBox(-2.5F, 5.1F, -2.0F, 4, 3, 4);
        this.backLeftHoof.setRotationPoint(4.0F, 16.0F, 11.0F);
        this.backRightLeg = new ModelRenderer(this, 96, 29);
        this.backRightLeg.addBox(-1.5F, -2.0F, -2.5F, 4, 9, 5);
        this.backRightLeg.setRotationPoint(-4.0F, 9.0F, 11.0F);
        this.backRightShin = new ModelRenderer(this, 96, 43);
        this.backRightShin.addBox(-1.0F, 0.0F, -1.5F, 3, 5, 3);
        this.backRightShin.setRotationPoint(-4.0F, 16.0F, 11.0F);
        this.backRightHoof = new ModelRenderer(this, 96, 51);
        this.backRightHoof.addBox(-1.5F, 5.1F, -2.0F, 4, 3, 4);
        this.backRightHoof.setRotationPoint(-4.0F, 16.0F, 11.0F);
        this.frontLeftLeg = new ModelRenderer(this, 44, 29);
        this.frontLeftLeg.addBox(-1.9F, -1.0F, -2.1F, 3, 8, 4);
        this.frontLeftLeg.setRotationPoint(4.0F, 9.0F, -8.0F);
        this.frontLeftShin = new ModelRenderer(this, 44, 41);
        this.frontLeftShin.addBox(-1.9F, 0.0F, -1.6F, 3, 5, 3);
        this.frontLeftShin.setRotationPoint(4.0F, 16.0F, -8.0F);
        this.frontLeftHoof = new ModelRenderer(this, 44, 51);
        this.frontLeftHoof.addBox(-2.4F, 5.1F, -2.1F, 4, 3, 4);
        this.frontLeftHoof.setRotationPoint(4.0F, 16.0F, -8.0F);
        this.frontRightLeg = new ModelRenderer(this, 60, 29);
        this.frontRightLeg.addBox(-1.1F, -1.0F, -2.1F, 3, 8, 4);
        this.frontRightLeg.setRotationPoint(-4.0F, 9.0F, -8.0F);
        this.frontRightShin = new ModelRenderer(this, 60, 41);
        this.frontRightShin.addBox(-1.1F, 0.0F, -1.6F, 3, 5, 3);
        this.frontRightShin.setRotationPoint(-4.0F, 16.0F, -8.0F);
        this.frontRightHoof = new ModelRenderer(this, 60, 51);
        this.frontRightHoof.addBox(-1.6F, 5.1F, -2.1F, 4, 3, 4);
        this.frontRightHoof.setRotationPoint(-4.0F, 16.0F, -8.0F);
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-2.5F, -10.0F, -1.5F, 5, 5, 7);
        this.head.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.head, 0.5235988F, 0.0F, 0.0F);
        // jaws are children of the head: their rotation point is relative to it and they inherit its rotation
        this.mouthTop = new ModelRenderer(this, 24, 18);
        this.mouthTop.addBox(-2.0F, -10.0F, -7.0F, 4, 3, 6);
        this.mouthTop.setRotationPoint(0.0F, -0.05F, 0.0F);
        this.mouthBottom = new ModelRenderer(this, 24, 27);
        this.mouthBottom.addBox(-2.0F, -7.0F, -6.5F, 4, 2, 5);
        this.mouthBottom.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.head.addChild(this.mouthTop);
        this.head.addChild(this.mouthBottom);
        this.horseLeftEar = new ModelRenderer(this, 0, 0);
        this.horseLeftEar.addBox(0.45F, -12.0F, 4.0F, 2, 3, 1);
        this.horseLeftEar.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.horseLeftEar, 0.5235988F, 0.0F, 0.0F);
        this.horseRightEar = new ModelRenderer(this, 0, 0);
        this.horseRightEar.addBox(-2.45F, -12.0F, 4.0F, 2, 3, 1);
        this.horseRightEar.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.horseRightEar, 0.5235988F, 0.0F, 0.0F);
        this.muleLeftEar = new ModelRenderer(this, 0, 12);
        this.muleLeftEar.addBox(-2.0F, -16.0F, 4.0F, 2, 7, 1);
        this.muleLeftEar.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.muleLeftEar, 0.5235988F, 0.0F, 0.2617994F);
        this.muleRightEar = new ModelRenderer(this, 0, 12);
        this.muleRightEar.addBox(0.0F, -16.0F, 4.0F, 2, 7, 1);
        this.muleRightEar.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.muleRightEar, 0.5235988F, 0.0F, -0.2617994F);
        this.neck = new ModelRenderer(this, 0, 12);
        this.neck.addBox(-2.0F, -16.0F, -1.5F, 4, 12, 7);
        this.neck.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.neck, 0.5235988F, 0.0F, 0.0F);
        this.muleLeftChest = new ModelRenderer(this, 0, 34);
        this.muleLeftChest.addBox(-3.0F, 0.0F, 0.0F, 8, 8, 3);
        this.muleLeftChest.setRotationPoint(-7.5F, 3.0F, 10.0F);
        setBoxRotation(this.muleLeftChest, 0.0F, 1.5707964F, 0.0F);
        this.muleRightChest = new ModelRenderer(this, 0, 47);
        this.muleRightChest.addBox(-3.0F, 0.0F, 0.0F, 8, 8, 3);
        this.muleRightChest.setRotationPoint(4.5F, 3.0F, 10.0F);
        setBoxRotation(this.muleRightChest, 0.0F, 1.5707964F, 0.0F);
        this.mane = new ModelRenderer(this, 58, 0);
        this.mane.addBox(-1.0F, -11.5F, 5.0F, 2, 16, 4);
        this.mane.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.mane, 0.5235988F, 0.0F, 0.0F);
        this.horseFaceRopes = new ModelRenderer(this, 80, 12);
        this.horseFaceRopes.addBox(-2.5F, -10.1F, -7.0F, 5, 5, 12, 0.2F);
        this.horseFaceRopes.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.horseFaceRopes, 0.5235988F, 0.0F, 0.0F);
        this.horseSaddleBottom = new ModelRenderer(this, 80, 0);
        this.horseSaddleBottom.addBox(-5.0F, 0.0F, -3.0F, 10, 1, 8);
        this.horseSaddleBottom.setRotationPoint(0.0F, 2.0F, 2.0F);
        this.horseSaddleFront = new ModelRenderer(this, 106, 9);
        this.horseSaddleFront.addBox(-1.5F, -1.0F, -3.0F, 3, 1, 2);
        this.horseSaddleFront.setRotationPoint(0.0F, 2.0F, 2.0F);
        this.horseSaddleBack = new ModelRenderer(this, 80, 9);
        this.horseSaddleBack.addBox(-4.0F, -1.0F, 3.0F, 8, 1, 2);
        this.horseSaddleBack.setRotationPoint(0.0F, 2.0F, 2.0F);
        this.horseLeftSaddleMetal = new ModelRenderer(this, 74, 0);
        this.horseLeftSaddleMetal.addBox(-0.5F, 6.0F, -1.0F, 1, 2, 2);
        this.horseLeftSaddleMetal.setRotationPoint(5.0F, 3.0F, 2.0F);
        this.horseLeftSaddleRope = new ModelRenderer(this, 70, 0);
        this.horseLeftSaddleRope.addBox(-0.5F, 0.0F, -0.5F, 1, 6, 1);
        this.horseLeftSaddleRope.setRotationPoint(5.0F, 3.0F, 2.0F);
        this.horseRightSaddleMetal = new ModelRenderer(this, 74, 4);
        this.horseRightSaddleMetal.addBox(-0.5F, 6.0F, -1.0F, 1, 2, 2);
        this.horseRightSaddleMetal.setRotationPoint(-5.0F, 3.0F, 2.0F);
        this.horseRightSaddleRope = new ModelRenderer(this, 80, 0);
        this.horseRightSaddleRope.addBox(-0.5F, 0.0F, -0.5F, 1, 6, 1);
        this.horseRightSaddleRope.setRotationPoint(-5.0F, 3.0F, 2.0F);
        this.horseLeftFaceMetal = new ModelRenderer(this, 74, 13);
        this.horseLeftFaceMetal.addBox(1.5F, -8.0F, -4.0F, 1, 2, 2);
        this.horseLeftFaceMetal.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.horseLeftFaceMetal, 0.5235988F, 0.0F, 0.0F);
        this.horseRightFaceMetal = new ModelRenderer(this, 74, 13);
        this.horseRightFaceMetal.addBox(-2.5F, -8.0F, -4.0F, 1, 2, 2);
        this.horseRightFaceMetal.setRotationPoint(0.0F, 4.0F, -10.0F);
        setBoxRotation(this.horseRightFaceMetal, 0.5235988F, 0.0F, 0.0F);
        this.horseLeftRein = new ModelRenderer(this, 44, 10);
        this.horseLeftRein.addBox(2.6F, -6.0F, -6.0F, 0, 3, 16);
        this.horseLeftRein.setRotationPoint(0.0F, 4.0F, -10.0F);
        this.horseRightRein = new ModelRenderer(this, 44, 5);
        this.horseRightRein.addBox(-2.6F, -6.0F, -6.0F, 0, 3, 16);
        this.horseRightRein.setRotationPoint(0.0F, 4.0F, -10.0F);
    }

    private static void setBoxRotation(ModelRenderer part, float x, float y, float z) {
        part.rotateAngleX = x;
        part.rotateAngleY = y;
        part.rotateAngleZ = z;
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        EntityHorse horse = (EntityHorse) entity;
        int type = horse.getHorseType();
        boolean adult = horse.isAdultHorse();
        boolean saddled = adult && horse.isHorseSaddled();
        boolean chested = adult && horse.isChested();
        boolean longEars = type == 1 || type == 2;
        float size = horse.getHorseSize();
        boolean ridden = entity.mcc$entity != null && entity.mcc$entity.isVehicle();
        // legs
        GL11.glPushMatrix();
        GL11.glScalef(size, size, size);
        GL11.glTranslatef(0.0F, 0.95F * (1.0F - size), 0.0F);
        this.backLeftLeg.render(f5);
        this.backLeftShin.render(f5);
        this.backLeftHoof.render(f5);
        this.backRightLeg.render(f5);
        this.backRightShin.render(f5);
        this.backRightHoof.render(f5);
        this.frontLeftLeg.render(f5);
        this.frontLeftShin.render(f5);
        this.frontLeftHoof.render(f5);
        this.frontRightLeg.render(f5);
        this.frontRightShin.render(f5);
        this.frontRightHoof.render(f5);
        GL11.glPopMatrix();
        // body, tail and what rides on the body
        GL11.glPushMatrix();
        GL11.glScalef(size, size, size);
        GL11.glTranslatef(0.0F, 0.95F * (1.0F - size), 0.0F);
        this.body.render(f5);
        this.tailBase.render(f5);
        this.tailMiddle.render(f5);
        this.tailTip.render(f5);
        if (saddled) {
            this.horseSaddleBottom.render(f5);
            this.horseSaddleFront.render(f5);
            this.horseSaddleBack.render(f5);
            this.horseLeftSaddleRope.render(f5);
            this.horseLeftSaddleMetal.render(f5);
            this.horseRightSaddleRope.render(f5);
            this.horseRightSaddleMetal.render(f5);
        }
        if (chested && longEars) {
            this.muleLeftChest.render(f5);
            this.muleRightChest.render(f5);
        }
        GL11.glPopMatrix();
        // head and neck (a foal's head is proportionally larger; factor not verified)
        float headScale = size + 0.15F * (1.0F - size);
        GL11.glPushMatrix();
        GL11.glScalef(headScale, headScale, headScale);
        GL11.glTranslatef(0.0F, 0.95F * (1.0F - headScale), 0.0F);
        this.neck.render(f5);
        this.mane.render(f5);
        this.head.render(f5);
        if (longEars) {
            this.muleLeftEar.render(f5);
            this.muleRightEar.render(f5);
        } else {
            this.horseLeftEar.render(f5);
            this.horseRightEar.render(f5);
        }
        if (saddled) {
            this.horseFaceRopes.render(f5);
            this.horseLeftFaceMetal.render(f5);
            this.horseRightFaceMetal.render(f5);
            if (ridden) {
                this.horseLeftRein.render(f5);
                this.horseRightRein.render(f5);
            }
        }
        GL11.glPopMatrix();
    }

    /** Puts a part that rides on the body (rest position y, z) where the body's tilt carries it. */
    private void attachToBody(ModelRenderer part, float restY, float restZ, float tilt) {
        float dy = restY - 11.0F;
        float dz = restZ - 9.0F;
        float cos = MathHelper.cos(tilt);
        float sin = MathHelper.sin(tilt);
        part.rotationPointY = 11.0F + dy * cos - dz * sin;
        part.rotationPointZ = 9.0F + dy * sin + dz * cos;
        part.rotateAngleX = tilt;
    }

    /** Sets a shin and hoof to hang from the end of their leg (7 units long), turning with it. */
    private void hangShin(ModelRenderer leg, ModelRenderer shin, ModelRenderer hoof) {
        float angle = leg.rotateAngleX;
        shin.rotationPointX = hoof.rotationPointX = leg.rotationPointX;
        shin.rotationPointY = hoof.rotationPointY = leg.rotationPointY + MathHelper.cos(angle) * 7.0F;
        shin.rotationPointZ = hoof.rotationPointZ = leg.rotationPointZ + MathHelper.sin(angle) * 7.0F;
        shin.rotateAngleX = hoof.rotateAngleX = angle;
    }

    /** Copies the neck/head pose to a part that sits on the head (ears, mane, bridle...). */
    private void followHead(ModelRenderer part, float keepZAngle) {
        part.rotationPointY = this.neck.rotationPointY;
        part.rotationPointZ = this.neck.rotationPointZ;
        part.rotateAngleX = this.neck.rotateAngleX;
        part.rotateAngleY = this.neck.rotateAngleY;
        part.rotateAngleZ = keepZAngle;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float f, float f1, float partialTicks) {
        super.setLivingAnimations(entity, f, f1, partialTicks);
        if (!(entity instanceof EntityHorse horse) || !(entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity m)) return;
        float bodyYaw = net.minecraft.util.Mth.rotLerp(partialTicks, m.yBodyRotO, m.yBodyRot);
        float headYaw = net.minecraft.util.Mth.rotLerp(partialTicks, m.yHeadRotO, m.yHeadRot);
        float pitch = m.xRotO + (m.getXRot() - m.xRotO) * partialTicks;
        float lookYaw = headYaw - bodyYaw;
        float lookPitch = pitch / (180.0F / (float) Math.PI);
        if (lookYaw > 20.0F) lookYaw = 20.0F;
        if (lookYaw < -20.0F) lookYaw = -20.0F;
        if (f1 > 0.2F) lookPitch += MathHelper.cos(f * 0.4F) * 0.15F * f1;
        float eating = horse.getGrassEatingAmount(partialTicks);
        float rearing = horse.getRearingAmount(partialTicks);
        float standing = 1.0F - rearing;
        float mouth = horse.getMouthOpennessAngle(partialTicks);
        float age = m.tickCount + partialTicks;
        float swingCos = MathHelper.cos(f * 0.6662F + (float) Math.PI);
        float frontSwing = swingCos * 0.8F * f1;
        float neutral = 1.0F - Math.max(rearing, eating);

        // neck and head: look, rear (head up), eat (head down)
        this.neck.rotateAngleX = rearing * (0.2617994F + lookPitch) + eating * 2.1816616F + neutral * (0.5235988F + lookPitch);
        this.neck.rotateAngleY = rearing * lookYaw / (180.0F / (float) Math.PI) + neutral * lookYaw / (180.0F / (float) Math.PI);
        this.neck.rotateAngleZ = 0.0F;
        this.neck.rotationPointY = rearing * -6.0F + eating * 11.0F + neutral * 4.0F;
        this.neck.rotationPointZ = rearing * -1.0F + eating * -10.0F + neutral * -10.0F;
        this.head.rotationPointY = this.neck.rotationPointY;
        this.head.rotationPointZ = this.neck.rotationPointZ;
        this.head.rotateAngleX = this.neck.rotateAngleX;
        this.head.rotateAngleY = this.neck.rotateAngleY;
        this.head.rotateAngleZ = 0.0F;
        this.mouthTop.rotateAngleX = -0.09424778F * mouth;
        this.mouthBottom.rotateAngleX = 0.15707964F * mouth;
        followHead(this.horseLeftEar, 0.0F);
        followHead(this.horseRightEar, 0.0F);
        followHead(this.muleLeftEar, 0.2617994F);
        followHead(this.muleRightEar, -0.2617994F);
        followHead(this.mane, 0.0F);
        followHead(this.horseFaceRopes, 0.0F);
        followHead(this.horseLeftFaceMetal, 0.0F);
        followHead(this.horseRightFaceMetal, 0.0F);
        followHead(this.horseLeftRein, 0.0F);
        followHead(this.horseRightRein, 0.0F);

        // body tilt while rearing, and everything attached to it
        float tilt = rearing * -((float) Math.PI / 4.0F);
        this.body.rotateAngleX = tilt;
        attachToBody(this.horseSaddleBottom, 2.0F, 2.0F, tilt);
        attachToBody(this.horseSaddleFront, 2.0F, 2.0F, tilt);
        attachToBody(this.horseSaddleBack, 2.0F, 2.0F, tilt);
        attachToBody(this.horseLeftSaddleRope, 3.0F, 2.0F, tilt);
        attachToBody(this.horseLeftSaddleMetal, 3.0F, 2.0F, tilt);
        attachToBody(this.horseRightSaddleRope, 3.0F, 2.0F, tilt);
        attachToBody(this.horseRightSaddleMetal, 3.0F, 2.0F, tilt);
        // chests keep their own 90 degree turn; only their position follows the tilt (the Euler order cannot tilt them about the body axis)
        attachToBody(this.muleLeftChest, 3.0F, 10.0F, tilt);
        attachToBody(this.muleRightChest, 3.0F, 10.0F, tilt);
        this.muleLeftChest.rotateAngleX = this.muleRightChest.rotateAngleX = 0.0F;
        this.muleLeftChest.rotateAngleZ = this.muleRightChest.rotateAngleZ = 0.0F;

        // tail: hangs from the rump, lifts a little when the horse moves
        float tailAngle = -1.134464F + f1 * 0.5F + tilt;
        this.tailBase.rotateAngleX = this.tailMiddle.rotateAngleX = tailAngle;
        this.tailTip.rotateAngleX = tailAngle - 0.26761F;
        // the tail's resting point rides on the tilted body
        float dy = 3.0F - 11.0F;
        float dz = 14.0F - 9.0F;
        this.tailBase.rotationPointY = this.tailMiddle.rotationPointY = this.tailTip.rotationPointY = 11.0F + dy * MathHelper.cos(tilt) - dz * MathHelper.sin(tilt);
        this.tailBase.rotationPointZ = this.tailMiddle.rotationPointZ = this.tailTip.rotationPointZ = 9.0F + dy * MathHelper.sin(tilt) + dz * MathHelper.cos(tilt);

        // legs
        float hindLift = 0.2617994F * rearing;
        float idle = MathHelper.cos(age * 0.6F + (float) Math.PI);
        this.frontLeftLeg.rotationPointY = -2.0F * rearing + 9.0F * standing;
        this.frontLeftLeg.rotationPointZ = -2.0F * rearing + -8.0F * standing;
        this.frontRightLeg.rotationPointY = this.frontLeftLeg.rotationPointY;
        this.frontRightLeg.rotationPointZ = this.frontLeftLeg.rotationPointZ;
        this.backLeftLeg.rotateAngleX = hindLift + swingCos * 0.5F * f1 * standing;
        this.backRightLeg.rotateAngleX = hindLift - swingCos * 0.5F * f1 * standing;
        this.frontLeftLeg.rotateAngleX = (-1.0471976F + idle) * rearing + frontSwing * standing;
        this.frontRightLeg.rotateAngleX = (-1.0471976F - idle) * rearing + -frontSwing * standing;
        hangShin(this.backLeftLeg, this.backLeftShin, this.backLeftHoof);
        hangShin(this.backRightLeg, this.backRightShin, this.backRightHoof);
        hangShin(this.frontLeftLeg, this.frontLeftShin, this.frontLeftHoof);
        hangShin(this.frontRightLeg, this.frontRightShin, this.frontRightHoof);
    }
}
