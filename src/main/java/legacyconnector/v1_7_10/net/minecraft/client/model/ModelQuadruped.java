package legacyconnector.v1_7_10.net.minecraft.client.model;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** Four-legged model (pigs, cows, sheep): head, body and four legs of a given height. */
public class ModelQuadruped extends ModelBase {
    public ModelRenderer head;
    public ModelRenderer body;
    public ModelRenderer leg1;
    public ModelRenderer leg2;
    public ModelRenderer leg3;
    public ModelRenderer leg4;
    protected float field_78145_g = 8.0F;
    protected float field_78151_h = 4.0F;

    public ModelQuadruped(int legHeight, float grow) {
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-4.0F, -4.0F, -8.0F, 8, 8, 8, grow);
        head.setRotationPoint(0.0F, 18 - legHeight, -6.0F);
        body = new ModelRenderer(this, 28, 8);
        body.addBox(-5.0F, -10.0F, -7.0F, 10, 16, 8, grow);
        body.setRotationPoint(0.0F, 17 - legHeight, 2.0F);
        leg1 = leg(legHeight, grow, -3.0F, 7.0F);
        leg2 = leg(legHeight, grow, 3.0F, 7.0F);
        leg3 = leg(legHeight, grow, -3.0F, -5.0F);
        leg4 = leg(legHeight, grow, 3.0F, -5.0F);
    }

    private ModelRenderer leg(int height, float grow, float x, float z) {
        ModelRenderer leg = new ModelRenderer(this, 0, 16);
        leg.addBox(-2.0F, 0.0F, -2.0F, 4, height, 4, grow);
        leg.setRotationPoint(x, 24 - height, z);
        return leg;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, entity);
        if (isChild) {
            RenderContext.push();
            RenderContext.translate(0.0F, field_78145_g * scale, field_78151_h * scale);
            head.render(scale);
            RenderContext.pop();
            RenderContext.push();
            RenderContext.scale(0.5F, 0.5F, 0.5F);
            RenderContext.translate(0.0F, 24.0F * scale, 0.0F);
            renderBody(scale);
            RenderContext.pop();
        } else {
            head.render(scale);
            renderBody(scale);
        }
    }

    private void renderBody(float scale) {
        body.render(scale);
        leg1.render(scale);
        leg2.render(scale);
        leg3.render(scale);
        leg4.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale, Entity entity) {
        final float deg = (float) (180.0 / Math.PI);
        final float pi = (float) Math.PI;
        head.rotateAngleX = headPitch / deg;
        head.rotateAngleY = headYaw / deg;
        body.rotateAngleX = pi / 2.0F;
        leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + pi) * 1.4F * limbSwingAmount;
        leg3.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + pi) * 1.4F * limbSwingAmount;
        leg4.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
    }
}
