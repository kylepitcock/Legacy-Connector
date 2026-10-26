package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A drifting smoke puff that grows in and shrinks through the eight smoke cells. */
public class EntitySmokeFX extends EntityFX {
    float smokeParticleScale;

    public EntitySmokeFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        this(world, x, y, z, vx, vy, vz, 1.0F);
    }

    public EntitySmokeFX(World world, double x, double y, double z, double vx, double vy, double vz, float scale) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        mcc$scaleMotion(0.1D, 0.1D, 0.1D);
        setVelocity(mcc$get$motionX(this) + vx, mcc$get$motionY(this) + vy, mcc$get$motionZ(this) + vz);
        float shade = (float) (Math.random() * 0.30000001192092896D);
        this.particleRed = shade;
        this.particleGreen = shade;
        this.particleBlue = shade;
        this.particleScale *= 0.75F;
        this.particleScale *= scale;
        this.smokeParticleScale = this.particleScale;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
        this.particleMaxAge = (int) (this.particleMaxAge * scale);
        mcc$set$noClip(this, false);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        this.particleScale = smokeParticleScale * mcc$growIn(partialTicks);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        setParticleTextureIndex(mcc$ageCell());
        mcc$set$motionY(this, mcc$get$motionY(this) + 0.004D);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        if (mcc$y() == mcc$get$prevPosY(this)) mcc$scaleMotion(1.1D, 1.0D, 1.1D);
        mcc$scaleMotion(0.9599999785423279D, 0.9599999785423279D, 0.9599999785423279D);
        mcc$groundFriction();
    }
}
