package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "bubble": rises through water and pops when it leaves it. */
public class EntityBubbleFX extends EntityFX {
    public EntityBubbleFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;
        setParticleTextureIndex(32);
        setSize(0.02F, 0.02F);
        this.particleScale *= rand.nextFloat() * 0.6F + 0.2F;
        setVelocity(vx * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D,
                vy * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D,
                vz * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D);
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        mcc$set$motionY(this, mcc$get$motionY(this) + 0.002D);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.8500000238418579D, 0.8500000238418579D, 0.8500000238418579D);
        World world = mcc$world();
        if (world != null && world.getBlock((int) Math.floor(mcc$x()), (int) Math.floor(mcc$y()), (int) Math.floor(mcc$z())).getMaterial() != Material.water) {
            setDead();
        }
        if (particleMaxAge-- <= 0) setDead();
    }
}
