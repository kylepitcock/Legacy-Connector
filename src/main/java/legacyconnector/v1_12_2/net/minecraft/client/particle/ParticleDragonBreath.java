package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 dragon breath: purple puffs drifting up and spreading, through three frames of the sheet. */
public class ParticleDragonBreath extends Particle {
    private final float oSize;
    private boolean hasHitGround;

    protected ParticleDragonBreath(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        this.motionX = xSpeedIn;
        this.motionY = ySpeedIn;
        this.motionZ = zSpeedIn;
        this.particleRed = 0.7176471F + this.rand.nextFloat() * (0.8745098F - 0.7176471F);
        this.particleGreen = 0.0F;
        this.particleBlue = 0.8235294F + this.rand.nextFloat() * (0.9764706F - 0.8235294F);
        this.particleScale *= 0.75F;
        this.oSize = this.particleScale;
        this.particleMaxAge = (int) (20.0D / ((double) this.rand.nextFloat() * 0.8D + 0.2D));
        this.hasHitGround = false;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setExpired();
            return;
        }
        setParticleTextureIndex(3 * particleAge / particleMaxAge + 5);
        if (onGround) {
            motionY = 0.0D;
            hasHitGround = true;
        }
        if (hasHitGround) motionY += 0.002D;
        move(motionX, motionY, motionZ);
        if (posY == prevPosY) {
            motionX *= 1.1D;
            motionZ *= 1.1D;
        }
        motionX *= 0.9599999785423279D;
        motionZ *= 0.9599999785423279D;
        if (hasHitGround) motionY *= 0.9599999785423279D;
    }

    @Override
    public void renderParticle(legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder buffer, legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn,
            float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float age = ((float) particleAge + partialTicks) / (float) particleMaxAge;
        particleScale = oSize * (1.0F - age * age * 0.5F);
        super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleDragonBreath(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        }
    }
}
