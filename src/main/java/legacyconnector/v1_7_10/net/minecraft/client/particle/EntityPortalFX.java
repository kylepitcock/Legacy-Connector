package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "portal": a purple speck that flies from its offset back towards the start point. */
public class EntityPortalFX extends EntityFX {
    private float portalParticleScale;
    private double portalPosX;
    private double portalPosY;
    private double portalPosZ;

    public EntityPortalFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        setVelocity(vx, vy, vz);
        this.portalPosX = x;
        this.portalPosY = y;
        this.portalPosZ = z;
        setPosition(x, y, z);
        float shade = rand.nextFloat() * 0.6F + 0.4F;
        this.portalParticleScale = this.particleScale = rand.nextFloat() * 0.2F + 0.5F;
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F * shade;
        this.particleGreen *= 0.3F;
        this.particleRed *= 0.9F;
        this.particleMaxAge = (int) (Math.random() * 10.0D) + 40;
        mcc$set$noClip(this, true);
        setParticleTextureIndex((int) (Math.random() * 8.0D));
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        float f = (particleAge + partialTicks) / particleMaxAge;
        f = 1.0F - f;
        f *= f;
        f = 1.0F - f;
        this.particleScale = portalParticleScale * f;
        super.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        int light = super.getBrightnessForRender(partialTicks);
        float f = (float) particleAge / (float) particleMaxAge;
        f *= f;
        f *= f;
        int block = (light & 255) + (int) (f * 15.0F * 16.0F);
        return Math.min(block, 240) | (light >> 16 & 255) << 16;
    }

    @Override
    public float getBrightness(float partialTicks) {
        float base = super.getBrightness(partialTicks);
        float f = (float) particleAge / (float) particleMaxAge;
        f = f * f * f * f;
        return base * (1.0F - f) + f;
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        float age = (float) particleAge / (float) particleMaxAge;
        float f = -age + age * age * 2.0F;
        f = 1.0F - f;
        setPosition(portalPosX + mcc$get$motionX(this) * f, portalPosY + mcc$get$motionY(this) * f + (1.0F - age), portalPosZ + mcc$get$motionZ(this) * f);
        if (particleAge++ >= particleMaxAge) setDead();
    }
}
