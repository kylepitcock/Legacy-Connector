package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;
import legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 fishing hook renderer: the bobber as a camera-facing sprite from the particle sheet and the line as a sagging black strip from
 * the angler's rod hand (the hand position is read from the angler's modern entity).
 */
public class RenderFish extends Render<EntityFishHook> {
    private static final ResourceLocation FISH_PARTICLES = new ResourceLocation("textures/particle/particles.png");

    public RenderFish(RenderManager renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void doRender(EntityFishHook entity, double x, double y, double z, float entityYaw, float partialTicks) {
        var anglerView = entity.getAngler();
        if (anglerView == null || renderOutlines) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        bindEntityTexture(entity);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.rotate(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        boolean frontal = renderManager.options != null && GameSettings.mcc$get$thirdPersonView(renderManager.options) == 2;
        GlStateManager.rotate((float) (frontal ? -1 : 1) * -renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
        buffer.pos(-0.5D, -0.5D, 0.0D).tex(0.0625D, 0.1875D).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(0.5D, -0.5D, 0.0D).tex(0.125D, 0.1875D).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(0.5D, 0.5D, 0.0D).tex(0.125D, 0.125D).normal(0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(-0.5D, 0.5D, 0.0D).tex(0.0625D, 0.125D).normal(0.0F, 1.0F, 0.0F).endVertex();
        tessellator.draw();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        if (anglerView.mcc$entity instanceof net.minecraft.world.entity.player.Player angler && entity.mcc$entity != null) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int side = angler.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
            if (!(angler.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem)) side = -side;
            float swing = angler.getAttackAnim(partialTicks);
            float swingSin = net.minecraft.util.Mth.sin(net.minecraft.util.Mth.sqrt(swing) * (float) Math.PI);
            double ax = net.minecraft.util.Mth.lerp(partialTicks, angler.xo, angler.getX());
            double ay = net.minecraft.util.Mth.lerp(partialTicks, angler.yo, angler.getY());
            double az = net.minecraft.util.Mth.lerp(partialTicks, angler.zo, angler.getZ());
            double sx, sy, sz, eye;
            if (mc.options.getCameraType().isFirstPerson() && angler == mc.player) {
                float fov = mc.options.fov().get() / 100.0F;
                var hand = new net.minecraft.world.phys.Vec3(side * -0.36D * fov, -0.045D * fov, 0.4D)
                        .xRot(-net.minecraft.util.Mth.lerp(partialTicks, angler.xRotO, angler.getXRot()) * 0.017453292F)
                        .yRot(-net.minecraft.util.Mth.lerp(partialTicks, angler.yRotO, angler.getYRot()) * 0.017453292F)
                        .yRot(swingSin * 0.5F)
                        .xRot(-swingSin * 0.7F);
                sx = ax + hand.x;
                sy = ay + hand.y;
                sz = az + hand.z;
                eye = angler.getEyeHeight();
            } else {
                float body = net.minecraft.util.Mth.lerp(partialTicks, angler.yBodyRotO, angler.yBodyRot) * 0.017453292F;
                double s = net.minecraft.util.Mth.sin(body), c = net.minecraft.util.Mth.cos(body), d2 = side * 0.35D;
                sx = ax - c * d2 - s * 0.8D;
                sy = ay + angler.getEyeHeight() - 0.45D;
                sz = az - s * d2 + c * 0.8D;
                eye = angler.isCrouching() ? -0.1875D : 0.0D;
            }
            var m = entity.mcc$entity;
            double hx = net.minecraft.util.Mth.lerp(partialTicks, m.xo, m.getX());
            double hy = net.minecraft.util.Mth.lerp(partialTicks, m.yo, m.getY()) + 0.25D;
            double hz = net.minecraft.util.Mth.lerp(partialTicks, m.zo, m.getZ());
            double dx = (float) (sx - hx), dy = (double) (float) (sy - hy) + eye, dz = (float) (sz - hz);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 16; i++) {
                float f = (float) i / 16.0F;
                buffer.pos(x + dx * f, y + dy * (f * f + f) * 0.5D + 0.25D, z + dz * f).color(0, 0, 0, 255).endVertex();
            }
            tessellator.draw();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityFishHook entity) {
        return FISH_PARTICLES;
    }
}
