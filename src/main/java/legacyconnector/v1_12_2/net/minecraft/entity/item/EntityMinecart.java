package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 minecart base. A view reads and drives the modern cart (type, damage, Forge's powered/rideable flags, rail movement); a mod's
 * subclass keeps its damage and NBT (1.12 rail physics are not reproduced for mods' own carts).
 */
public abstract class EntityMinecart extends Entity {
    private float mcc$damage;
    private int mcc$rollingAmplitude;

    public EntityMinecart(World world) {
        super(world);
        preventEntitySpawning = true;
        setSize(0.98F, 0.7F);
    }

    public EntityMinecart(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
        mcc$set$prevPosX(this, x);
        mcc$set$prevPosY(this, y);
        mcc$set$prevPosZ(this, z);
    }

    protected EntityMinecart(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.vehicle.AbstractMinecart mcc$cart() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecart c ? c : null;
    }

    public abstract Type getType();

    @Override
    protected void entityInit() {
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !mcc$get$isDead(this);
    }

    @Override
    public double getMountedYOffset() {
        return 0.0D;
    }

    public void setDamage(float damage) {
        var c = mcc$cart();
        if (c != null) c.setDamage(damage);
        mcc$damage = damage;
    }

    public float getDamage() {
        var c = mcc$cart();
        return c != null ? c.getDamage() : mcc$damage;
    }

    public void setRollingAmplitude(int amplitude) {
        var c = mcc$cart();
        if (c != null) c.setHurtTime(amplitude);
        mcc$rollingAmplitude = amplitude;
    }

    public int getRollingAmplitude() {
        var c = mcc$cart();
        return c != null ? c.getHurtTime() : mcc$rollingAmplitude;
    }

    /** Breaks the cart and drops it (and its contents). */
    public void killMinecart(DamageSource source) {
        var c = mcc$cart();
        if (c != null && c.level() instanceof net.minecraft.server.level.ServerLevel) {
            var modernSource = source == null ? c.damageSources().generic() : source.mcc$modern(c.level());
            if (c instanceof net.minecraft.world.entity.vehicle.AbstractMinecartContainer container) container.destroy(modernSource);
            else c.destroy(c.getPickResult().getItem());
            return;
        }
        setDead();
        if (mcc$entity != null && mcc$entity.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) {
            entityDropItem(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:minecart")), 0.0F);
        }
    }

    private boolean mcc$hasDisplayTile;

    /** Forge: the item this cart drops and is picked as (a plain minecart for spawner carts and mods' own kinds). */
    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getCartItem() {
        var c = mcc$cart();
        if (c != null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(c.getPickResult());
        String name = switch (getType()) {
            case CHEST -> "chest_minecart";
            case FURNACE -> "furnace_minecart";
            case TNT -> "tnt_minecart";
            case HOPPER -> "hopper_minecart";
            case COMMAND_BLOCK -> "command_block_minecart";
            default -> "minecart";
        };
        return new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:" + name));
    }

    /** Whether the cart shows a custom block (set once a mod gives it one). */
    public void setHasDisplayTile(boolean showBlock) {
        var c = mcc$cart();
        if (c != null) c.setCustomDisplay(showBlock);
        mcc$hasDisplayTile = showBlock;
    }

    public boolean hasDisplayTile() {
        var c = mcc$cart();
        return c != null ? c.hasCustomDisplay() : mcc$hasDisplayTile;
    }

    /** Forge: whether it is pushed by its own power (furnace carts). */
    public boolean isPoweredCart() {
        var c = mcc$cart();
        return c != null ? c.isPoweredCart() : getType() == Type.FURNACE;
    }

    /** Forge: whether entities may ride it. */
    public boolean canBeRidden() {
        var c = mcc$cart();
        return c != null ? c.canBeRidden() : getType() == Type.RIDEABLE;
    }

    /** Forge: moves along the rail at {@code pos} (the modern rail movement). */
    public void moveMinecartOnRail(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        var c = mcc$cart();
        if (c != null && pos != null) c.moveMinecartOnRail(pos.mcc$modern());
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
    }

    /** The 1.12 cart kinds (Forge's getType). */
    public enum Type {
        RIDEABLE(0, "MinecartRideable"), CHEST(1, "MinecartChest"), FURNACE(2, "MinecartFurnace"), TNT(3, "MinecartTNT"),
        SPAWNER(4, "MinecartSpawner"), HOPPER(5, "MinecartHopper"), COMMAND_BLOCK(6, "MinecartCommandBlock");

        private final int networkID;
        private final String name;

        Type(int networkID, String name) {
            this.networkID = networkID;
            this.name = name;
        }

        public int getId() {
            return networkID;
        }

        public String getName() {
            return name;
        }

        public static Type getById(int id) {
            return id < 0 || id >= values().length ? RIDEABLE : values()[id];
        }
    }
}
