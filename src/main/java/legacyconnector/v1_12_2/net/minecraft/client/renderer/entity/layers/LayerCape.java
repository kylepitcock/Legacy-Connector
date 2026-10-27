package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers;

import legacyconnector.v1_12_2.net.minecraft.client.entity.AbstractClientPlayer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 cape layer: the player's cape (the modern skin's cape texture), hanging behind the body. */
public class LayerCape implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer playerRenderer;

    public LayerCape(RenderPlayer playerRendererIn) {
        this.playerRenderer = playerRendererIn;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (entitylivingbaseIn.isInvisible() || !(entitylivingbaseIn.mcc$entity instanceof net.minecraft.client.player.AbstractClientPlayer player)) return;
        var cape = player.getSkin().capeTexture();
        if (cape == null || !player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.CAPE)) return;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        playerRenderer.bindTexture(new ResourceLocation(cape.getNamespace(), cape.getPath()));
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.125F);
        double dx = player.xCloakO + (player.xCloak - player.xCloakO) * partialTicks - (player.xo + (player.getX() - player.xo) * partialTicks);
        double dy = player.yCloakO + (player.yCloak - player.yCloakO) * partialTicks - (player.yo + (player.getY() - player.yo) * partialTicks);
        double dz = player.zCloakO + (player.zCloak - player.zCloakO) * partialTicks - (player.zo + (player.getZ() - player.zo) * partialTicks);
        float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTicks;
        double sin = Math.sin(bodyYaw * 0.017453292F);
        double cos = -Math.cos(bodyYaw * 0.017453292F);
        float lift = (float) dy * 10.0F;
        lift = Math.max(-6.0F, Math.min(32.0F, lift));
        float swing = (float) (dx * sin + dz * cos) * 100.0F;
        float side = (float) (dx * cos - dz * sin) * 100.0F;
        if (swing < 0.0F) swing = 0.0F;
        float bob = player.oBob + (player.bob - player.oBob) * partialTicks;
        lift = lift + (float) Math.sin((player.walkDistO + (player.walkDist - player.walkDistO) * partialTicks) * 6.0F) * 32.0F * bob;
        if (entitylivingbaseIn.isSneaking()) lift += 25.0F;
        GlStateManager.rotate(6.0F + swing / 2.0F + lift, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(side / 2.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-side / 2.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        playerRenderer.getMainModel().renderCape(0.0625F);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
