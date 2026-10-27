package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 particle factory, registered by id with {@code ParticleManager.registerParticle}. */
public interface IParticleFactory {
    Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn,
            int... p_178902_15_);
}
