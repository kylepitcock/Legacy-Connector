package mccompat.v1_7_10.net.minecraft.client.particle;

import mccompat.v1_7_10.net.minecraft.client.renderer.Tessellator;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.util.IIcon;
import mccompat.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 particle: a client-only legacy entity (no modern counterpart) ticked and drawn by {@link EffectRenderer}.
 * Default rendering is a camera-facing quad of its icon (or its cell of the legacy particle sheet).
 */
public class EntityFX extends Entity {
    protected int particleTextureIndexX;
    protected int particleTextureIndexY;
    protected float particleTextureJitterX;
    protected float particleTextureJitterY;
    protected int particleAge;
    protected int particleMaxAge;
    protected float particleScale;
    protected float particleGravity;
    protected float particleRed;
    protected float particleGreen;
    protected float particleBlue;
    protected float particleAlpha = 1.0F;
    protected IIcon particleIcon;
    public static double interpPosX;
    public static double interpPosY;
    public static double interpPosZ;

    protected EntityFX(World world, double x, double y, double z) {
        super(world);
        setSize(0.2F, 0.2F);
        setPosition(x, y, z);
        mcc$set$lastTickPosX(this, x);
        mcc$set$lastTickPosY(this, y);
        mcc$set$lastTickPosZ(this, z);
        mcc$set$prevPosX(this, x);
        mcc$set$prevPosY(this, y);
        mcc$set$prevPosZ(this, z);
        particleRed = particleGreen = particleBlue = 1.0F;
        particleTextureJitterX = rand.nextFloat() * 3.0F;
        particleTextureJitterY = rand.nextFloat() * 3.0F;
        particleScale = (rand.nextFloat() * 0.5F + 0.5F) * 2.0F;
        particleMaxAge = (int) (4.0F / (rand.nextFloat() * 0.9F + 0.1F));
    }

    public EntityFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        this(world, x, y, z);
        double mx = vx + (Math.random() * 2.0 - 1.0) * 0.4;
        double my = vy + (Math.random() * 2.0 - 1.0) * 0.4;
        double mz = vz + (Math.random() * 2.0 - 1.0) * 0.4;
        float speed = (float) (Math.random() + Math.random() + 1.0) * 0.15F;
        double len = Math.sqrt(mx * mx + my * my + mz * mz);
        if (len > 0) {
            mx = mx / len * speed * 0.4;
            my = my / len * speed * 0.4 + 0.1;
            mz = mz / len * speed * 0.4;
        }
        setVelocity(mx, my, mz);
    }

    public EntityFX multiplyVelocity(float f) {
        setVelocity(mcc$get$motionX(this) * f, (mcc$get$motionY(this) - 0.1) * f + 0.1, mcc$get$motionZ(this) * f);
        return this;
    }

    public EntityFX multipleParticleScaleBy(float f) {
        setSize(0.2F * f, 0.2F * f);
        particleScale *= f;
        return this;
    }

    public void setRBGColorF(float r, float g, float b) {
        particleRed = r;
        particleGreen = g;
        particleBlue = b;
    }

    public void setAlphaF(float a) {
        particleAlpha = a;
    }

    public float getRedColorF() {
        return particleRed;
    }

    public float getGreenColorF() {
        return particleGreen;
    }

    public float getBlueColorF() {
        return particleBlue;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        mcc$set$prevPosX(this, mcc$x());
        mcc$set$prevPosY(this, mcc$y());
        mcc$set$prevPosZ(this, mcc$z());
        if (particleAge++ >= particleMaxAge) setDead();
        mcc$set$motionY(this, mcc$get$motionY(this) - 0.04 * particleGravity);
        moveEntity(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this));
        setVelocity(mcc$get$motionX(this) * 0.98, mcc$get$motionY(this) * 0.98, mcc$get$motionZ(this) * 0.98);
    }

    /** Draws a camera-facing quad (the rotation arguments are the legacy camera basis). */
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        float u0 = particleTextureIndexX / 16.0F;
        float u1 = u0 + 0.0624375F;
        float v0 = particleTextureIndexY / 16.0F;
        float v1 = v0 + 0.0624375F;
        if (particleIcon != null) {
            u0 = particleIcon.getMinU();
            u1 = particleIcon.getMaxU();
            v0 = particleIcon.getMinV();
            v1 = particleIcon.getMaxV();
        }
        float size = 0.1F * particleScale;
        float x = (float) (mcc$get$prevPosX(this) + (mcc$x() - mcc$get$prevPosX(this)) * partialTicks - interpPosX);
        float y = (float) (mcc$get$prevPosY(this) + (mcc$y() - mcc$get$prevPosY(this)) * partialTicks - interpPosY);
        float z = (float) (mcc$get$prevPosZ(this) + (mcc$z() - mcc$get$prevPosZ(this)) * partialTicks - interpPosZ);
        t.setColorRGBA_F(particleRed, particleGreen, particleBlue, particleAlpha);
        t.addVertexWithUV(x - rx * size - ryz * size, y - rxz * size, z - rz * size - rxy * size, u1, v1);
        t.addVertexWithUV(x - rx * size + ryz * size, y + rxz * size, z - rz * size + rxy * size, u1, v0);
        t.addVertexWithUV(x + rx * size + ryz * size, y + rxz * size, z + rz * size + rxy * size, u0, v0);
        t.addVertexWithUV(x + rx * size - ryz * size, y - rxz * size, z + rz * size - rxy * size, u0, v1);
    }

    /** 0: legacy particle sheet, 1: block atlas, 2: item atlas, 3: the particle binds its own texture. */
    public int getFXLayer() {
        return 0;
    }

    public void setParticleIcon(IIcon icon) {
        this.particleIcon = icon;
    }

    public void setParticleTextureIndex(int index) {
        particleTextureIndexX = index % 16;
        particleTextureIndexY = index / 16;
    }

    public void nextTextureIndexX() {
        ++particleTextureIndexX;
    }

    public int getBrightnessForRender(float partialTicks) {
        World w = mcc$world();
        if (w == null) return 0xF000F0;
        int x = (int) Math.floor(mcc$x()), y = (int) Math.floor(mcc$y()), z = (int) Math.floor(mcc$z());
        return w.getLightBrightnessForSkyBlocks(x, y, z, 0);
    }

    public float getBrightness(float partialTicks) {
        World w = mcc$world();
        return w == null ? 1.0F : w.getLightBrightness((int) Math.floor(mcc$x()), (int) Math.floor(mcc$y()), (int) Math.floor(mcc$z()));
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", Pos (" + mcc$x() + "," + mcc$y() + "," + mcc$z() + "), RGBA (" + particleRed + "," + particleGreen + ","
                + particleBlue + "," + particleAlpha + "), Age " + particleAge;
    }
}
