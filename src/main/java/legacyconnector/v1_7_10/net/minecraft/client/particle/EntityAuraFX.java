package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "townaura" / "happyVillager": a tiny slow speck that drifts through blocks. */
public class EntityAuraFX extends EntityFX {
    public EntityAuraFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        float shade = rand.nextFloat() * 0.1F + 0.2F;
        this.particleRed = shade;
        this.particleGreen = shade;
        this.particleBlue = shade;
        setParticleTextureIndex(0);
        setSize(0.02F, 0.02F);
        this.particleScale *= rand.nextFloat() * 0.6F + 0.5F;
        mcc$scaleMotion(0.019999999552965164D, 0.019999999552965164D, 0.019999999552965164D);
        this.particleMaxAge = (int) (20.0D / (Math.random() * 0.8D + 0.2D));
        mcc$set$noClip(this, true);
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.99D, 0.99D, 0.99D);
        if (particleMaxAge-- <= 0) setDead();
    }
}
