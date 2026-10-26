package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A rain splash droplet (cells 19-22) that bounces off the ground and vanishes into liquids. */
public class EntityRainFX extends EntityFX {
    public EntityRainFX(World world, double x, double y, double z) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        setVelocity(mcc$get$motionX(this) * 0.30000001192092896D, (float) Math.random() * 0.2F + 0.1F, mcc$get$motionZ(this) * 0.30000001192092896D);
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;
        setParticleTextureIndex(19 + rand.nextInt(4));
        setSize(0.01F, 0.01F);
        this.particleGravity = 0.06F;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        mcc$set$motionY(this, mcc$get$motionY(this) - particleGravity);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.9800000190734863D, 0.9800000190734863D, 0.9800000190734863D);
        if (particleMaxAge-- <= 0) setDead();
        if (mcc$get$onGround(this)) {
            if (Math.random() < 0.5D) setDead();
            mcc$scaleMotion(0.699999988079071D, 1.0D, 0.699999988079071D);
        }
        World world = mcc$world();
        if (world == null) return;
        int bx = (int) Math.floor(mcc$x()), by = (int) Math.floor(mcc$y()), bz = (int) Math.floor(mcc$z());
        Material m = world.getBlock(bx, by, bz).getMaterial();
        if (m.isLiquid() || m.isSolid()) {
            double surface = Math.floor(mcc$y()) + 1 - BlockLiquid.getLiquidHeightPercent(world.getBlockMetadata(bx, by, bz));
            if (mcc$y() < surface) setDead();
        }
    }
}
