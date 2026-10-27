package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide;

/** 1.12.2 held-item layer: the items in both hands at the hands of a biped model, drawn by the modern item renderer. */
public class LayerHeldItem implements LayerRenderer<EntityLivingBase> {
    protected final RenderLivingBase<?> livingEntityRenderer;

    public LayerHeldItem(RenderLivingBase<?> livingEntityRendererIn) {
        this.livingEntityRenderer = livingEntityRendererIn;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        boolean rightHanded = entitylivingbaseIn.getPrimaryHand() == EnumHandSide.RIGHT;
        ItemStack left = rightHanded ? entitylivingbaseIn.getHeldItemOffhand() : entitylivingbaseIn.getHeldItemMainhand();
        ItemStack right = rightHanded ? entitylivingbaseIn.getHeldItemMainhand() : entitylivingbaseIn.getHeldItemOffhand();
        if (left.isEmpty() && right.isEmpty()) return;
        GlStateManager.pushMatrix();
        if (livingEntityRenderer.getMainModel().isChild) {
            GlStateManager.translate(0.0F, 0.75F, 0.0F);
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
        }
        renderHeldItem(entitylivingbaseIn, right, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, EnumHandSide.RIGHT);
        renderHeldItem(entitylivingbaseIn, left, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, EnumHandSide.LEFT);
        GlStateManager.popMatrix();
    }

    private void renderHeldItem(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType transform, EnumHandSide handSide) {
        if (stack.isEmpty()) return;
        GlStateManager.pushMatrix();
        if (entity.isSneaking()) GlStateManager.translate(0.0F, 0.2F, 0.0F);
        translateToHand(handSide);
        GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        boolean left = handSide == EnumHandSide.LEFT;
        GlStateManager.translate((float) (left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        RenderItem.mcc$instance().renderItem(stack, entity, transform, left);
        GlStateManager.popMatrix();
    }

    protected void translateToHand(EnumHandSide handSide) {
        if (livingEntityRenderer.getMainModel() instanceof ModelBiped biped) biped.postRenderArm(0.0625F, handSide);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
