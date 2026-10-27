package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 entity: a view over the modern entity it is bound to. Its public legacy fields (posX, motionY, rotationYaw, ...) are
 * phantoms whose accesses the loader redirects to the static accessors below, which read and write the modern entity; an
 * entity a mod made but has not spawned keeps them in local storage.
 */
public abstract class Entity implements legacyconnector.v1_12_2.net.minecraft.command.ICommandSender {
    public World world;
    public java.util.Random rand = new java.util.Random();
    /** The modern entity this one stands for. */
    public net.minecraft.world.entity.Entity mcc$entity;

    protected legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager dataManager;

    /** Phantom field: accesses are redirected to {@code mcc$get$posX}. */
    @Deprecated
    public double posX;
    private double mcc$local_posX;

    /** Phantom field: accesses are redirected to {@code mcc$get$posY}. */
    @Deprecated
    public double posY;
    private double mcc$local_posY;

    /** Phantom field: accesses are redirected to {@code mcc$get$posZ}. */
    @Deprecated
    public double posZ;
    private double mcc$local_posZ;

    /** Phantom field: accesses are redirected to {@code mcc$get$prevPosX}. */
    @Deprecated
    public double prevPosX;
    private double mcc$local_prevPosX;

    /** Phantom field: accesses are redirected to {@code mcc$get$prevPosY}. */
    @Deprecated
    public double prevPosY;
    private double mcc$local_prevPosY;

    /** Phantom field: accesses are redirected to {@code mcc$get$prevPosZ}. */
    @Deprecated
    public double prevPosZ;
    private double mcc$local_prevPosZ;

    /** Phantom field: accesses are redirected to {@code mcc$get$lastTickPosX}. */
    @Deprecated
    public double lastTickPosX;
    private double mcc$local_lastTickPosX;

    /** Phantom field: accesses are redirected to {@code mcc$get$lastTickPosY}. */
    @Deprecated
    public double lastTickPosY;
    private double mcc$local_lastTickPosY;

    /** Phantom field: accesses are redirected to {@code mcc$get$lastTickPosZ}. */
    @Deprecated
    public double lastTickPosZ;
    private double mcc$local_lastTickPosZ;

    /** Phantom field: accesses are redirected to {@code mcc$get$motionX}. */
    @Deprecated
    public double motionX;
    private double mcc$local_motionX;

    /** Phantom field: accesses are redirected to {@code mcc$get$motionY}. */
    @Deprecated
    public double motionY;
    private double mcc$local_motionY;

    /** Phantom field: accesses are redirected to {@code mcc$get$motionZ}. */
    @Deprecated
    public double motionZ;
    private double mcc$local_motionZ;

    /** Phantom field: accesses are redirected to {@code mcc$get$rotationYaw}. */
    @Deprecated
    public float rotationYaw;
    private float mcc$local_rotationYaw;

    /** Phantom field: accesses are redirected to {@code mcc$get$rotationPitch}. */
    @Deprecated
    public float rotationPitch;
    private float mcc$local_rotationPitch;

    /** Phantom field: accesses are redirected to {@code mcc$get$prevRotationYaw}. */
    @Deprecated
    public float prevRotationYaw;
    private float mcc$local_prevRotationYaw;

    /** Phantom field: accesses are redirected to {@code mcc$get$prevRotationPitch}. */
    @Deprecated
    public float prevRotationPitch;
    private float mcc$local_prevRotationPitch;

    /** Phantom field: accesses are redirected to {@code mcc$get$onGround}. */
    @Deprecated
    public boolean onGround;
    private boolean mcc$local_onGround;

    /** Phantom field: accesses are redirected to {@code mcc$get$isDead}. */
    @Deprecated
    public boolean isDead;
    private boolean mcc$local_isDead;

    /** Phantom field: accesses are redirected to {@code mcc$get$width}. */
    @Deprecated
    public float width;
    private float mcc$local_width;

    /** Phantom field: accesses are redirected to {@code mcc$get$height}. */
    @Deprecated
    public float height;
    private float mcc$local_height;

    /** Phantom field: accesses are redirected to {@code mcc$get$ticksExisted}. */
    @Deprecated
    public int ticksExisted;
    private int mcc$local_ticksExisted;

    /** Phantom field: accesses are redirected to {@code mcc$get$fallDistance}. */
    @Deprecated
    public float fallDistance;
    private float mcc$local_fallDistance;

