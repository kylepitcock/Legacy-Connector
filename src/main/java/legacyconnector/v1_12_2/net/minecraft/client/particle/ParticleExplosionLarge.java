package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 large explosion (the 16-frame cloud, size given in the first speed): drawn with its own texture, so the vanilla one becomes the
 * modern explosion particle; mods' subclasses keep the timing and draw in their renderParticle.
 */
public class ParticleExplosionLarge extends Particle {
    private final float size;

    protected ParticleExplosionLarge(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double sizeIn, double p_i1213_10_, double p_i1213_12_) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
        this.particleMaxAge = 6 + this.rand.nextInt(4);
        float f = this.rand.nextFloat() * 0.6F + 0.4F;
        this.particleRed = f;
        this.particleGreen = f;
        this.particleBlue = f;
        this.size = 1.0F - (float) sizeIn * 0.5F;
        if (getClass() == ParticleExplosionLarge.class) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION, xCoordIn, yCoordIn, zCoordIn, sizeIn, 0.0D, 0.0D);
            setExpired();
        }
    }

    @Override
    public int getFXLayer() {
        return 3;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY,
            float rotationXZ) {
        // the vanilla cloud is drawn by the modern particle
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return 61680;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setExpired();
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleExplosionLarge(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }
}
