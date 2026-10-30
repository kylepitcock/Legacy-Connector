package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 portal particle: flies from its start along its speed and curls back in, purple, shrinking and brightening as it ends. */
public class ParticlePortal extends Particle {
    private final float portalParticleScale;
    private final double portalPosX;
    private final double portalPosY;
    private final double portalPosZ;

    protected ParticlePortal(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        motionX = xSpeedIn;
        motionY = ySpeedIn;
        motionZ = zSpeedIn;
        posX = xCoordIn;
        posY = yCoordIn;
        posZ = zCoordIn;
        portalPosX = posX;
        portalPosY = posY;
        portalPosZ = posZ;
        float f = rand.nextFloat() * 0.6F + 0.4F;
        particleScale = rand.nextFloat() * 0.2F + 0.5F;
        portalParticleScale = particleScale;
        particleRed = f * 0.9F;
        particleGreen = f * 0.3F;
        particleBlue = f;
        particleMaxAge = (int) (Math.random() * 10.0D) + 40;
        setParticleTextureIndex((int) (Math.random() * 8.0D));
    }

    @Override
    public void move(double x, double y, double z) {
        setBoundingBox(getBoundingBox().offset(x, y, z));
        resetPositionToBB();
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float f = ((float) particleAge + partialTicks) / (float) particleMaxAge;
        f = 1.0F - f;
        f = f * f;
        f = 1.0F - f;
        particleScale = portalParticleScale * f;
        super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        int i = super.getBrightnessForRender(partialTick);
        float f = (float) particleAge / (float) particleMaxAge;
        f = f * f;
        f = f * f;
        int j = i & 255;
        int k = i >> 16 & 255;
        k += (int) (f * 15.0F * 16.0F);
        if (k > 240) k = 240;
        return j | k << 16;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        float f = (float) particleAge / (float) particleMaxAge;
        float f1 = -f + f * f * 2.0F;
        float f2 = 1.0F - f1;
        posX = portalPosX + motionX * (double) f2;
        posY = portalPosY + motionY * (double) f2 + (double) (1.0F - f);
        posZ = portalPosZ + motionZ * (double) f2;
        if (particleAge++ >= particleMaxAge) setExpired();
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticlePortal(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }
}
