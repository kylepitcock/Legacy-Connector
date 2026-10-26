package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A flame particle: a short-lived glowing ember that shrinks as it burns out. */
public class EntityFlameFX extends EntityFX {
    private float flameScale;

    public EntityFlameFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        setVelocity(mcc$get$motionX(this) * 0.009999999776482582D + vx, mcc$get$motionY(this) * 0.009999999776482582D + vy,
                mcc$get$motionZ(this) * 0.009999999776482582D + vz);
        this.flameScale = this.particleScale;
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
        mcc$set$noClip(this, true);
        setParticleTextureIndex(48);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        float f = (particleAge + partialTicks) / particleMaxAge;
        this.particleScale = flameScale * (1.0F - f * f * 0.5F);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        float f = Math.max(0.0F, Math.min(1.0F, (particleAge + partialTicks) / particleMaxAge));
        int light = super.getBrightnessForRender(partialTicks);
        int block = (light & 255) + (int) (f * 15.0F * 16.0F);
        return Math.min(block, 240) | (light >> 16 & 255) << 16;
    }

    @Override
    public float getBrightness(float partialTicks) {
        float f = Math.max(0.0F, Math.min(1.0F, (particleAge + partialTicks) / particleMaxAge));
        return super.getBrightness(partialTicks) * f + (1.0F - f);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.9599999785423279D, 0.9599999785423279D, 0.9599999785423279D);
        mcc$groundFriction();
    }
}
