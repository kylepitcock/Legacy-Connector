package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityBoat;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/** The 1.12 boat (texture 128x64): bottom, four sides and two paddles; the second pass draws the plane that hides water inside. */
public class ModelBoat extends ModelBase implements IMultipassModel {
    public ModelRenderer[] boatSides = new ModelRenderer[5];
    public ModelRenderer[] paddles = new ModelRenderer[2];
    public ModelRenderer noWater;

    public ModelBoat() {
        boatSides[0] = new ModelRenderer(this, 0, 0).setTextureSize(128, 64);
        boatSides[1] = new ModelRenderer(this, 0, 19).setTextureSize(128, 64);
        boatSides[2] = new ModelRenderer(this, 0, 27).setTextureSize(128, 64);
        boatSides[3] = new ModelRenderer(this, 0, 35).setTextureSize(128, 64);
        boatSides[4] = new ModelRenderer(this, 0, 43).setTextureSize(128, 64);
        boatSides[0].addBox(-14.0F, -9.0F, -3.0F, 28, 16, 3, 0.0F);
        boatSides[0].setRotationPoint(0.0F, 3.0F, 1.0F);
        boatSides[1].addBox(-13.0F, -7.0F, -1.0F, 18, 6, 2, 0.0F);
        boatSides[1].setRotationPoint(-15.0F, 4.0F, 4.0F);
        boatSides[2].addBox(-8.0F, -7.0F, -1.0F, 16, 6, 2, 0.0F);
        boatSides[2].setRotationPoint(15.0F, 4.0F, 0.0F);
        boatSides[3].addBox(-14.0F, -7.0F, -1.0F, 28, 6, 2, 0.0F);
        boatSides[3].setRotationPoint(0.0F, 4.0F, -9.0F);
        boatSides[4].addBox(-14.0F, -7.0F, -1.0F, 28, 6, 2, 0.0F);
        boatSides[4].setRotationPoint(0.0F, 4.0F, 9.0F);
        boatSides[0].rotateAngleX = (float) Math.PI / 2F;
        boatSides[1].rotateAngleY = (float) Math.PI * 3F / 2F;
        boatSides[2].rotateAngleY = (float) Math.PI / 2F;
        boatSides[3].rotateAngleY = (float) Math.PI;
        paddles[0] = makePaddle(true);
        paddles[0].setRotationPoint(3.0F, -5.0F, 9.0F);
        paddles[1] = makePaddle(false);
        paddles[1].setRotationPoint(3.0F, -5.0F, -9.0F);
        paddles[1].rotateAngleY = (float) Math.PI;
        paddles[0].rotateAngleZ = 0.19634955F;
        paddles[1].rotateAngleZ = 0.19634955F;
        noWater = new ModelRenderer(this, 0, 0).setTextureSize(128, 64);
        noWater.addBox(-14.0F, -9.0F, -3.0F, 28, 16, 3, 0.0F);
        noWater.setRotationPoint(0.0F, -3.0F, 1.0F);
        noWater.rotateAngleX = (float) Math.PI / 2F;
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        for (int i = 0; i < 5; i++) boatSides[i].render(scale);
        if (entityIn instanceof EntityBoat boat) {
            renderPaddle(boat, 0, scale, limbSwing);
            renderPaddle(boat, 1, scale, limbSwing);
        } else {
            paddles[0].render(scale);
            paddles[1].render(scale);
        }
    }

    @Override
    public void renderMultipass(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.colorMask(false, false, false, false);
        noWater.render(scale);
        GlStateManager.colorMask(true, true, true, true);
    }

    protected ModelRenderer makePaddle(boolean left) {
        ModelRenderer paddle = new ModelRenderer(this, 62, left ? 0 : 20).setTextureSize(128, 64);
        paddle.addBox(-1.0F, 0.0F, -5.0F, 2, 2, 18);
        paddle.addBox(left ? -1.001F : 0.001F, -3.0F, 8.0F, 1, 6, 7);
        return paddle;
    }

    protected void renderPaddle(EntityBoat boat, int paddle, float scale, float limbSwing) {
        float f = mcc$rowingTime(boat, paddle, limbSwing);
        ModelRenderer renderer = paddles[paddle];
        renderer.rotateAngleX = (float) MathHelper.clampedLerp(-1.0471975803375244D, -0.2617993950843811D, (MathHelper.sin(-f) + 1.0F) / 2.0F);
        renderer.rotateAngleY = (float) MathHelper.clampedLerp(-(Math.PI / 4D), Math.PI / 4D, (MathHelper.sin(-f + 1.0F) + 1.0F) / 2.0F);
        if (paddle == 1) renderer.rotateAngleY = (float) Math.PI - renderer.rotateAngleY;
        renderer.render(scale);
    }

    /** EntityBoat.getRowingTime: the modern boat's paddle phase for a vanilla boat, the legacy paddle positions for a mod's own. */
    private static float mcc$rowingTime(EntityBoat boat, int side, float partialTicks) {
        if (boat.mcc$entity instanceof net.minecraft.world.entity.vehicle.Boat modern) return modern.getRowingTime(side, partialTicks);
        if (!boat.getPaddleState(side)) return 0.0F;
        float pos = boat.paddlePositions[side];
        return (float) MathHelper.clampedLerp(pos - 0.39269909262657166D, pos, partialTicks);
    }
}
