package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** "enchantmenttable": a glyph that flies from its start along the velocity into the target, brightening. */
public class EntityEnchantmentTableParticleFX extends EntityFX {
    private float field_70565_a;
    private double field_70568_aq;
    private double field_70567_ar;
    private double field_70566_as;

    public EntityEnchantmentTableParticleFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        setVelocity(vx, vy, vz);
        this.field_70568_aq = x;
        this.field_70567_ar = y;
        this.field_70566_as = z;
        setPosition(x, y, z);
        float shade = rand.nextFloat() * 0.6F + 0.4F;
        this.field_70565_a = this.particleScale = rand.nextFloat() * 0.5F + 0.2F;
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F * shade;
        this.particleGreen *= 0.9F;
        this.particleRed *= 0.9F;
        this.particleMaxAge = (int) (Math.random() * 10.0D) + 30;
        mcc$set$noClip(this, true);
        setParticleTextureIndex((int) (Math.random() * 26.0D + 1.0D + 224.0D));
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
        f *= f;
        f *= f;
        return base * (1.0F - f) + f;
    }

    @Override
    public void onUpdate() {
        mcc$savePrev();
        float f = 1.0F - (float) particleAge / (float) particleMaxAge;
        float g = 1.0F - f;
        g *= g;
        g *= g;
        setPosition(field_70568_aq + mcc$get$motionX(this) * f, field_70567_ar + mcc$get$motionY(this) * f - g * 1.2F,
                field_70566_as + mcc$get$motionZ(this) * f);
        if (particleAge++ >= particleMaxAge) setDead();
    }
}
