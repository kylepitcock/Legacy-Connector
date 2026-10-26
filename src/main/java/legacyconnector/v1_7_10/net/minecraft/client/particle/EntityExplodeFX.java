package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "explode": a grey puff of a small explosion that rises through the smoke cells. */
public class EntityExplodeFX extends EntityFX {
    public EntityExplodeFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        setVelocity(vx + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D, vy + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D,
                vz + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D);
        this.particleRed = this.particleGreen = this.particleBlue = rand.nextFloat() * 0.3F + 0.7F;
        this.particleScale = rand.nextFloat() * rand.nextFloat() * 6.0F + 1.0F;
        this.particleMaxAge = (int) (16.0D / (rand.nextFloat() * 0.8D + 0.2D)) + 2;
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        setParticleTextureIndex(mcc$ageCell());
        mcc$set$motionY(this, mcc$get$motionY(this) + 0.004D);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.8999999761581421D, 0.8999999761581421D, 0.8999999761581421D);
        mcc$groundFriction();
    }
}
