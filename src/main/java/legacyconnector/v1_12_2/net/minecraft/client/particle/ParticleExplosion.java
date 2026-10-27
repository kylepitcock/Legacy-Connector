package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 small explosion puff (poof): pale, large, slowing and fading through the smoke frames. */
public class ParticleExplosion extends Particle {
    public ParticleExplosion(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        motionX = xSpeedIn + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D;
        motionY = ySpeedIn + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D;
        motionZ = zSpeedIn + (Math.random() * 2.0D - 1.0D) * 0.05000000074505806D;
        float f = rand.nextFloat() * 0.3F + 0.7F;
        particleRed = f;
        particleGreen = f;
        particleBlue = f;
        particleScale = rand.nextFloat() * rand.nextFloat() * 6.0F + 1.0F;
        particleMaxAge = (int) (16.0D / ((double) rand.nextFloat() * 0.8D + 0.2D)) + 2;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setExpired();
        setParticleTextureIndex(7 - particleAge * 8 / particleMaxAge);
        motionY += 0.004D;
        move(motionX, motionY, motionZ);
        motionX *= 0.8999999761581421D;
        motionY *= 0.8999999761581421D;
        motionZ *= 0.8999999761581421D;
        if (onGround) {
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
    }
}
