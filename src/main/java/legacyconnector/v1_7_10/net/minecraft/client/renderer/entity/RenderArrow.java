package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityArrow;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** 1.7.10's arrow renderer: four crossed planes and two end caps from the arrow texture, shaking after a hit. */
public class RenderArrow extends Render {
    static final ResourceLocation arrowTextures = new ResourceLocation("textures/entity/arrow.png");

    public void doRender(EntityArrow arrow, double x, double y, double z, float yaw, float partialTicks) {
        bindEntityTexture(arrow);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        float prevYaw = Entity.mcc$get$prevRotationYaw(arrow), prevPitch = Entity.mcc$get$prevRotationPitch(arrow);
        GL11.glRotatef(prevYaw + (Entity.mcc$get$rotationYaw(arrow) - prevYaw) * partialTicks - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(prevPitch + (Entity.mcc$get$rotationPitch(arrow) - prevPitch) * partialTicks, 0.0F, 0.0F, 1.0F);
        float shake = arrow.arrowShake - partialTicks;
        if (shake > 0.0F) GL11.glRotatef(-(float) Math.sin(shake * 3.0F) * shake, 0.0F, 0.0F, 1.0F);
        mcc$drawArrow();
        GL11.glPopMatrix();
    }

    /** The arrow geometry along +x in the current pose and texture (also the arrows stuck in living entities). */
    static void mcc$drawArrow() {
        Tessellator t = Tessellator.instance;
        float shaftU1 = 0.0F, shaftU2 = 0.5F, shaftV1 = 0.0F / 32.0F, shaftV2 = 5.0F / 32.0F;
        float capU1 = 0.0F, capU2 = 0.15625F, capV1 = 5.0F / 32.0F, capV2 = 10.0F / 32.0F;
        float scale = 0.05625F;
        GL11.glPushMatrix();
        GL11.glRotatef(45.0F, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(-4.0F, 0.0F, 0.0F);
        t.startDrawingQuads();
        t.setNormal(1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(-7.0D, -2.0D, -2.0D, capU1, capV1);
        t.addVertexWithUV(-7.0D, -2.0D, 2.0D, capU2, capV1);
        t.addVertexWithUV(-7.0D, 2.0D, 2.0D, capU2, capV2);
        t.addVertexWithUV(-7.0D, 2.0D, -2.0D, capU1, capV2);
        t.draw();
        t.startDrawingQuads();
        t.setNormal(-1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(-7.0D, 2.0D, -2.0D, capU1, capV1);
        t.addVertexWithUV(-7.0D, 2.0D, 2.0D, capU2, capV1);
        t.addVertexWithUV(-7.0D, -2.0D, 2.0D, capU2, capV2);
        t.addVertexWithUV(-7.0D, -2.0D, -2.0D, capU1, capV2);
        t.draw();
        for (int i = 0; i < 4; ++i) {
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            t.startDrawingQuads();
            t.setNormal(0.0F, 0.0F, 1.0F);
            t.addVertexWithUV(-8.0D, -2.0D, 0.0D, shaftU1, shaftV1);
            t.addVertexWithUV(8.0D, -2.0D, 0.0D, shaftU2, shaftV1);
            t.addVertexWithUV(8.0D, 2.0D, 0.0D, shaftU2, shaftV2);
            t.addVertexWithUV(-8.0D, 2.0D, 0.0D, shaftU1, shaftV2);
            t.draw();
        }
        GL11.glPopMatrix();
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityArrow arrow) doRender(arrow, x, y, z, yaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntityArrow arrow) {
        return arrowTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityArrow arrow ? getEntityTexture(arrow) : arrowTextures;
    }
}
