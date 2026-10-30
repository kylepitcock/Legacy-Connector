package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 cloud (and sneeze): a big pale puff growing in, drifting with its speed through the smoke frames; it rides up over a nearby player. */
public class ParticleCloud extends Particle {
    private final float oSize;

    protected ParticleCloud(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
        motionX *= 0.10000000149011612D;
        motionY *= 0.10000000149011612D;
        motionZ *= 0.10000000149011612D;
        motionX += xSpeedIn;
        motionY += ySpeedIn;
        motionZ += zSpeedIn;
        float f1 = 1.0F - (float) (Math.random() * 0.30000001192092896D);
        particleRed = f1;
        particleGreen = f1;
        particleBlue = f1;
        particleScale *= 0.75F;
        particleScale *= 2.5F;
        oSize = particleScale;
        particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.3D));
        particleMaxAge = (int) ((float) particleMaxAge * 2.5F);
        particleMaxAge = (int) Math.max((float) particleMaxAge, 1.0F);
        canCollide = false;
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
        setParticleTextureIndex(7 - particleAge * 8 / particleMaxAge);
        move(motionX, motionY, motionZ);
        motionX *= 0.9599999785423279D;
        motionY *= 0.9599999785423279D;
        motionZ *= 0.9599999785423279D;
        var level = world == null ? null : world.mcc$level();
        var player = level == null ? null : level.getNearestPlayer(posX, posY, posZ, 2.0D, false);
        if (player != null) {
            double minY = player.getBoundingBox().minY;
            if (posY > minY) {
                posY += (minY - posY) * 0.2D;
                motionY += (player.getDeltaMovement().y - motionY) * 0.2D;
                setPosition(posX, posY, posZ);
            }
        }
        if (onGround) {
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleCloud(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }
}
