package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 mob appearance (the elder guardian jump scare): a 30-tick, custom-drawn (layer 3) particle. The vanilla one becomes the modern
 * elder guardian appearance; mods' subclasses (their own creature's face) keep the timing and draw in their renderParticle.
 */
public class ParticleMobAppearance extends Particle {
    protected ParticleMobAppearance(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
        particleRed = 1.0F;
        particleGreen = 1.0F;
        particleBlue = 1.0F;
        motionX = 0.0D;
        motionY = 0.0D;
        motionZ = 0.0D;
        particleGravity = 0.0F;
        particleMaxAge = 30;
        if (getClass() == ParticleMobAppearance.class) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) level.addParticle(net.minecraft.core.particles.ParticleTypes.ELDER_GUARDIAN, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
            setExpired();
        }
    }

    @Override
    public int getFXLayer() {
        return 3;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY,
            float rotationXZ) {
        // the vanilla appearance is drawn by the modern particle
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleMobAppearance(worldIn, xCoordIn, yCoordIn, zCoordIn);
        }
    }
}
