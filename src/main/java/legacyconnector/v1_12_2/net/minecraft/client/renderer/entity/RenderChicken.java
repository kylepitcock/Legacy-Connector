package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelChicken;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityChicken;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 chicken renderer. */
public class RenderChicken extends RenderLiving<EntityChicken> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/chicken.png");

    public RenderChicken(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelChicken(), 0.3F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityChicken entity) {
        return TEXTURE;
    }

    /** The wing flap angle (the model reads it as the age argument). */
    @Override
    protected float handleRotationFloat(EntityChicken livingBase, float partialTicks) {
        float f = livingBase.oFlap + (livingBase.wingRotation - livingBase.oFlap) * partialTicks;
        float f1 = livingBase.oFlapSpeed + (livingBase.destPos - livingBase.oFlapSpeed) * partialTicks;
        return (legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.sin(f) + 1.0F) * f1;
    }
}
