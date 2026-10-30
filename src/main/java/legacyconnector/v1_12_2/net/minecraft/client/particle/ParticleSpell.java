package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import java.util.Random;

/** 1.12.2 potion swirl: rises and fades through the eight spell frames from its base index. */
public class ParticleSpell extends Particle {
    private static final Random RANDOM = new Random();
    private int baseSpellTextureIndex = 128;

    public ParticleSpell(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.5D - RANDOM.nextDouble(), ySpeedIn, 0.5D - RANDOM.nextDouble());
        motionY *= 0.20000000298023224D;
        if (xSpeedIn == 0.0D && zSpeedIn == 0.0D) {
            motionX *= 0.10000000149011612D;
            motionZ *= 0.10000000149011612D;
        }
        particleScale *= 0.75F;
        particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public boolean shouldDisableDepth() {
        return true;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setExpired();
        setParticleTextureIndex(baseSpellTextureIndex + (7 - particleAge * 8 / particleMaxAge));
        motionY += 0.004D;
        move(motionX, motionY, motionZ);
        if (posY == prevPosY) {
            motionX *= 1.1D;
            motionZ *= 1.1D;
        }
        motionX *= 0.9599999785423279D;
        motionY *= 0.9599999785423279D;
        motionZ *= 0.9599999785423279D;
        if (onGround) {
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
    }

    public void setBaseSpellTextureIndex(int baseSpellTextureIndexIn) {
        this.baseSpellTextureIndex = baseSpellTextureIndexIn;
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }

    /** Instant effects (instant health and damage): the sparkle frames. */
    public static class InstantFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            ParticleSpell particle = new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
            particle.setBaseSpellTextureIndex(144);
            return particle;
        }
    }

    /** Mob effect swirls: the speed is the colour. */
    public static class MobFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            Particle particle = new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
            particle.setRBGColorF((float) xSpeedIn, (float) ySpeedIn, (float) zSpeedIn);
            return particle;
        }
    }

    /** Beacon (ambient) effect swirls: the speed is the colour, mostly transparent. */
    public static class AmbientMobFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            Particle particle = new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
            particle.setAlphaF(0.15F);
            particle.setRBGColorF((float) xSpeedIn, (float) ySpeedIn, (float) zSpeedIn);
            return particle;
        }
    }

    /** Witch magic: the witch frames in purple shades. */
    public static class WitchFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            ParticleSpell particle = new ParticleSpell(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
            particle.setBaseSpellTextureIndex(144);
            float f = worldIn == null ? 0.85F : RANDOM.nextFloat() * 0.5F + 0.35F;
            particle.setRBGColorF(1.0F * f, 0.0F * f, 1.0F * f);
            return particle;
        }
    }
}