    /** Phantom field: accesses are redirected to {@code mcc$get$isAirBorne}. */
    @Deprecated
    public boolean isAirBorne;
    private boolean mcc$local_isAirBorne;

    /** Phantom field: accesses are redirected to {@code mcc$get$noClip}. */
    @Deprecated
    public boolean noClip;
    private boolean mcc$local_noClip;

    /** Phantom field: accesses are redirected to {@code mcc$get$collidedHorizontally}. */
    @Deprecated
    public boolean collidedHorizontally;
    private boolean mcc$local_collidedHorizontally;

    /** Phantom field: accesses are redirected to {@code mcc$get$collidedVertically}. */
    @Deprecated
    public boolean collidedVertically;
    private boolean mcc$local_collidedVertically;

    /** Phantom field: accesses are redirected to {@code mcc$get$collided}. */
    @Deprecated
    public boolean collided;
    private boolean mcc$local_collided;

    /** Phantom field: accesses are redirected to {@code mcc$get$isImmuneToFire}. */
    @Deprecated
    public boolean isImmuneToFire;
    private boolean mcc$local_isImmuneToFire;

    /** Phantom field: accesses are redirected to {@code mcc$get$hurtResistantTime}. */
    @Deprecated
    public int hurtResistantTime;
    private int mcc$local_hurtResistantTime;

    /** Phantom field: accesses are redirected to {@code mcc$get$timeUntilPortal}. */
    @Deprecated
    public int timeUntilPortal;
    private int mcc$local_timeUntilPortal;

    /** Phantom field: accesses are redirected to {@code mcc$get$inWater}. */
    @Deprecated
    public boolean inWater;
    private boolean mcc$local_inWater;

    /** Phantom field: accesses are redirected to {@code mcc$get$stepHeight}. */
    @Deprecated
    public float stepHeight;
    private float mcc$local_stepHeight;

    /** Phantom field: accesses are redirected to {@code mcc$get$dimension}. */
    @Deprecated
    public int dimension;
    private int mcc$local_dimension;

    /** Phantom field: accesses are redirected to {@code mcc$get$entityUniqueID}. */
    @Deprecated
    public java.util.UUID entityUniqueID;
    private java.util.UUID mcc$local_entityUniqueID;

    public Entity(World world) {
        this.world = world;
        this.dataManager = new legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager(this);
        try {
            entityInit();
        } catch (RuntimeException | LinkageError e) {
            // a mod's data registration reaching an API the layer lacks must not stop the entity from being made
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entity").debug("entityInit of {} failed: {}", getClass().getName(), e.toString());
        }
    }

    /** Registers the entity's data keys (vanilla's are not kept; mods register theirs here). */
    protected void entityInit() {
    }

    public legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager getDataManager() {
        return dataManager;
    }

    public net.minecraft.world.entity.Entity mcc$modern() {
        return mcc$entity;
    }

