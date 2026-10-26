package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "snowshovel": a white puff thrown up that falls back down. */
public class EntitySnowShovelFX extends EntityFX {
    float snowDigParticleScale;

    public EntitySnowShovelFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        this(world, x, y, z, vx, vy, vz, 1.0F);
    }

    public EntitySnowShovelFX(World world, double x, double y, double z, double vx, double vy, double vz, float scale) {
        super(world, x, y, z, vx, vy, vz);
        mcc$scaleMotion(0.10000000149011612D, 0.10000000149011612D, 0.10000000149011612D);
        setVelocity(mcc$get$motionX(this) + vx, mcc$get$motionY(this) + vy, mcc$get$motionZ(this) + vz);
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F - (float) (Math.random() * 0.30000001192092896D);
        this.particleScale *= 0.75F;
        this.particleScale *= scale;
        this.snowDigParticleScale = this.particleScale;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
        this.particleMaxAge = (int) (this.particleMaxAge * scale);
        mcc$set$noClip(this, false);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        this.particleScale = snowDigParticleScale * mcc$growIn(partialTicks);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        setParticleTextureIndex(mcc$ageCell());
        mcc$set$motionY(this, mcc$get$motionY(this) - 0.03D);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.9900000095367432D, 0.9900000095367432D, 0.9900000095367432D);
        mcc$groundFriction();
    }
}
