package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "lava": a glowing ember that pops up out of lava, smoking, and shrinks away. */
public class EntityLavaFX extends EntityFX {
    private float lavaParticleScale;

    public EntityLavaFX(World world, double x, double y, double z) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        mcc$scaleMotion(0.800000011920929D, 0.800000011920929D, 0.800000011920929D);
        mcc$set$motionY(this, rand.nextFloat() * 0.4F + 0.05F);
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
        this.particleScale *= rand.nextFloat() * 2.0F + 0.2F;
        this.lavaParticleScale = this.particleScale;
        this.particleMaxAge = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
        mcc$set$noClip(this, false);
        setParticleTextureIndex(49);
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        int light = super.getBrightnessForRender(partialTicks);
        return 240 | (light >> 16 & 255) << 16;
    }

    @Override
    public float getBrightness(float partialTicks) {
        return 1.0F;
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        float f = (particleAge + partialTicks) / particleMaxAge;
        this.particleScale = lavaParticleScale * (1.0F - f * f);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        float f = (float) particleAge / (float) particleMaxAge;
        World world = mcc$world();
        if (world != null && rand.nextFloat() > f) {
            world.spawnParticle("smoke", mcc$x(), mcc$y(), mcc$z(), mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        }
        mcc$set$motionY(this, mcc$get$motionY(this) - 0.03D);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.9990000128746033D, 0.9990000128746033D, 0.9990000128746033D);
        mcc$groundFriction();
    }
}