    protected static Entity mcc$view(net.minecraft.world.entity.Entity modern) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern);
    }

    public World getEntityWorld() {
        return world;
    }

    public int getEntityId() {
        return mcc$entity == null ? 0 : mcc$entity.getId();
    }

    public java.util.UUID getUniqueID() {
        return mcc$get$entityUniqueID(this);
    }

    public String getCachedUniqueIdString() {
        return getUniqueID().toString();
    }

    public String getName() {
        return mcc$entity == null ? "" : mcc$entity.getName().getString();
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(mcc$entity == null ? getName() : mcc$entity.getDisplayName().getString());
    }

    public String getCustomNameTag() {
        return mcc$entity != null && mcc$entity.getCustomName() != null ? mcc$entity.getCustomName().getString() : "";
    }

    public boolean hasCustomName() {
        return mcc$entity != null && mcc$entity.hasCustomName();
    }

    public void setCustomNameTag(String name) {
        if (mcc$entity != null) mcc$entity.setCustomName(name == null || name.isEmpty() ? null : net.minecraft.network.chat.Component.literal(name));
    }

    // ------------------------------------------------------------------ position and movement

    public AxisAlignedBB getEntityBoundingBox() {
        if (mcc$entity == null) {
            double x = mcc$get$posX(this), y = mcc$get$posY(this), z = mcc$get$posZ(this);
            return new AxisAlignedBB(x - 0.3D, y, z - 0.3D, x + 0.3D, y + 1.8D, z + 0.3D);
        }
        var b = mcc$entity.getBoundingBox();
        return new AxisAlignedBB(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }

    public AxisAlignedBB getCollisionBoundingBox() {
        return null;
    }

    public void setPosition(double x, double y, double z) {
        if (mcc$entity != null) mcc$entity.setPos(x, y, z);
        else {
            mcc$local_posX = x;
            mcc$local_posY = y;
            mcc$local_posZ = z;
        }
    }

    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        setPosition(x, y, z);
        mcc$set$rotationYaw(this, yaw);
        mcc$set$rotationPitch(this, pitch);
    }

    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        if (mcc$entity != null) mcc$entity.moveTo(x, y, z, yaw, pitch);
        else setPositionAndRotation(x, y, z, yaw, pitch);
    }

    public void setPositionAndUpdate(double x, double y, double z) {
        if (mcc$entity != null) mcc$entity.teleportTo(x, y, z);
        else setPosition(x, y, z);
    }

    public void addVelocity(double x, double y, double z) {
        if (mcc$entity != null) {
            mcc$entity.push(x, y, z);
        } else {
            mcc$local_motionX += x;
            mcc$local_motionY += y;
            mcc$local_motionZ += z;
        }
    }

    public void move(MoverType type, double x, double y, double z) {
        if (mcc$entity != null) mcc$entity.move(type == null ? net.minecraft.world.entity.MoverType.SELF : type.mcc$modern(), new net.minecraft.world.phys.Vec3(x, y, z));
    }

    public float getEyeHeight() {
        return mcc$entity == null ? mcc$get$height(this) * 0.85F : mcc$entity.getEyeHeight();
    }

    public Vec3d getPositionEyes(float partialTicks) {
        if (mcc$entity == null) return new Vec3d(mcc$local_posX, mcc$local_posY + getEyeHeight(), mcc$local_posZ);
        var v = mcc$entity.getEyePosition(partialTicks);
        return new Vec3d(v.x, v.y, v.z);
    }

    public Vec3d getLookVec() {
        if (mcc$entity == null) return new Vec3d(0, 0, 1);
        var v = mcc$entity.getLookAngle();
        return new Vec3d(v.x, v.y, v.z);
    }

    public Vec3d getLook(float partialTicks) {
        if (mcc$entity == null) return getLookVec();
        var v = mcc$entity.getViewVector(partialTicks);
        return new Vec3d(v.x, v.y, v.z);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumFacing getHorizontalFacing() {
        return mcc$entity == null ? legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH
                : legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.mcc$of(mcc$entity.getDirection());
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumFacing getAdjustedHorizontalFacing() {
        return getHorizontalFacing();
    }

    public double getDistance(double x, double y, double z) {
        return Math.sqrt(getDistanceSq(x, y, z));
    }

    public double getDistanceSq(double x, double y, double z) {
        double dx = mcc$get$posX(this) - x, dy = mcc$get$posY(this) - y, dz = mcc$get$posZ(this) - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getDistanceSq(BlockPos pos) {
        return pos.distanceSq(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
    }

    public double getDistanceSqToCenter(BlockPos pos) {
        return pos.distanceSq(mcc$get$posX(this) - 0.5D, mcc$get$posY(this) - 0.5D, mcc$get$posZ(this) - 0.5D);
    }

    public float getDistance(Entity other) {
        return (float) Math.sqrt(getDistanceSq(other));
    }

    public double getDistanceSq(Entity other) {
        return getDistanceSq(mcc$get$posX(other), mcc$get$posY(other), mcc$get$posZ(other));
    }

    // ------------------------------------------------------------------ state

    public void setDead() {
        mcc$set$isDead(this, true);
    }

    public void onUpdate() {
    }

    public boolean isEntityAlive() {
        return mcc$entity != null ? mcc$entity.isAlive() : !mcc$local_isDead;
    }

    public boolean isSneaking() {
        return mcc$entity != null && mcc$entity.isShiftKeyDown();
    }

    public void setSneaking(boolean sneaking) {
        if (mcc$entity != null) mcc$entity.setShiftKeyDown(sneaking);
    }

    public boolean isSprinting() {
        return mcc$entity != null && mcc$entity.isSprinting();
    }

    public void setSprinting(boolean sprinting) {
        if (mcc$entity != null) mcc$entity.setSprinting(sprinting);
    }

    public boolean isInvisible() {
        return mcc$entity != null && mcc$entity.isInvisible();
    }

    public void setInvisible(boolean invisible) {
        if (mcc$entity != null) mcc$entity.setInvisible(invisible);
    }

    public boolean isGlowing() {
        return mcc$entity != null && mcc$entity.isCurrentlyGlowing();
    }

    public void setGlowing(boolean glowing) {
        if (mcc$entity != null) mcc$entity.setGlowingTag(glowing);
    }

    public boolean isSilent() {
        return mcc$entity != null && mcc$entity.isSilent();
    }

    public void setSilent(boolean silent) {
        if (mcc$entity != null) mcc$entity.setSilent(silent);
    }

    public boolean hasNoGravity() {
        return mcc$entity != null && mcc$entity.isNoGravity();
    }

    public void setNoGravity(boolean noGravity) {
        if (mcc$entity != null) mcc$entity.setNoGravity(noGravity);
    }

    public boolean isBurning() {
        return mcc$entity != null && mcc$entity.isOnFire();
    }

    public void setFire(int seconds) {
        if (mcc$entity != null) mcc$entity.igniteForSeconds(seconds);
    }

    public void extinguish() {
        if (mcc$entity != null) mcc$entity.clearFire();
    }

    public boolean isInWater() {
        return mcc$entity != null && mcc$entity.isInWater();
    }

    public boolean isWet() {
        return mcc$entity != null && mcc$entity.isInWaterRainOrBubble();
    }

    public boolean isInLava() {
        return mcc$entity != null && mcc$entity.isInLava();
    }

    public boolean isEntityInvulnerable(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        if (mcc$entity == null) return false;
        return source == null ? mcc$entity.isInvulnerable() : mcc$entity.isInvulnerableTo(source.mcc$modern(mcc$entity.level()));
    }

    public boolean getIsInvulnerable() {
        return mcc$entity != null && mcc$entity.isInvulnerable();
    }

    public void setEntityInvulnerable(boolean invulnerable) {
        if (mcc$entity != null) mcc$entity.setInvulnerable(invulnerable);
    }

    public boolean attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source, float amount) {
        return mcc$entity != null && source != null && mcc$entity.hurt(source.mcc$modern(mcc$entity.level()), amount);
    }

    public boolean canBeCollidedWith() {
        return mcc$entity != null && mcc$entity.isPickable();
    }

    public boolean canBePushed() {
        return mcc$entity != null && mcc$entity.isPushable();
    }

    public boolean isNonBoss() {
        return true;
    }

    public java.util.Set<String> getTags() {
        return mcc$entity == null ? new java.util.HashSet<>() : mcc$entity.getTags();
    }

    public boolean addTag(String tag) {
        return mcc$entity != null && mcc$entity.addTag(tag);
    }

    public boolean removeTag(String tag) {
        return mcc$entity != null && mcc$entity.removeTag(tag);
    }

    // ------------------------------------------------------------------ riding

    public Entity getRidingEntity() {
        return mcc$entity == null ? null : mcc$view(mcc$entity.getVehicle());
    }

    public boolean isRiding() {
        return mcc$entity != null && mcc$entity.isPassenger();
    }

    public boolean isBeingRidden() {
        return mcc$entity != null && mcc$entity.isVehicle();
    }

    public java.util.List<Entity> getPassengers() {
        java.util.List<Entity> out = new java.util.ArrayList<>();
        if (mcc$entity != null) for (var p : mcc$entity.getPassengers()) out.add(mcc$view(p));
        return out;
    }

    public Entity getControllingPassenger() {
        return mcc$entity == null ? null : mcc$view(mcc$entity.getControllingPassenger());
    }

    public Entity getLowestRidingEntity() {
        return mcc$entity == null ? this : mcc$view(mcc$entity.getRootVehicle());
    }

    public boolean startRiding(Entity vehicle) {
        return startRiding(vehicle, false);
    }

    public boolean startRiding(Entity vehicle, boolean force) {
        return mcc$entity != null && vehicle != null && vehicle.mcc$entity != null && mcc$entity.startRiding(vehicle.mcc$entity, force);
    }

    public void dismountRidingEntity() {
        if (mcc$entity != null) mcc$entity.stopRiding();
    }

    public void removePassengers() {
        if (mcc$entity != null) mcc$entity.ejectPassengers();
    }

    // ------------------------------------------------------------------ an entity as a command sender

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public BlockPos getPosition() {
        if (mcc$entity == null) return new BlockPos(mcc$local_posX, mcc$local_posY + 0.5D, mcc$local_posZ);
        var p = mcc$entity.blockPosition();
        return new BlockPos(p.getX(), p.getY(), p.getZ());
    }

    @Override
    public Vec3d getPositionVector() {
        return new Vec3d(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
    }

    @Override
    public Entity getCommandSenderEntity() {
        return this;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer getServer() {
        return legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "['" + getName() + "'/" + getEntityId() + "]";
    }

    // ------------------------------------------------------------------ phantom field accessors

    public static double mcc$get$posX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getX() : e.mcc$local_posX;
    }

    public static void mcc$set$posX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPos(v, m.getY(), m.getZ());
        } else {
            e.mcc$local_posX = v;
        }
    }

    public static double mcc$get$posY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getY() : e.mcc$local_posY;
    }

    public static void mcc$set$posY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPos(m.getX(), v, m.getZ());
        } else {
            e.mcc$local_posY = v;
        }
    }

    public static double mcc$get$posZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getZ() : e.mcc$local_posZ;
    }

    public static void mcc$set$posZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPos(m.getX(), m.getY(), v);
        } else {
            e.mcc$local_posZ = v;
        }
    }

    public static double mcc$get$prevPosX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xo : e.mcc$local_prevPosX;
    }

    public static void mcc$set$prevPosX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.xo = v;
        } else {
            e.mcc$local_prevPosX = v;
        }
    }

    public static double mcc$get$prevPosY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yo : e.mcc$local_prevPosY;
    }

    public static void mcc$set$prevPosY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.yo = v;
        } else {
            e.mcc$local_prevPosY = v;
        }
    }

    public static double mcc$get$prevPosZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.zo : e.mcc$local_prevPosZ;
    }

    public static void mcc$set$prevPosZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.zo = v;
        } else {
            e.mcc$local_prevPosZ = v;
        }
    }

    public static double mcc$get$lastTickPosX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xOld : e.mcc$local_lastTickPosX;
    }

    public static void mcc$set$lastTickPosX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.xOld = v;
        } else {
            e.mcc$local_lastTickPosX = v;
        }
    }

    public static double mcc$get$lastTickPosY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yOld : e.mcc$local_lastTickPosY;
    }

    public static void mcc$set$lastTickPosY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.yOld = v;
        } else {
            e.mcc$local_lastTickPosY = v;
        }
    }

    public static double mcc$get$lastTickPosZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.zOld : e.mcc$local_lastTickPosZ;
    }

    public static void mcc$set$lastTickPosZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.zOld = v;
        } else {
            e.mcc$local_lastTickPosZ = v;
        }
    }

    public static double mcc$get$motionX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getDeltaMovement().x : e.mcc$local_motionX;
    }

    public static void mcc$set$motionX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setDeltaMovement(v, m.getDeltaMovement().y, m.getDeltaMovement().z);
        } else {
            e.mcc$local_motionX = v;
        }
    }

    public static double mcc$get$motionY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getDeltaMovement().y : e.mcc$local_motionY;
    }

    public static void mcc$set$motionY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setDeltaMovement(m.getDeltaMovement().x, v, m.getDeltaMovement().z);
        } else {
            e.mcc$local_motionY = v;
        }
    }

    public static double mcc$get$motionZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getDeltaMovement().z : e.mcc$local_motionZ;
    }

    public static void mcc$set$motionZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setDeltaMovement(m.getDeltaMovement().x, m.getDeltaMovement().y, v);
        } else {
            e.mcc$local_motionZ = v;
        }
    }

    public static float mcc$get$rotationYaw(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getYRot() : e.mcc$local_rotationYaw;
    }

    public static void mcc$set$rotationYaw(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setYRot(v);
        } else {
            e.mcc$local_rotationYaw = v;
        }
    }

    public static float mcc$get$rotationPitch(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getXRot() : e.mcc$local_rotationPitch;
    }

    public static void mcc$set$rotationPitch(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setXRot(v);
        } else {
            e.mcc$local_rotationPitch = v;
        }
    }

    public static float mcc$get$prevRotationYaw(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yRotO : e.mcc$local_prevRotationYaw;
    }

    public static void mcc$set$prevRotationYaw(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.yRotO = v;
        } else {
            e.mcc$local_prevRotationYaw = v;
        }
    }

    public static float mcc$get$prevRotationPitch(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xRotO : e.mcc$local_prevRotationPitch;
    }

    public static void mcc$set$prevRotationPitch(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.xRotO = v;
        } else {
            e.mcc$local_prevRotationPitch = v;
        }
    }

    public static boolean mcc$get$onGround(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.onGround() : e.mcc$local_onGround;
    }

    public static void mcc$set$onGround(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setOnGround(v);
        } else {
            e.mcc$local_onGround = v;
        }
    }

    public static boolean mcc$get$isDead(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.isRemoved() : e.mcc$local_isDead;
    }

    public static void mcc$set$isDead(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            if (v && !m.isRemoved()) m.discard();
        } else {
            e.mcc$local_isDead = v;
        }
    }

    public static float mcc$get$width(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getBbWidth() : e.mcc$local_width;
    }

    public static void mcc$set$width(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            // derived from the modern entity
        } else {
            e.mcc$local_width = v;
        }
    }

    public static float mcc$get$height(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getBbHeight() : e.mcc$local_height;
    }

    public static void mcc$set$height(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            // derived from the modern entity
        } else {
            e.mcc$local_height = v;
        }
    }

    public static int mcc$get$ticksExisted(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.tickCount : e.mcc$local_ticksExisted;
    }

    public static void mcc$set$ticksExisted(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.tickCount = v;
        } else {
            e.mcc$local_ticksExisted = v;
        }
    }

    public static float mcc$get$fallDistance(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.fallDistance : e.mcc$local_fallDistance;
    }

    public static void mcc$set$fallDistance(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.fallDistance = v;
        } else {
            e.mcc$local_fallDistance = v;
        }
    }

    public static boolean mcc$get$isAirBorne(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.hasImpulse : e.mcc$local_isAirBorne;
    }

    public static void mcc$set$isAirBorne(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.hasImpulse = v;
        } else {
            e.mcc$local_isAirBorne = v;
        }
    }

    public static boolean mcc$get$noClip(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.noPhysics : e.mcc$local_noClip;
    }

    public static void mcc$set$noClip(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.noPhysics = v;
        } else {
            e.mcc$local_noClip = v;
        }
    }

    public static boolean mcc$get$collidedHorizontally(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.horizontalCollision : e.mcc$local_collidedHorizontally;
    }

    public static void mcc$set$collidedHorizontally(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.horizontalCollision = v;
        } else {
            e.mcc$local_collidedHorizontally = v;
        }
    }

    public static boolean mcc$get$collidedVertically(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.verticalCollision : e.mcc$local_collidedVertically;
    }

    public static void mcc$set$collidedVertically(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.verticalCollision = v;
        } else {
            e.mcc$local_collidedVertically = v;
        }
    }

    public static boolean mcc$get$collided(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.horizontalCollision || m.verticalCollision : e.mcc$local_collided;
    }

    public static void mcc$set$collided(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            // derived from the modern entity
        } else {
            e.mcc$local_collided = v;
        }
    }

    public static boolean mcc$get$isImmuneToFire(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.fireImmune() : e.mcc$local_isImmuneToFire;
    }

    public static void mcc$set$isImmuneToFire(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            // derived from the modern entity
        } else {
            e.mcc$local_isImmuneToFire = v;
        }
    }

    public static int mcc$get$hurtResistantTime(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.invulnerableTime : e.mcc$local_hurtResistantTime;
    }

    public static void mcc$set$hurtResistantTime(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.invulnerableTime = v;
        } else {
            e.mcc$local_hurtResistantTime = v;
        }
    }

    public static int mcc$get$timeUntilPortal(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getPortalCooldown() : e.mcc$local_timeUntilPortal;
    }

    public static void mcc$set$timeUntilPortal(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPortalCooldown(v);
        } else {
            e.mcc$local_timeUntilPortal = v;
        }
    }

    public static boolean mcc$get$inWater(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.isInWater() : e.mcc$local_inWater;
    }

    public static void mcc$set$inWater(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            // derived from the modern entity
        } else {
            e.mcc$local_inWater = v;
        }
    }

    public static float mcc$get$stepHeight(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.maxUpStep() : e.mcc$local_stepHeight;
    }

    public static void mcc$set$stepHeight(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            // derived from the modern entity
        } else {
            e.mcc$local_stepHeight = v;
        }
    }

    public static int mcc$get$dimension(Entity e) {
        var m = e.mcc$entity;
        return m != null ? org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.dimensionId(m.level()) : e.mcc$local_dimension;
    }

    public static void mcc$set$dimension(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) {
            // derived from the modern entity
        } else {
            e.mcc$local_dimension = v;
        }
    }

    public static java.util.UUID mcc$get$entityUniqueID(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getUUID() : e.mcc$local_entityUniqueID;
    }

    public static void mcc$set$entityUniqueID(Entity e, java.util.UUID v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setUUID(v);
        } else {
            e.mcc$local_entityUniqueID = v;
        }
    }
}
