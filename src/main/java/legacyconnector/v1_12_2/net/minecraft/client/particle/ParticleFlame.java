package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 flame (torches, furnaces): shrinks as it ages and glows brighter, drifting without gravity. */
public class ParticleFlame extends Particle {
    private final float flameScale;

    public ParticleFlame(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        motionX = motionX * 0.009999999776482582D + xSpeedIn;
        motionY = motionY * 0.009999999776482582D + ySpeedIn;
        motionZ = motionZ * 0.009999999776482582D + zSpeedIn;
        posX += (double) ((rand.nextFloat() - rand.nextFloat()) * 0.05F);
        posY += (double) ((rand.nextFloat() - rand.nextFloat()) * 0.05F);
        posZ += (double) ((rand.nextFloat() - rand.nextFloat()) * 0.05F);
        flameScale = particleScale;
        particleRed = 1.0F;
        particleGreen = 1.0F;
        particleBlue = 1.0F;
        particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
        setParticleTextureIndex(48);
    }

    @Override
    public void move(double x, double y, double z) {
        setBoundingBox(getBoundingBox().offset(x, y, z));
        resetPositionToBB();
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float f = ((float) particleAge + partialTicks) / (float) particleMaxAge;
        particleScale = flameScale * (1.0F - f * f * 0.5F);
        super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        float f = ((float) particleAge + partialTick) / (float) particleMaxAge;
        f = Math.max(0.0F, Math.min(1.0F, f));
        int i = super.getBrightnessForRender(partialTick);
        int block = i & 255;
        int sky = i >> 16 & 255;
        block = Math.min(240, block + (int) (f * 15.0F * 16.0F));
        return block | sky << 16;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setExpired();
        move(motionX, motionY, motionZ);
        motionX *= 0.9599999785423279D;
        motionY *= 0.9599999785423279D;
        motionZ *= 0.9599999785423279D;
        if (onGround) {
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
    }
}
