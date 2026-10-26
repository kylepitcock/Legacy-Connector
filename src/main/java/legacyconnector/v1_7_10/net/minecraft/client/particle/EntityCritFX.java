package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "crit" (cell 65) and, one cell on, "magicCrit": a falling star that turns orange. */
public class EntityCritFX extends EntityFX {
    float initialParticleScale;

    public EntityCritFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        this(world, x, y, z, vx, vy, vz, 1.0F);
    }

    public EntityCritFX(World world, double x, double y, double z, double vx, double vy, double vz, float scale) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        mcc$scaleMotion(0.10000000149011612D, 0.10000000149011612D, 0.10000000149011612D);
        setVelocity(mcc$get$motionX(this) + vx * 0.4D, mcc$get$motionY(this) + vy * 0.4D, mcc$get$motionZ(this) + vz * 0.4D);
        this.particleRed = this.particleGreen = this.particleBlue = (float) (Math.random() * 0.30000001192092896D + 0.6000000238418579D);
        this.particleScale *= 0.75F;
        this.particleScale *= scale;
        this.initialParticleScale = this.particleScale;
        this.particleMaxAge = (int) (6.0D / (Math.random() * 0.8D + 0.6D));
        this.particleMaxAge = (int) (this.particleMaxAge * scale);
        mcc$set$noClip(this, false);
        setParticleTextureIndex(65);
        onUpdate();
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        this.particleScale = initialParticleScale * mcc$growIn(partialTicks);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        this.particleGreen = (float) (particleGreen * 0.96D);
        this.particleBlue = (float) (particleBlue * 0.9D);
        mcc$scaleMotion(0.699999988079071D, 0.699999988079071D, 0.699999988079071D);
        mcc$set$motionY(this, mcc$get$motionY(this) - 0.019999999552965164D);
        mcc$groundFriction();
    }
}
