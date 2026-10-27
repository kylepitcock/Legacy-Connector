package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 drip (water or lava under a block): hangs, falls, then splashes (water) or lies glowing (lava). */
public class ParticleDrip extends Particle {
    private final Material materialType;
    private int bobTimer;

    public ParticleDrip(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, Material material) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
        motionX = 0.0D;
        motionY = 0.0D;
        motionZ = 0.0D;
        if (material == Material.WATER) {
            particleRed = 0.0F;
            particleGreen = 0.0F;
            particleBlue = 1.0F;
        } else {
            particleRed = 1.0F;
            particleGreen = 0.0F;
            particleBlue = 0.0F;
        }
        setParticleTextureIndex(113);
        setSize(0.01F, 0.01F);
        particleGravity = 0.06F;
        materialType = material;
        bobTimer = 40;
        particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return materialType == Material.WATER ? super.getBrightnessForRender(partialTick) : 257;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (materialType == Material.WATER) {
            particleRed = 0.2F;
            particleGreen = 0.3F;
            particleBlue = 1.0F;
        } else {
            particleRed = 1.0F;
            particleGreen = 16.0F / (float) (40 - bobTimer + 16);
            particleBlue = 4.0F / (float) (40 - bobTimer + 8);
        }
        motionY -= (double) particleGravity;
        if (bobTimer-- > 0) {
            motionX *= 0.02D;
            motionY *= 0.02D;
            motionZ *= 0.02D;
            setParticleTextureIndex(113);
        } else {
            setParticleTextureIndex(112);
        }
        move(motionX, motionY, motionZ);
        motionX *= 0.9800000190734863D;
        motionY *= 0.9800000190734863D;
        motionZ *= 0.9800000190734863D;
        if (particleMaxAge-- <= 0) setExpired();
        if (onGround) {
            if (materialType == Material.WATER) {
                setExpired();
                org.kylepitcock.legacyconnector.v1_12_2.client.LegacyParticles1122.spawnVanilla(5, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
            } else {
                setParticleTextureIndex(114);
            }
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
        if (world == null) return;
        IBlockState state = world.getBlockState(new BlockPos(posX, posY, posZ));
        Material material = state.getMaterial();
        // inside a liquid or solid block: gone once below its top (1.12 also lowered the top by a liquid's level)
        if ((material.isLiquid() || material.isSolid()) && posY < Math.floor(posY) + 1.0D) setExpired();
    }
}
