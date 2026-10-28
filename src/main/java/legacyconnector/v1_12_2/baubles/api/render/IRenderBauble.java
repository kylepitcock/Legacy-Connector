package legacyconnector.v1_12_2.baubles.api.render;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Baubles 1.12's worn-bauble renderer, with Baubles' own transforms for drawing on the head, face and chest. */
public interface IRenderBauble {
    void onPlayerBaubleRender(ItemStack stack, EntityPlayer player, RenderType type, float partialTicks);

    enum RenderType {
        BODY,
        HEAD
    }

    final class Helper {
        private Helper() {}

        public static void rotateIfSneaking(EntityPlayer player) {
            if (player.isSneaking()) applySneakingRotation();
        }

        public static void applySneakingRotation() {
            GlStateManager.translate(0F, 0.2F, 0F);
            GlStateManager.rotate(90F / (float) Math.PI, 1.0F, 0.0F, 0.0F);
        }

        public static void translateToHeadLevel(EntityPlayer player) {
            GlStateManager.translate(0, -player.getDefaultEyeHeight(), 0);
            if (player.isSneaking()) {
                float pitch = Entity.mcc$get$rotationPitch(player) * (float) Math.PI / 180F;
                GlStateManager.translate(0.25F * (float) Math.sin(pitch), 0.25F * (float) Math.cos(pitch), 0F);
            }
        }

        public static void translateToFace() {
            GlStateManager.rotate(90F, 0F, 1F, 0F);
            GlStateManager.rotate(180F, 1F, 0F, 0F);
            GlStateManager.translate(0F, -4.35F, -1.27F);
        }

        public static void defaultTransforms() {
            GlStateManager.translate(0.0, 3.0, 1.0);
            GlStateManager.scale(0.55, 0.55, 0.55);
        }

        public static void translateToChest() {
            GlStateManager.rotate(180F, 1F, 0F, 0F);
            GlStateManager.translate(0F, -3.2F, -0.85F);
        }
    }
}
