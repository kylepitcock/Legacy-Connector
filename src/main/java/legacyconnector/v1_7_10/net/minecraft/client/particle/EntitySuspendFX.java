package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "suspended": a still blue speck floating in water. */
public class EntitySuspendFX extends EntityFX {
    public EntitySuspendFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y - 0.125D, z, vx, vy, vz);
        this.particleRed = 0.4F;
        this.particleGreen = 0.4F;
        this.particleBlue = 0.7F;
        setParticleTextureIndex(0);
        setSize(0.01F, 0.01F);
        this.particleScale *= rand.nextFloat() * 0.6F + 0.2F;
        setVelocity(vx * 0.0D, vy * 0.0D, vz * 0.0D);
        this.particleMaxAge = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        World world = mcc$world();
        if (world != null && world.getBlock((int) Math.floor(mcc$x()), (int) Math.floor(mcc$y()), (int) Math.floor(mcc$z())).getMaterial() != Material.water) {
            setDead();
        }
        if (particleMaxAge-- <= 0) setDead();
    }
}
