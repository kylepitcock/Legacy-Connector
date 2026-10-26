package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityFishHook;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's fishing hook renderer: the bobber as a camera-facing sprite from the particle sheet, and the line as a sagging
 * black strip from the angler's rod hand.
 */
public class RenderFish extends Render {
    private static final ResourceLocation field_110792_a = new ResourceLocation("textures/particle/particles.png");

    public void doRender(EntityFishHook hook, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        bindEntityTexture(hook);
        Tessellator t = Tessellator.instance;
        float u1 = 8 / 128.0F, u2 = 16 / 128.0F, v1 = 16 / 128.0F, v2 = 24 / 128.0F;
        RenderManager rm = renderManager != null ? renderManager : RenderManager.instance;
        GL11.glRotatef(180.0F - rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rm.playerViewX, 1.0F, 0.0F, 0.0F);
        t.startDrawingQuads();
        t.setNormal(0.0F, 1.0F, 0.0F);
        t.addVertexWithUV(-0.5F, -0.5F, 0.0D, u1, v2);
        t.addVertexWithUV(0.5F, -0.5F, 0.0D, u2, v2);
        t.addVertexWithUV(0.5F, 0.5F, 0.0D, u2, v1);
        t.addVertexWithUV(-0.5F, 0.5F, 0.0D, u1, v1);
        t.draw();
        GL11.glPopMatrix();
        var angler = hook.angler == null ? null : hook.angler.mcc$entity;
        if (!(angler instanceof net.minecraft.world.entity.LivingEntity living) || hook.mcc$entity == null) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        float swing = living.getAttackAnim(partialTicks);
        float swingSin = (float) Math.sin(Math.sqrt(swing) * Math.PI);
        float pitch = net.minecraft.util.Mth.lerp(partialTicks, living.xRotO, living.getXRot());
        float yawDeg = net.minecraft.util.Mth.lerp(partialTicks, living.yRotO, living.getYRot());
        var hand = new net.minecraft.world.phys.Vec3(-0.5D, 0.03D, 0.8D)
                .xRot(-pitch * (float) Math.PI / 180.0F)
                .yRot(-yawDeg * (float) Math.PI / 180.0F)
                .yRot(swingSin * 0.5F)
                .xRot(-swingSin * 0.7F);
        double ax = net.minecraft.util.Mth.lerp(partialTicks, living.xo, living.getX());
        double ay = net.minecraft.util.Mth.lerp(partialTicks, living.yo, living.getY()) + living.getEyeHeight();
        double az = net.minecraft.util.Mth.lerp(partialTicks, living.zo, living.getZ());
        double sx = ax + hand.x, sy = ay + hand.y, sz = az + hand.z;
        if (!mc.options.getCameraType().isFirstPerson() || living != mc.getCameraEntity()) {
            float body = net.minecraft.util.Mth.lerp(partialTicks, living.yBodyRotO, living.yBodyRot) * (float) Math.PI / 180.0F;
            double s = Math.sin(body), c = Math.cos(body);
            sx = ax - c * 0.35D - s * 0.85D;
            sy = ay - 0.45D;
            sz = az - s * 0.35D + c * 0.85D;
        }
        var m = hook.mcc$entity;
        double hx = net.minecraft.util.Mth.lerp(partialTicks, m.xo, m.getX());
        double hy = net.minecraft.util.Mth.lerp(partialTicks, m.yo, m.getY()) + 0.25D;
        double hz = net.minecraft.util.Mth.lerp(partialTicks, m.zo, m.getZ());
        double dx = (float) (sx - hx), dy = (float) (sy - hy), dz = (float) (sz - hz);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        t.startDrawing(GL11.GL_LINE_STRIP);
        t.setColorOpaque_I(0);
        for (int i = 0; i <= 16; ++i) {
            float f = i / 16.0F;
            t.addVertex(x + dx * f, y + dy * (f * f + f) * 0.5D + 0.25D, z + dz * f);
        }
        t.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityFishHook hook) doRender(hook, x, y, z, yaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntityFishHook hook) {
        return field_110792_a;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return field_110792_a;
    }
}
