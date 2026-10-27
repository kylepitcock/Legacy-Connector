package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 boat. A view (and {@code new EntityBoat(world, x, y, z)}) is a modern boat: type, damage, hurt time and direction, paddles read
 * and change the modern one, which floats and steers as usual. A mod's subclass keeps the 1.12 state; its own movement is whatever its
 * onUpdate does (the 1.12 boat physics are not reproduced for hosted boats).
 */
public class EntityBoat extends Entity {
    private float mcc$damage;
    private int mcc$timeSinceHit;
    private int mcc$forwardDirection = 1;
    private Type mcc$type = Type.OAK;
    private final boolean[] mcc$paddles = new boolean[2];
    @Deprecated
    public Status status;
    @Deprecated
    public Status previousStatus;
    public float outOfControlTicks;
    public float[] paddlePositions = new float[2];

    public EntityBoat(World world) {
        super(world);
        preventEntitySpawning = true;
        setSize(1.375F, 0.5625F);
    }

    public EntityBoat(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
        mcc$set$motionX(this, 0.0D);
        mcc$set$motionY(this, 0.0D);
        mcc$set$motionZ(this, 0.0D);
        mcc$set$prevPosX(this, x);
        mcc$set$prevPosY(this, y);
        mcc$set$prevPosZ(this, z);
    }

    protected EntityBoat(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.vehicle.Boat mcc$boat() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.vehicle.Boat b ? b : null;
    }

    public static Status mcc$get$status(EntityBoat e) {
        return e.getBoatStatus();
    }

    public static void mcc$set$status(EntityBoat e, Status v) {
    }

    public static Status mcc$get$previousStatus(EntityBoat e) {
        return e.getBoatStatus();
    }

    public static void mcc$set$previousStatus(EntityBoat e, Status v) {
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !mcc$get$isDead(this);
    }

    @Override
    public boolean canBePushed() {
        return true;
    }

    @Override
    public double getMountedYOffset() {
        return -0.1D;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.SoundCategory getSoundCategory() {
        return legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.NEUTRAL;
    }

    public void setDamageTaken(float damage) {
        var b = mcc$boat();
        if (b != null) b.setDamage(damage);
        mcc$damage = damage;
    }

    public float getDamageTaken() {
        var b = mcc$boat();
        return b != null ? b.getDamage() : mcc$damage;
    }

    public void setTimeSinceHit(int time) {
        var b = mcc$boat();
        if (b != null) b.setHurtTime(time);
        mcc$timeSinceHit = time;
    }

    public int getTimeSinceHit() {
        var b = mcc$boat();
        return b != null ? b.getHurtTime() : mcc$timeSinceHit;
    }

    public void setForwardDirection(int direction) {
        var b = mcc$boat();
        if (b != null) b.setHurtDir(direction);
        mcc$forwardDirection = direction;
    }

    public int getForwardDirection() {
        var b = mcc$boat();
        return b != null ? b.getHurtDir() : mcc$forwardDirection;
    }

    public void setBoatType(Type type) {
        if (type == null) return;
        var b = mcc$boat();
        if (b != null) b.setVariant(type.mcc$modern());
        mcc$type = type;
    }

    public Type getBoatType() {
        var b = mcc$boat();
        return b != null ? Type.mcc$of(b.getVariant()) : mcc$type;
    }

    public void setPaddleState(boolean left, boolean right) {
        var b = mcc$boat();
        if (b != null) b.setPaddleState(left, right);
        mcc$paddles[0] = left;
        mcc$paddles[1] = right;
    }

    public boolean getPaddleState(int side) {
        var b = mcc$boat();
        return b != null ? b.getPaddleState(side) : mcc$paddles[side];
    }

    public Status getBoatStatus() {
        var m = mcc$entity;
        if (m == null) return Status.ON_LAND;
        if (m.isUnderWater()) return m.level().getFluidState(m.blockPosition().above()).isSource() ? Status.UNDER_WATER : Status.UNDER_FLOWING_WATER;
        if (m.isInWater()) return Status.IN_WATER;
        return m.onGround() ? Status.ON_LAND : Status.IN_AIR;
    }

    /** The boat item of its wood. */
    public legacyconnector.v1_12_2.net.minecraft.item.Item getItemBoat() {
        String name = switch (getBoatType()) {
            case SPRUCE -> "spruce_boat";
            case BIRCH -> "birch_boat";
            case JUNGLE -> "jungle_boat";
            case ACACIA -> "acacia_boat";
            case DARK_OAK -> "dark_oak_boat";
            default -> "boat";
        };
        return legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:" + name);
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getPaddleSound() {
        Status s = getBoatStatus();
        var sound = s == Status.IN_WATER || s == Status.UNDER_WATER || s == Status.UNDER_FLOWING_WATER ? net.minecraft.sounds.SoundEvents.BOAT_PADDLE_WATER
                : s == Status.ON_LAND ? net.minecraft.sounds.SoundEvents.BOAT_PADDLE_LAND : null;
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(sound);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (mcc$boat() == null) tag.setString("Type", getBoatType().getName());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (mcc$boat() == null && tag.hasKey("Type", 8)) setBoatType(Type.getTypeFromString(tag.getString("Type")));
    }

    /** Where the boat is: in or under water, on land, in the air. */
    public enum Status {
        IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR
    }

    /** The 1.12 boat woods (metadata = the planks' metadata). */
    public enum Type {
        OAK(0, "oak"), SPRUCE(1, "spruce"), BIRCH(2, "birch"), JUNGLE(3, "jungle"), ACACIA(4, "acacia"), DARK_OAK(5, "dark_oak");

        private final int metadata;
        private final String name;

        Type(int metadata, String name) {
            this.metadata = metadata;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getMetadata() {
            return metadata;
        }

        @Override
        public String toString() {
            return name;
        }

        public static Type byId(int id) {
            return id < 0 || id >= values().length ? OAK : values()[id];
        }

        public static Type getTypeFromString(String name) {
            for (Type t : values()) if (t.name.equals(name)) return t;
            return OAK;
        }

        public net.minecraft.world.entity.vehicle.Boat.Type mcc$modern() {
            return net.minecraft.world.entity.vehicle.Boat.Type.byName(name);
        }

        public static Type mcc$of(net.minecraft.world.entity.vehicle.Boat.Type modern) {
            return modern == null ? OAK : getTypeFromString(modern.getName());
        }
    }
}
