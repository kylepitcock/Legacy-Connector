package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "note": a music note whose colour is picked by the first velocity argument (the pitch, 0-1). */
public class EntityNoteFX extends EntityFX {
    float noteParticleScale;

    public EntityNoteFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        this(world, x, y, z, vx, vy, vz, 2.0F);
    }

    public EntityNoteFX(World world, double x, double y, double z, double vx, double vy, double vz, float scale) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        mcc$scaleMotion(0.009999999776482582D, 0.009999999776482582D, 0.009999999776482582D);
        mcc$set$motionY(this, mcc$get$motionY(this) + 0.2D);
        float pitch = (float) vx;
        this.particleRed = (float) Math.sin((pitch + 0.0F) * (float) Math.PI * 2.0F) * 0.65F + 0.35F;
        this.particleGreen = (float) Math.sin((pitch + 0.33333334F) * (float) Math.PI * 2.0F) * 0.65F + 0.35F;
        this.particleBlue = (float) Math.sin((pitch + 0.6666667F) * (float) Math.PI * 2.0F) * 0.65F + 0.35F;
        this.particleScale *= 0.75F;
        this.particleScale *= scale;
        this.noteParticleScale = this.particleScale;
        this.particleMaxAge = 6;
        mcc$set$noClip(this, false);
        setParticleTextureIndex(64);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        this.particleScale = noteParticleScale * mcc$growIn(partialTicks);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        if (mcc$y() == mcc$get$prevPosY(this)) mcc$scaleMotion(1.1D, 1.0D, 1.1D);
        mcc$scaleMotion(0.6600000262260437D, 0.6600000262260437D, 0.6600000262260437D);
        mcc$groundFriction();
    }
}
