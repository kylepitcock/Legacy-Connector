package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.IProjectile;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 EntityThrowable: a small projectile on a gravity arc that ray-traces blocks and entities each tick and calls
 * {@link #onImpact} on the first thing it hits (mods' thrown items). A mod's subclass runs this flight itself on its host; views of
 * modern thrown projectiles (snowballs, pearls) are moved by the modern game.
 */
public abstract class EntityThrowable extends Entity implements IProjectile {
    /** Data fixers of saved throwables: the layer reads saves with the modern game's fixers, so there is nothing to register. */
    public static void registerFixesThrowable(legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer fixer, String name) {
    }

    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;
    private legacyconnector.v1_12_2.net.minecraft.block.Block inTile;
    protected boolean inGround;
    public int throwableShake;
    protected EntityLivingBase thrower;
    private String throwerName;
    private int ticksInGround;
    private int ticksInAir;
    public Entity ignoreEntity;
    private int ignoreTime;

    public EntityThrowable(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    public EntityThrowable(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    public EntityThrowable(World world, EntityLivingBase thrower) {
        this(world, thrower.mcc$x(), thrower.mcc$y() + thrower.getEyeHeight() - 0.10000000149011612D, thrower.mcc$z());
        this.thrower = thrower;
        // a view of a vanilla projectile (snowball, pearl, ...) credits its thrower (kills, ender pearl teleports)
        if (!mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.Projectile p && thrower.mcc$entity != null) p.setOwner(thrower.mcc$entity);
    }

    protected EntityThrowable(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

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

    /** Aims along the shooter's rotation plus a pitch offset, adding the shooter's own motion. */
    public void shoot(Entity shooter, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy) {
        float x = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float y = -MathHelper.sin((pitch + pitchOffset) * 0.017453292F);
        float z = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        shoot(x, y, z, velocity, inaccuracy);
        mcc$set$motionX(this, mcc$get$motionX(this) + mcc$get$motionX(shooter));
        mcc$set$motionZ(this, mcc$get$motionZ(this) + mcc$get$motionZ(shooter));
        if (!mcc$get$onGround(shooter)) mcc$set$motionY(this, mcc$get$motionY(this) + mcc$get$motionY(shooter));
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        if (!mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.Projectile p) {
            p.shoot(x, y, z, velocity, inaccuracy);
            return;
        }
        float f = MathHelper.sqrt(x * x + y * y + z * z);
        x /= f;
        y /= f;
        z /= f;
        x += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        y += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        z += rand.nextGaussian() * 0.007499999832361937D * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        float h = MathHelper.sqrt(x * x + z * z);
        float yawNow = (float) (Math.atan2(x, z) * (180D / Math.PI));
        float pitchNow = (float) (Math.atan2(y, h) * (180D / Math.PI));
        mcc$set$rotationYaw(this, yawNow);
        mcc$set$prevRotationYaw(this, yawNow);
        mcc$set$rotationPitch(this, pitchNow);
        mcc$set$prevRotationPitch(this, pitchNow);
        ticksInGround = 0;
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        mcc$set$motionX(this, x);
        mcc$set$motionY(this, y);
        mcc$set$motionZ(this, z);
        if (mcc$get$prevRotationPitch(this) == 0.0F && mcc$get$prevRotationYaw(this) == 0.0F) {
            float h = MathHelper.sqrt(x * x + z * z);
            float yawNow = (float) (Math.atan2(x, z) * (180D / Math.PI));
            float pitchNow = (float) (Math.atan2(y, h) * (180D / Math.PI));
            mcc$set$rotationYaw(this, yawNow);
            mcc$set$prevRotationYaw(this, yawNow);
            mcc$set$rotationPitch(this, pitchNow);
            mcc$set$prevRotationPitch(this, pitchNow);
        }
    }

    @Override
    public void onUpdate() {
        mcc$set$lastTickPosX(this, mcc$x());
        mcc$set$lastTickPosY(this, mcc$y());
        mcc$set$lastTickPosZ(this, mcc$z());
        super.onUpdate();
        if (!mcc$hosted() || mcc$entity == null) return;
        var self = mcc$entity;
        var level = self.level();
        if (throwableShake > 0) --throwableShake;
        if (inGround) {
            var at = level.getBlockState(new net.minecraft.core.BlockPos(xTile, yTile, zTile));
            if (inTile != null && org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.legacyBlock(at) == inTile) {
                if (++ticksInGround == 1200) setDead();
                return;
            }
            inGround = false;
            mcc$set$motionX(this, mcc$get$motionX(this) * (rand.nextFloat() * 0.2F));
            mcc$set$motionY(this, mcc$get$motionY(this) * (rand.nextFloat() * 0.2F));
            mcc$set$motionZ(this, mcc$get$motionZ(this) * (rand.nextFloat() * 0.2F));
            ticksInGround = 0;
            ticksInAir = 0;
        } else {
            ++ticksInAir;
        }
        var from = self.position();
        var motion = self.getDeltaMovement();
        var to = from.add(motion);
        var blockHit = level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, self));
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) to = blockHit.getLocation();
        net.minecraft.world.phys.HitResult hit = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : blockHit;
        net.minecraft.world.entity.Entity target = null;
        double nearest = 0.0D;
        boolean ignoreSeen = false;
        EntityLivingBase owner = getThrower();
        for (var candidate : level.getEntities(self, self.getBoundingBox().expandTowards(motion).inflate(1.0D))) {
            var view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(candidate);
            if (view == null || !view.canBeCollidedWith()) continue;
            if (view == ignoreEntity) {
                ignoreSeen = true;
                continue;
            }
            if (owner != null && view == owner && ticksExisted() < 2 && ignoreEntity == null) {
                ignoreEntity = view;
                ignoreSeen = true;
                continue;
            }
            var intercept = candidate.getBoundingBox().inflate(0.30000001192092896D).clip(from, to);
            if (intercept.isPresent()) {
                double d = from.distanceToSqr(intercept.get());
                if (d < nearest || nearest == 0.0D) {
                    target = candidate;
                    nearest = d;
                }
            }
        }
        if (ignoreEntity != null) {
            if (ignoreSeen) ignoreTime = 2;
            else if (ignoreTime-- <= 0) ignoreEntity = null;
        }
        if (target != null) hit = new net.minecraft.world.phys.EntityHitResult(target);
        if (hit != null) {
            RayTraceResult result = RayTraceResult.mcc$of(hit);
            if (hit instanceof net.minecraft.world.phys.BlockHitResult b && level.getBlockState(b.getBlockPos()).is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL)) {
                setPortal(result.getBlockPos());
            } else if (!org.kylepitcock.legacyconnector.v1_12_2.entity.ProjectileEvents1122.impact(this, result)) {
                onImpact(result);
            }
        }
        double nx = mcc$x() + mcc$get$motionX(this);
        double ny = mcc$y() + mcc$get$motionY(this);
        double nz = mcc$z() + mcc$get$motionZ(this);
        float h = MathHelper.sqrt(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
        mcc$set$rotationYaw(this, (float) (Math.atan2(mcc$get$motionX(this), mcc$get$motionZ(this)) * (180D / Math.PI)));
        mcc$set$rotationPitch(this, (float) (Math.atan2(mcc$get$motionY(this), h) * (180D / Math.PI)));
        while (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this) < -180.0F) mcc$set$prevRotationPitch(this, mcc$get$prevRotationPitch(this) - 360.0F);
        while (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this) >= 180.0F) mcc$set$prevRotationPitch(this, mcc$get$prevRotationPitch(this) + 360.0F);
        while (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this) < -180.0F) mcc$set$prevRotationYaw(this, mcc$get$prevRotationYaw(this) - 360.0F);
        while (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this) >= 180.0F) mcc$set$prevRotationYaw(this, mcc$get$prevRotationYaw(this) + 360.0F);
        mcc$set$rotationPitch(this, mcc$get$prevRotationPitch(this) + (mcc$get$rotationPitch(this) - mcc$get$prevRotationPitch(this)) * 0.2F);
        mcc$set$rotationYaw(this, mcc$get$prevRotationYaw(this) + (mcc$get$rotationYaw(this) - mcc$get$prevRotationYaw(this)) * 0.2F);
        float drag = 0.99F;
        if (isInWater()) {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, nx, ny, nz, 4, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            drag = 0.8F;
        }
        mcc$set$motionX(this, mcc$get$motionX(this) * drag);
        mcc$set$motionY(this, mcc$get$motionY(this) * drag);
        mcc$set$motionZ(this, mcc$get$motionZ(this) * drag);
        if (!hasNoGravity()) mcc$set$motionY(this, mcc$get$motionY(this) - getGravityVelocity());
        if (!mcc$get$isDead(this)) setPosition(nx, ny, nz);
    }

    private int ticksExisted() {
        return mcc$get$ticksExisted(this);
    }

    protected float getGravityVelocity() {
        return 0.03F;
    }

    protected abstract void onImpact(RayTraceResult result);

    /** Marks the block the throwable is stuck in (subclasses that stop on blocks). */
    protected final void mcc$stickIn(int x, int y, int z) {
        xTile = x;
        yTile = y;
        zTile = z;
        if (mcc$entity != null) {
            inTile = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.legacyBlock(mcc$entity.level().getBlockState(new net.minecraft.core.BlockPos(x, y, z)));
        }
        inGround = true;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("xTile", xTile);
        tag.setInteger("yTile", yTile);
        tag.setInteger("zTile", zTile);
        var key = inTile == null ? null : inTile.getRegistryName();
        tag.setString("inTile", key == null ? "" : key.toString());
        tag.setByte("shake", (byte) throwableShake);
        tag.setByte("inGround", (byte) (inGround ? 1 : 0));
        if ((throwerName == null || throwerName.isEmpty()) && thrower instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer) throwerName = thrower.getName();
        tag.setString("ownerName", throwerName == null ? "" : throwerName);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        xTile = tag.getInteger("xTile");
        yTile = tag.getInteger("yTile");
        zTile = tag.getInteger("zTile");
        String tile = tag.getString("inTile");
        inTile = tile.isEmpty() ? null : legacyconnector.v1_12_2.net.minecraft.block.Block.getBlockFromName(tile);
        throwableShake = tag.getByte("shake") & 255;
        inGround = tag.getByte("inGround") == 1;
        thrower = null;
        throwerName = tag.getString("ownerName");
        if (throwerName != null && throwerName.isEmpty()) throwerName = null;
        thrower = getThrower();
    }

    public EntityLivingBase getThrower() {
        if (!mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.Projectile p) {
            var owner = p.getOwner();
            return owner == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(owner) instanceof EntityLivingBase l ? l : null;
        }
        if (thrower == null && throwerName != null && !throwerName.isEmpty() && mcc$entity != null) {
            for (var p : mcc$entity.level().players()) {
                if (p.getGameProfile().getName().equals(throwerName)
                        && org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p) instanceof EntityLivingBase l) {
                    thrower = l;
                }
            }
        }
        return thrower;
    }
}
