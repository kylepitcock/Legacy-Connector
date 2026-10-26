package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;

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

    /**
     * 1.7.10 particles collide with blocks unless {@code noClip}: the move is clipped against the block collision shapes of
     * the client world (particles have no modern entity, so the plain Entity move would pass through the ground).
     */
    @Override
    public void moveEntity(double dx, double dy, double dz) {
        World w = mcc$world();
        net.minecraft.world.level.Level level = w == null ? null : w.mcc$fullLevel();
        if (mcc$get$noClip(this) || level == null) {
            super.moveEntity(dx, dy, dz);
            return;
        }
        double x = mcc$x(), y = mcc$y(), z = mcc$z();
        double hw = Math.max(0.005D, mcc$get$width(this) / 2.0D), hh = Math.max(0.005D, mcc$get$height(this) / 2.0D);
        var box = new net.minecraft.world.phys.AABB(x - hw, y - hh, z - hw, x + hw, y + hh, z + hw);
        net.minecraft.world.phys.Vec3 moved;
        try {
            moved = net.minecraft.world.entity.Entity.collideBoundingBox(null, new net.minecraft.world.phys.Vec3(dx, dy, dz), box, level, java.util.List.of());
        } catch (RuntimeException e) {
            moved = new net.minecraft.world.phys.Vec3(dx, dy, dz);
        }
        setPosition(x + moved.x, y + moved.y, z + moved.z);
        boolean hitX = moved.x != dx, hitY = moved.y != dy, hitZ = moved.z != dz;
        mcc$set$isCollidedHorizontally(this, hitX || hitZ);
        mcc$set$isCollidedVertically(this, hitY);
        mcc$set$isCollided(this, hitX || hitY || hitZ);
        mcc$set$onGround(this, hitY && dy < 0.0D);
        if (hitX) mcc$set$motionX(this, 0.0D);
        if (hitY) mcc$set$motionY(this, 0.0D);
        if (hitZ) mcc$set$motionZ(this, 0.0D);
    }

    /** Entity's "eating" flag: particles have no data watcher, the flag only matters to code reading it back. */
    public void setEating(boolean eating) {
        setFlag(4, eating);
    }

    public void addEntityCrashInfo(legacyconnector.v1_7_10.net.minecraft.crash.CrashReportCategory category) {
        category.addCrashSection("Entity Type", getClass().getName());
        category.addCrashSection("Entity ID", getEntityId());
        category.addCrashSection("Entity's Exact location", String.format("%.2f, %.2f, %.2f", mcc$x(), mcc$y(), mcc$z()));
        category.addCrashSection("Entity's Momentum", String.format("%.2f, %.2f, %.2f", mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this)));
        category.addCrashSection("Particle", toString());
    }

    /** The interpolated render position relative to the camera (the start of every legacy renderParticle). */
    protected float mcc$renderX(float partialTicks) {
        return (float) (mcc$get$prevPosX(this) + (mcc$x() - mcc$get$prevPosX(this)) * partialTicks - interpPosX);
    }

    protected float mcc$renderY(float partialTicks) {
        return (float) (mcc$get$prevPosY(this) + (mcc$y() - mcc$get$prevPosY(this)) * partialTicks - interpPosY);
    }

    protected float mcc$renderZ(float partialTicks) {
        return (float) (mcc$get$prevPosZ(this) + (mcc$z() - mcc$get$prevPosZ(this)) * partialTicks - interpPosZ);
    }

    /** Draws a quarter of the icon picked by the texture jitter (digging and item-crack particles). */
    protected void mcc$renderIconPiece(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        float u0 = (particleTextureIndexX + particleTextureJitterX / 4.0F) / 16.0F;
        float u1 = u0 + 0.015609375F;
        float v0 = (particleTextureIndexY + particleTextureJitterY / 4.0F) / 16.0F;
        float v1 = v0 + 0.015609375F;
        float size = 0.1F * particleScale;
        if (particleIcon != null) {
            u0 = particleIcon.getInterpolatedU(particleTextureJitterX / 4.0F * 16.0F);
            u1 = particleIcon.getInterpolatedU((particleTextureJitterX + 1.0F) / 4.0F * 16.0F);
            v0 = particleIcon.getInterpolatedV(particleTextureJitterY / 4.0F * 16.0F);
            v1 = particleIcon.getInterpolatedV((particleTextureJitterY + 1.0F) / 4.0F * 16.0F);
        }
        float x = mcc$renderX(partialTicks), y = mcc$renderY(partialTicks), z = mcc$renderZ(partialTicks);
        t.setColorOpaque_F(particleRed, particleGreen, particleBlue);
        t.addVertexWithUV(x - rx * size - ryz * size, y - rxz * size, z - rz * size - rxy * size, u0, v1);
        t.addVertexWithUV(x - rx * size + ryz * size, y + rxz * size, z - rz * size + rxy * size, u0, v0);
        t.addVertexWithUV(x + rx * size + ryz * size, y + rxz * size, z + rz * size + rxy * size, u1, v0);
        t.addVertexWithUV(x + rx * size - ryz * size, y - rxz * size, z + rz * size - rxy * size, u1, v1);
    }

    /** The common 1.7.10 "grow in over the first frames" scale curve: min(1, (age + partial) / maxAge * 32). */
    protected float mcc$growIn(float partialTicks) {
        float f = (particleAge + partialTicks) / particleMaxAge * 32.0F;
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }

    /** One step of the frame index that counts the eight smoke-like cells down over the particle's life. */
    protected int mcc$ageCell() {
        return particleMaxAge <= 0 ? 0 : 7 - particleAge * 8 / particleMaxAge;
    }

    /** prevPos = pos, the first line of every legacy particle onUpdate. */
    protected void mcc$savePrev() {
        mcc$set$prevPosX(this, mcc$x());
        mcc$set$prevPosY(this, mcc$y());
        mcc$set$prevPosZ(this, mcc$z());
    }

    protected void mcc$scaleMotion(double fx, double fy, double fz) {
        setVelocity(mcc$get$motionX(this) * fx, mcc$get$motionY(this) * fy, mcc$get$motionZ(this) * fz);
    }

    /** Ground friction: horizontal motion times 0.7 while on the ground. */
    protected void mcc$groundFriction() {
        if (mcc$get$onGround(this)) mcc$scaleMotion(0.7D, 1.0D, 0.7D);
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
