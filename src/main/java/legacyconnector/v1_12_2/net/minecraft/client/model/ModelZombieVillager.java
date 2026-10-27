package legacyconnector.v1_12_2.net.minecraft.client.model;

/**
 * 1.12.2 zombie villager model: the villager head (with nose) and robed body on zombie limbs, arms held out like a zombie's. With the
 * armour flag set it is the plain armour shape.
 */
public class ModelZombieVillager extends ModelZombie {
    public ModelZombieVillager() {
        this(0.0F, 0.0F, false);
    }

    public ModelZombieVillager(float modelSize, float yOffset, boolean armor) {
        super(modelSize, 0.0F, 64, armor ? 32 : 64);
        if (armor) {
            bipedHead = new ModelRenderer(this, 0, 0);
            bipedHead.addBox(-4.0F, -10.0F, -4.0F, 8, 8, 8, modelSize);
            bipedHead.setRotationPoint(0.0F, yOffset, 0.0F);
            bipedBody = new ModelRenderer(this, 16, 16);
            bipedBody.setRotationPoint(0.0F, yOffset, 0.0F);
            bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize + 0.1F);
            bipedRightLeg = new ModelRenderer(this, 0, 16);
            bipedRightLeg.setRotationPoint(-2.0F, 12.0F + yOffset, 0.0F);
            bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.1F);
            bipedLeftLeg = new ModelRenderer(this, 0, 16);
            bipedLeftLeg.mirror = true;
            bipedLeftLeg.setRotationPoint(2.0F, 12.0F + yOffset, 0.0F);
            bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.1F);
        } else {
            bipedHead = new ModelRenderer(this, 0, 0);
            bipedHead.setRotationPoint(0.0F, yOffset, 0.0F);
            bipedHead.setTextureOffset(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, modelSize);
            bipedHead.setTextureOffset(24, 0).addBox(-1.0F, -3.0F, -6.0F, 2, 4, 2, modelSize);
            bipedBody = new ModelRenderer(this, 16, 20);
            bipedBody.setRotationPoint(0.0F, yOffset, 0.0F);
            bipedBody.addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6, modelSize);
            bipedBody.setTextureOffset(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, modelSize + 0.05F);
            bipedRightArm = new ModelRenderer(this, 44, 38);
            bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            bipedRightArm.setRotationPoint(-5.0F, 2.0F + yOffset, 0.0F);
            bipedLeftArm = new ModelRenderer(this, 44, 38);
            bipedLeftArm.mirror = true;
            bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            bipedLeftArm.setRotationPoint(5.0F, 2.0F + yOffset, 0.0F);
            bipedRightLeg = new ModelRenderer(this, 0, 22);
            bipedRightLeg.setRotationPoint(-2.0F, 12.0F + yOffset, 0.0F);
            bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
            bipedLeftLeg = new ModelRenderer(this, 0, 22);
            bipedLeftLeg.mirror = true;
            bipedLeftLeg.setRotationPoint(2.0F, 12.0F + yOffset, 0.0F);
            bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
        }
    }
}
