package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "dripWater" / "dripLava": a drop that hangs under a block, falls, and splashes or lands. */
public class EntityDropParticleFX extends EntityFX {
    private Material materialType;
    private int bobTimer;

    public EntityDropParticleFX(World world, double x, double y, double z, Material material) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        setVelocity(0.0D, 0.0D, 0.0D);
        if (material == Material.water) {
            this.particleRed = 0.0F;
            this.particleGreen = 0.0F;
            this.particleBlue = 1.0F;
        } else {
            this.particleRed = 1.0F;
            this.particleGreen = 0.0F;
            this.particleBlue = 0.0F;
        }
        setParticleTextureIndex(113);
        setSize(0.01F, 0.01F);
        this.particleGravity = 0.06F;
        this.materialType = material;
        this.bobTimer = 40;
        this.particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        return materialType == Material.water ? super.getBrightnessForRender(partialTicks) : 257;
    }

    @Override
    public float getBrightness(float partialTicks) {
        return materialType == Material.water ? super.getBrightness(partialTicks) : 1.0F;
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        if (materialType == Material.water) {
            this.particleRed = 0.2F;
            this.particleGreen = 0.3F;
            this.particleBlue = 1.0F;
        } else {
            this.particleRed = 1.0F;
            this.particleGreen = 16.0F / (40 - bobTimer + 16);
            this.particleBlue = 4.0F / (40 - bobTimer + 8);
        }
        mcc$set$motionY(this, mcc$get$motionY(this) - particleGravity);
        if (bobTimer-- > 0) {
            mcc$scaleMotion(0.02D, 0.02D, 0.02D);
            setParticleTextureIndex(113);
        } else {
            setParticleTextureIndex(112);
        }
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        mcc$scaleMotion(0.9800000190734863D, 0.9800000190734863D, 0.9800000190734863D);
        if (particleMaxAge-- <= 0) setDead();
        World world = mcc$world();
        if (mcc$get$onGround(this)) {
            if (materialType == Material.water) {
                setDead();
                if (world != null) world.spawnParticle("splash", mcc$x(), mcc$y(), mcc$z(), 0.0D, 0.0D, 0.0D);
            } else {
                setParticleTextureIndex(114);
            }
            mcc$scaleMotion(0.699999988079071D, 1.0D, 0.699999988079071D);
        }
        if (world == null) return;
        int bx = (int) Math.floor(mcc$x()), by = (int) Math.floor(mcc$y()), bz = (int) Math.floor(mcc$z());
        Material m = world.getBlock(bx, by, bz).getMaterial();
        if (m.isLiquid() || m.isSolid()) {
            double surface = Math.floor(mcc$y()) + 1 - BlockLiquid.getLiquidHeightPercent(world.getBlockMetadata(bx, by, bz));
            if (mcc$y() < surface) setDead();
        }
    }
}
