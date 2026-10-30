package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 water splash: a rain droplet with lighter gravity, thrown with the given horizontal speed. */
public class ParticleSplash extends ParticleRain {
    public ParticleSplash(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn);
        particleGravity = 0.04F;
        nextTextureIndexX();
        if (ySpeedIn == 0.0D && (xSpeedIn != 0.0D || zSpeedIn != 0.0D)) {
            motionX = xSpeedIn;
            motionY = 0.1D;
            motionZ = zSpeedIn;
        }
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleSplash(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }
}
