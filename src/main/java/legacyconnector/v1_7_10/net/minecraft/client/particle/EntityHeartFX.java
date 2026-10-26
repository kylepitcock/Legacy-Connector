package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "heart" (cell 80) and "angryVillager" (cell 81): floats up briefly. */
public class EntityHeartFX extends EntityFX {
    float particleScaleOverTime;

    public EntityHeartFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        this(world, x, y, z, vx, vy, vz, 2.0F);
    }

    public EntityHeartFX(World world, double x, double y, double z, double vx, double vy, double vz, float scale) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        mcc$scaleMotion(0.009999999776482582D, 0.009999999776482582D, 0.009999999776482582D);
        mcc$set$motionY(this, mcc$get$motionY(this) + 0.1D);
        this.particleScale *= 0.75F;
        this.particleScale *= scale;
        this.particleScaleOverTime = this.particleScale;
        this.particleMaxAge = 16;
        mcc$set$noClip(this, false);
        setParticleTextureIndex(80);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        this.particleScale = particleScaleOverTime * mcc$growIn(partialTicks);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        if (mcc$y() == mcc$get$prevPosY(this)) mcc$scaleMotion(1.1D, 1.0D, 1.1D);
        mcc$scaleMotion(0.8600000143051147D, 0.8600000143051147D, 0.8600000143051147D);
        mcc$groundFriction();
    }
}
