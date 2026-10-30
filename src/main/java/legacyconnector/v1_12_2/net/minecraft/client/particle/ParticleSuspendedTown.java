package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 town aura / mycelium spore, and the happy-villager sparkle: a tiny speck drifting slowly without gravity or collision. */
public class ParticleSuspendedTown extends Particle {
    public ParticleSuspendedTown(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        float f = rand.nextFloat() * 0.1F + 0.2F;
        particleRed = f;
        particleGreen = f;
        particleBlue = f;
        setParticleTextureIndex(0);
        setSize(0.02F, 0.02F);
        particleScale *= rand.nextFloat() * 0.6F + 0.5F;
        motionX *= 0.019999999552965164D;
        motionY *= 0.019999999552965164D;
        motionZ *= 0.019999999552965164D;
        particleMaxAge = (int) (20.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public void move(double x, double y, double z) {
        setBoundingBox(getBoundingBox().offset(x, y, z));
        resetPositionToBB();
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        move(motionX, motionY, motionZ);
        motionX *= 0.99D;
        motionY *= 0.99D;
        motionZ *= 0.99D;
        if (particleMaxAge-- <= 0) setExpired();
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleSuspendedTown(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }

    public static class HappyVillagerFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            Particle particle = new ParticleSuspendedTown(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
            particle.setParticleTextureIndex(82);
            particle.setRBGColorF(1.0F, 1.0F, 1.0F);
            return particle;
        }
    }
}
