package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 end rod sparkle: a slowly rising, full-bright flicker of the sheet's twinkle frames fading to pale yellow. */
public class ParticleEndRod extends ParticleSimpleAnimated {
    public ParticleEndRod(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 176, 8, -5.0E-4F);
        this.motionX = xSpeedIn;
        this.motionY = ySpeedIn;
        this.motionZ = zSpeedIn;
        this.particleScale *= 0.75F;
        this.particleMaxAge = 60 + this.rand.nextInt(12);
        setColorFade(15916745);
    }

    @Override
    public void move(double x, double y, double z) {
        // passes through blocks: only the position changes
        setPosition(posX + x, posY + y, posZ + z);
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleEndRod(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }
}
