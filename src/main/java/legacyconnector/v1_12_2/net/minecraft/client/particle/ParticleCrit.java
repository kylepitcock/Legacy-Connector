package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 critical-hit star: flies out, falls and turns yellow-red as it fades. */
public class ParticleCrit extends Particle {
    private final float oSize;

    public ParticleCrit(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        this(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, 1.0F);
    }

    public ParticleCrit(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, float scale) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
        motionX *= 0.10000000149011612D;
        motionY *= 0.10000000149011612D;
        motionZ *= 0.10000000149011612D;
        motionX += xSpeedIn * 0.4D;
        motionY += ySpeedIn * 0.4D;
        motionZ += zSpeedIn * 0.4D;
        float f = (float) (Math.random() * 0.30000001192092896D + 0.6000000238418579D);
        particleRed = f;
        particleGreen = f;
        particleBlue = f;
        particleScale *= 0.75F;
        particleScale *= scale;
        oSize = particleScale;
        particleMaxAge = (int) (6.0D / (Math.random() * 0.8D + 0.6D));
        particleMaxAge = (int) ((float) particleMaxAge * scale);
        canCollide = false;
        setParticleTextureIndex(65);
        onUpdate();
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float f = ((float) particleAge + partialTicks) / (float) particleMaxAge * 32.0F;
        f = Math.max(0.0F, Math.min(1.0F, f));
        particleScale = oSize * f;
        super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setExpired();
        move(motionX, motionY, motionZ);
        particleGreen = (float) ((double) particleGreen * 0.96D);
        particleBlue = (float) ((double) particleBlue * 0.9D);
        motionX *= 0.699999988079071D;
        motionY *= 0.699999988079071D;
        motionZ *= 0.699999988079071D;
        motionY -= 0.019999999552965164D;
        if (onGround) {
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleCrit(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }

    public static class MagicFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            Particle particle = new ParticleCrit(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
            particle.setRBGColorF(particle.getRedColorF() * 0.3F, particle.getGreenColorF() * 0.8F, particle.getBlueColorF());
            particle.nextTextureIndexX();
            return particle;
        }
    }

    public static class DamageIndicatorFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            Particle particle = new ParticleCrit(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn + 1.0D, zSpeedIn, 1.0F);
            particle.setMaxAge(20);
            particle.setParticleTextureIndex(67);
            return particle;
        }
    }
}
