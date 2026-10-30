package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 huge explosion: an invisible emitter that spawns six large explosion puffs around itself for eight ticks. */
public class ParticleExplosionHuge extends Particle {
    private int timeSinceStart;
    private final int maximumTime = 8;

    public ParticleExplosionHuge(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double p_i1214_8_, double p_i1214_10_, double p_i1214_12_) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
    }

    @Override
    public void onUpdate() {
        for (int i = 0; i < 6; ++i) {
            double d0 = posX + (rand.nextDouble() - rand.nextDouble()) * 4.0D;
            double d1 = posY + (rand.nextDouble() - rand.nextDouble()) * 4.0D;
            double d2 = posZ + (rand.nextDouble() - rand.nextDouble()) * 4.0D;
            if (world != null) world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, d0, d1, d2, (double) ((float) timeSinceStart / (float) maximumTime), 0.0D, 0.0D);
        }
        ++timeSinceStart;
        if (timeSinceStart >= maximumTime) setExpired();
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleExplosionHuge(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }
}
