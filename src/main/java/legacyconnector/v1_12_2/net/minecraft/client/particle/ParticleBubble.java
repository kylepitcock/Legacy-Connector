package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** 1.12.2 underwater bubble: rises slowly and pops when it leaves water. */
public class ParticleBubble extends Particle {
    public ParticleBubble(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        particleRed = 1.0F;
        particleGreen = 1.0F;
        particleBlue = 1.0F;
        setParticleTextureIndex(32);
        setSize(0.02F, 0.02F);
        particleScale *= rand.nextFloat() * 0.6F + 0.2F;
        motionX = xSpeedIn * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D;
        motionY = ySpeedIn * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D;
        motionZ = zSpeedIn * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D;
        particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        motionY += 0.002D;
        move(motionX, motionY, motionZ);
        motionX *= 0.8500000238418579D;
        motionY *= 0.8500000238418579D;
        motionZ *= 0.8500000238418579D;
        if (world == null || world.getBlockState(new BlockPos(posX, posY, posZ)).getMaterial() != Material.WATER) setExpired();
        if (particleMaxAge-- <= 0) setExpired();
    }
}
