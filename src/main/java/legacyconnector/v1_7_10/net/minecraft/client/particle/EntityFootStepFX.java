package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** "footstep": a footprint flat on the ground that fades over ten seconds. */
public class EntityFootStepFX extends EntityFX {
    private static final ResourceLocation field_110126_a = new ResourceLocation("textures/particle/footprint.png");
    private int footstepAge;
    private int footstepMaxAge;
    private TextureManager currentFootSteps;

    public EntityFootStepFX(TextureManager textures, World world, double x, double y, double z) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.currentFootSteps = textures != null ? textures : TextureManager.INSTANCE;
        setVelocity(0.0D, 0.0D, 0.0D);
        this.footstepMaxAge = 200;
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        float f = (footstepAge + partialTicks) / footstepMaxAge;
        f *= f;
        float alpha = Math.min(1.0F, 2.0F - f * 2.0F) * 0.2F;
        GL11.glDisable(GL11.GL_LIGHTING);
        float half = 0.125F;
        float x = (float) (mcc$x() - interpPosX), y = (float) (mcc$y() - interpPosY), z = (float) (mcc$z() - interpPosZ);
        World world = mcc$world();
        float light = world == null ? 1.0F : world.getLightBrightness((int) Math.floor(mcc$x()), (int) Math.floor(mcc$y()), (int) Math.floor(mcc$z()));
        currentFootSteps.bindTexture(field_110126_a);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        t.startDrawingQuads();
        t.setColorRGBA_F(light, light, light, alpha);
        t.addVertexWithUV(x - half, y, z + half, 0.0D, 1.0D);
        t.addVertexWithUV(x + half, y, z + half, 1.0D, 1.0D);
        t.addVertexWithUV(x + half, y, z - half, 1.0D, 0.0D);
        t.addVertexWithUV(x - half, y, z - half, 0.0D, 0.0D);
        t.draw();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    @Override
    public void onUpdate() {
        ++footstepAge;
        if (footstepAge >= footstepMaxAge) setDead();
    }

    @Override
    public int getFXLayer() {
        return 3;
    }
}
