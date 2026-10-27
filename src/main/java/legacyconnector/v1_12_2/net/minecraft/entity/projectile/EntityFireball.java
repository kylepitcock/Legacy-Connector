package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 fireball: a projectile without gravity that accelerates along its acceleration vector and calls {@link #onImpact} on what it
 * hits (mods' magic projectiles). A mod's subclass flies this way on its host; a view of a modern fireball is flown by the modern game
 * (its acceleration is the modern acceleration power along its motion).
 */
public abstract class EntityFireball extends Entity {
    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;
    private int ticksAlive;
    private int ticksInAir;
    @Deprecated
    public EntityLivingBase shootingEntity;
    private EntityLivingBase mcc$shooter;
    @Deprecated
    public double accelerationX;
    @Deprecated
    public double accelerationY;
    @Deprecated
    public double accelerationZ;
    private double mcc$ax;
    private double mcc$ay;
    private double mcc$az;

    public EntityFireball(World world) {
        super(world);
        setSize(1.0F, 1.0F);
    }

    public EntityFireball(World world, double x, double y, double z, double accelX, double accelY, double accelZ) {
        this(world);
        setLocationAndAngles(x, y, z, mcc$get$rotationYaw(this), mcc$get$rotationPitch(this));
        setPosition(x, y, z);
        mcc$setAcceleration(accelX, accelY, accelZ);
    }

    public EntityFireball(World world, EntityLivingBase shooter, double accelX, double accelY, double accelZ) {
        this(world);
        mcc$set$shootingEntity(this, shooter);
        if (shooter != null) {
            setLocationAndAngles(shooter.mcc$x(), shooter.mcc$y(), shooter.mcc$z(), mcc$get$rotationYaw(shooter), mcc$get$rotationPitch(shooter));
            setPosition(shooter.mcc$x(), shooter.mcc$y(), shooter.mcc$z());
        }
        mcc$set$motionX(this, 0.0D);
        mcc$set$motionY(this, 0.0D);
        mcc$set$motionZ(this, 0.0D);
        accelX += rand.nextGaussian() * 0.4D;
        accelY += rand.nextGaussian() * 0.4D;
        accelZ += rand.nextGaussian() * 0.4D;
        mcc$setAcceleration(accelX, accelY, accelZ);
    }

    protected EntityFireball(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.projectile.AbstractHurtingProjectile mcc$modernFireball() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.AbstractHurtingProjectile p ? p : null;
    }

    /** 1.12 normalised the acceleration to 0.1 per tick along the given direction. */
    private void mcc$setAcceleration(double x, double y, double z) {
        double d = MathHelper.sqrt(x * x + y * y + z * z);
        if (d == 0.0D) d = 1.0D;
        mcc$ax = x / d * 0.1D;
        mcc$ay = y / d * 0.1D;
        mcc$az = z / d * 0.1D;
        var m = mcc$modernFireball();
        if (m != null) {
            m.accelerationPower = 0.1D;
            m.setDeltaMovement(mcc$ax, mcc$ay, mcc$az);
        }
    }

    // ------------------------------------------------------------------ phantom fields

    public static EntityLivingBase mcc$get$shootingEntity(EntityFireball f) {
        var m = f.mcc$modernFireball();
        if (m != null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(m.getOwner()) instanceof EntityLivingBase l ? l : null;
        return f.mcc$shooter;
    }

    public static void mcc$set$shootingEntity(EntityFireball f, EntityLivingBase v) {
        f.mcc$shooter = v;
        var m = f.mcc$modernFireball();
        if (m != null) m.setOwner(v == null ? null : v.mcc$entity);
    }

    private static net.minecraft.world.phys.Vec3 mcc$modernAccel(EntityFireball f) {
        var m = f.mcc$modernFireball();
        if (m == null) return null;
        var v = m.getDeltaMovement();
        return v.lengthSqr() < 1.0E-7D ? net.minecraft.world.phys.Vec3.ZERO : v.normalize().scale(m.accelerationPower);
    }

    public static double mcc$get$accelerationX(EntityFireball f) {
        var a = mcc$modernAccel(f);
        return a != null ? a.x : f.mcc$ax;
    }

    public static double mcc$get$accelerationY(EntityFireball f) {
        var a = mcc$modernAccel(f);
        return a != null ? a.y : f.mcc$ay;
    }

    public static double mcc$get$accelerationZ(EntityFireball f) {
        var a = mcc$modernAccel(f);
        return a != null ? a.z : f.mcc$az;
    }

    public static void mcc$set$accelerationX(EntityFireball f, double v) {
        f.mcc$ax = v;
        f.mcc$syncAcceleration();
    }

    public static void mcc$set$accelerationY(EntityFireball f, double v) {
        f.mcc$ay = v;
        f.mcc$syncAcceleration();
    }

    public static void mcc$set$accelerationZ(EntityFireball f, double v) {
        f.mcc$az = v;
        f.mcc$syncAcceleration();
    }

    private void mcc$syncAcceleration() {
        var m = mcc$modernFireball();
        if (m == null) return;
        double len = Math.sqrt(mcc$ax * mcc$ax + mcc$ay * mcc$ay + mcc$az * mcc$az);
        m.accelerationPower = len;
        if (len > 0.0D) m.setDeltaMovement(new net.minecraft.world.phys.Vec3(mcc$ax, mcc$ay, mcc$az).normalize().scale(Math.max(m.getDeltaMovement().length(), len)));
    }

    // ------------------------------------------------------------------ flight

    @Override
    protected void entityInit() {
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        double d = getEntityBoundingBox().getAverageEdgeLength() * 4.0D;
        if (Double.isNaN(d)) d = 4.0D;
        d *= 64.0D;
        return distance < d * d;
    }

    @Override
    public void onUpdate() {
        var self = mcc$entity;
        if (!mcc$hosted() || self == null) {
            super.onUpdate();
            return;
        }
        var level = self.level();
        EntityLivingBase shooter = mcc$get$shootingEntity(this);
        if (!level.isClientSide() && (shooter != null && mcc$get$isDead(shooter) || !level.hasChunkAt(self.blockPosition()))) {
            setDead();
            return;
        }
        super.onUpdate();
        if (isFireballFiery()) setFire(1);
        ++ticksInAir;
        RayTraceResult result = ProjectileHelper.forwardsRaycast(this, true, ticksInAir >= 25, shooter);
        if (result != null && !org.kylepitcock.legacyconnector.v1_12_2.entity.ProjectileEvents1122.impact(this, result)) onImpact(result);
        double nx = mcc$x() + mcc$get$motionX(this);
        double ny = mcc$y() + mcc$get$motionY(this);
        double nz = mcc$z() + mcc$get$motionZ(this);
        ProjectileHelper.rotateTowardsMovement(this, 0.2F);
        float f = getMotionFactor();
        if (isInWater()) {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, nx, ny, nz, 4, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            f = 0.8F;
        }
        mcc$set$motionX(this, (mcc$get$motionX(this) + mcc$ax) * f);
        mcc$set$motionY(this, (mcc$get$motionY(this) + mcc$ay) * f);
        mcc$set$motionZ(this, (mcc$get$motionZ(this) + mcc$az) * f);
        EnumParticleTypes particle = getParticleType();
        if (particle != null && level.isClientSide()) {
            var options = particle.mcc$modern(0.0D, 0.0D, 0.0D);
            if (options != null) level.addParticle(options, nx, ny + 0.5D, nz, 0.0D, 0.0D, 0.0D);
        }
        setPosition(nx, ny, nz);
    }

    protected boolean isFireballFiery() {
        return true;
    }

    protected EnumParticleTypes getParticleType() {
        return EnumParticleTypes.SMOKE_NORMAL;
    }

    protected float getMotionFactor() {
        return 0.95F;
    }

    protected abstract void onImpact(RayTraceResult result);

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("xTile", xTile);
        tag.setInteger("yTile", yTile);
        tag.setInteger("zTile", zTile);
        tag.setTag("direction", newDoubleNBTList(mcc$get$motionX(this), mcc$get$motionY(this), mcc$get$motionZ(this)));
        tag.setTag("power", newDoubleNBTList(mcc$ax, mcc$ay, mcc$az));
        tag.setInteger("life", ticksAlive);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        xTile = tag.getInteger("xTile");
        yTile = tag.getInteger("yTile");
        zTile = tag.getInteger("zTile");
        if (tag.hasKey("power", 9)) {
            var power = tag.getTagList("power", 6);
            if (power.tagCount() == 3) {
                mcc$ax = power.getDoubleAt(0);
                mcc$ay = power.getDoubleAt(1);
                mcc$az = power.getDoubleAt(2);
            }
        }
        ticksAlive = tag.getInteger("life");
        if (tag.hasKey("direction", 9) && tag.getTagList("direction", 6).tagCount() == 3) {
            var dir = tag.getTagList("direction", 6);
            mcc$set$motionX(this, dir.getDoubleAt(0));
            mcc$set$motionY(this, dir.getDoubleAt(1));
            mcc$set$motionZ(this, dir.getDoubleAt(2));
        } else {
            setDead();
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public float getCollisionBorderSize() {
        return 1.0F;
    }

    /** Hitting a fireball deflects it along the attacker's look (1.12: and makes the attacker its shooter). */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!mcc$hosted()) return super.attackEntityFrom(source, amount);
        if (isEntityInvulnerable(source)) return false;
        markVelocityChanged();
        Entity attacker = source == null ? null : source.getTrueSource();
        if (attacker == null) return false;
        var look = attacker.getLookVec();
        mcc$set$motionX(this, look.x);
        mcc$set$motionY(this, look.y);
        mcc$set$motionZ(this, look.z);
        mcc$ax = look.x * 0.1D;
        mcc$ay = look.y * 0.1D;
        mcc$az = look.z * 0.1D;
        if (attacker instanceof EntityLivingBase l) mcc$set$shootingEntity(this, l);
        return true;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public int getBrightnessForRender() {
        return 15728880;
    }
}
