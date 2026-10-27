package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemArmor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** 1.12.2 head layer: a non-helmet item in the head slot (pumpkins, skulls, banners) on the head part, in the HEAD transform. */
public class LayerCustomHead implements LayerRenderer<EntityLivingBase> {
    private final ModelRenderer modelRenderer;

    public LayerCustomHead(ModelRenderer modelRendererIn) {
        this.modelRenderer = modelRendererIn;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack stack = entitylivingbaseIn.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof ItemArmor armor && armor.getEquipmentSlot() == EntityEquipmentSlot.HEAD) return;
        GlStateManager.pushMatrix();
        if (entitylivingbaseIn.isSneaking()) GlStateManager.translate(0.0F, 0.2F, 0.0F);
        if (entitylivingbaseIn.isChild()) {
            GlStateManager.translate(0.0F, 0.5F * scale, 0.0F);
            GlStateManager.scale(0.7F, 0.7F, 0.7F);
            GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
        }
        modelRenderer.postRender(0.0625F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translate(0.0F, -0.25F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.625F, -0.625F, -0.625F);
        RenderItem.mcc$instance().renderItem(stack, entitylivingbaseIn, ItemCameraTransforms.TransformType.HEAD, false);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
