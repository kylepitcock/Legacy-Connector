package legacyconnector.v1_7_10.net.minecraft.client.model;

/** The skeleton: a biped with 2x2 limbs. */
public class ModelSkeleton extends ModelBiped {
    public ModelSkeleton() {
        this(0.0F);
    }

    public ModelSkeleton(float grow) {
        super(grow, 0.0F, 64, 32);
        bipedRightArm = new ModelRenderer(this, 40, 16);
        bipedRightArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, grow);
        bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        bipedLeftArm = new ModelRenderer(this, 40, 16);
        bipedLeftArm.mirror = true;
        bipedLeftArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, grow);
        bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        bipedRightLeg = new ModelRenderer(this, 0, 16);
        bipedRightLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, grow);
        bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
        bipedLeftLeg = new ModelRenderer(this, 0, 16);
        bipedLeftLeg.mirror = true;
        bipedLeftLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, grow);
        bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
    }
}
