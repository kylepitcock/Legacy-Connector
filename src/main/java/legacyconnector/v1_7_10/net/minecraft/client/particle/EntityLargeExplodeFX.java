package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** "largeexplode": one burst of the 4x4-frame explosion animation (textures/entity/explosion.png, see LegacyParticleSheets). */
public class EntityLargeExplodeFX extends EntityFX {
    private static final ResourceLocation field_110127_a = new ResourceLocation("textures/entity/explosion.png");
    private int field_70581_a;
    private int field_70584_aq;
    private TextureManager theRenderEngine;
    private float field_70582_as;

    public EntityLargeExplodeFX(TextureManager textures, World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.theRenderEngine = textures != null ? textures : TextureManager.INSTANCE;
        this.field_70584_aq = 6 + rand.nextInt(4);
        this.particleRed = this.particleGreen = this.particleBlue = rand.nextFloat() * 0.6F + 0.4F;
        this.field_70582_as = 1.0F - (float) vx * 0.5F;
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        int frame = (int) ((field_70581_a + partialTicks) * 15.0F / field_70584_aq);
        if (frame > 15) return;
        theRenderEngine.bindTexture(field_110127_a);
        float u0 = frame % 4 / 4.0F;
        float u1 = u0 + 0.24975F;
        float v0 = frame / 4 / 4.0F;
        float v1 = v0 + 0.24975F;
        float size = 2.0F * field_70582_as;
        float x = mcc$renderX(partialTicks), y = mcc$renderY(partialTicks), z = mcc$renderZ(partialTicks);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        t.startDrawingQuads();
        t.setColorRGBA_F(particleRed, particleGreen, particleBlue, 1.0F);
        t.setNormal(0.0F, 1.0F, 0.0F);
        t.setBrightness(240);
        t.addVertexWithUV(x - rx * size - ryz * size, y - rxz * size, z - rz * size - rxy * size, u1, v1);
        t.addVertexWithUV(x - rx * size + ryz * size, y + rxz * size, z - rz * size + rxy * size, u1, v0);
        t.addVertexWithUV(x + rx * size + ryz * size, y + rxz * size, z + rz * size + rxy * size, u0, v0);
        t.addVertexWithUV(x + rx * size - ryz * size, y - rxz * size, z + rz * size - rxy * size, u0, v1);
        t.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        return 61680;
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        ++field_70581_a;
        if (field_70581_a >= field_70584_aq) setDead();
    }

    @Override
    public int getFXLayer() {
        return 3;
    }
}
