package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Potion swirls ("spell", "mobSpell"; base cell 128) and the instant/witch sparkles (base cell 144). */
public class EntitySpellParticleFX extends EntityFX {
    private int baseSpellTextureIndex = 128;

    public EntitySpellParticleFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        mcc$set$motionY(this, mcc$get$motionY(this) * 0.20000000298023224D);
        if (vx == 0.0D && vz == 0.0D) mcc$scaleMotion(0.10000000149011612D, 1.0D, 0.10000000149011612D);
        this.particleScale *= 0.75F;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
        mcc$set$noClip(this, false);
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        float scale = this.particleScale;
        this.particleScale = scale * mcc$growIn(partialTicks);
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
        this.particleScale = scale;
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (particleAge++ >= particleMaxAge) setDead();
        setParticleTextureIndex(baseSpellTextureIndex + mcc$ageCell());
        mcc$set$motionY(this, mcc$get$motionY(this) + 0.004D);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        if (mcc$y() == mcc$get$prevPosY(this)) mcc$scaleMotion(1.1D, 1.0D, 1.1D);
        mcc$scaleMotion(0.9599999785423279D, 0.9599999785423279D, 0.9599999785423279D);
        mcc$groundFriction();
    }

    public void setBaseSpellTextureIndex(int index) {
        this.baseSpellTextureIndex = index;
    }
}
