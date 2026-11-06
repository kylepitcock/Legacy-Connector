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

    private legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState mcc$displayTile;
    private int mcc$displayTileOffset = -1;
    private boolean mcc$canUseRail = true;
    private float mcc$maxSpeedOnRail = 0.4F;

    /** The block the cart shows (air when none was set; a game cart reads its own). */
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getDisplayTile() {
        var c = mcc$cart();
        if (c != null) {
            var s = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(c.getDisplayBlockState());
            if (s != null) return s;
        }
        return mcc$displayTile != null ? mcc$displayTile : legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState();
    }

    public void setDisplayTile(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState displayTile) {
        mcc$displayTile = displayTile;
        var c = mcc$cart();
        if (c != null && displayTile != null) {
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(displayTile);
            if (modern != null) c.setDisplayBlockState(modern);
        }
        setHasDisplayTile(true);
    }

    /** How far (in sixteenths) the shown block sits above the cart's floor; -1 follows the default. */
    public int getDisplayTileOffset() {
        if (mcc$displayTileOffset != -1) return mcc$displayTileOffset;
        var c = mcc$cart();
        return c != null && hasDisplayTile() ? c.getDisplayOffset() : getDefaultDisplayTileOffset();
    }

    public void setDisplayTileOffset(int displayTileOffset) {
        mcc$displayTileOffset = displayTileOffset;
        var c = mcc$cart();
        if (c != null) c.setDisplayOffset(displayTileOffset);
    }

    public int getDefaultDisplayTileOffset() {
        return 6;
    }

    /** Forge: whether the cart follows rails (off: it moves like a free entity). */
    public boolean canUseRail() {
        return mcc$canUseRail;
    }

    public void setCanUseRail(boolean use) {
        mcc$canUseRail = use;
    }

    /** Forge: the speed cap on rails in blocks per tick (the vanilla 0.4). */
    public float getMaxCartSpeedOnRail() {
        return mcc$maxSpeedOnRail;
    }

    public void setCurrentCartSpeedCapOnRail(float value) {
        mcc$maxSpeedOnRail = Math.min(value, getMaxCartSpeedOnRail());
    }

    /** Forge: the speed cap in the cart's current medium (the game's own value for a game cart). */
    public double getMaximumSpeed() {
        var c = mcc$cart();
        return c != null ? (c.isInWater() ? 0.2D : 0.4D) : (double) mcc$maxSpeedOnRail;
    }

    /** Slows the cart down with its drag (the game applies its own on game carts). */
    protected void applyDrag() {
        mcc$set$motionX(this, mcc$get$motionX(this) * 0.96D);
        mcc$set$motionY(this, 0.0D);
        mcc$set$motionZ(this, mcc$get$motionZ(this) * 0.96D);
    }

    /** One step of rail movement: the game's cart moves itself, a mod's cart overrides this. */
    protected void moveAlongTrack(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
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

    private int mcc$rollingDirection = 1;

    public void setRollingDirection(int direction) {
        mcc$rollingDirection = direction;
    }

    /** The side the cart rocks to when hit (1 or -1). */
    public int getRollingDirection() {
        var c = mcc$cart();
        return c != null ? c.getHurtDir() : mcc$rollingDirection;
    }

    /** The rail shapes' end points ({@code MATRIX} of 1.12's minecart: two block offsets per shape, by metadata). */
    private static final int[][][] RAIL_MATRIX = {{{0, 0, -1}, {0, 0, 1}}, {{-1, 0, 0}, {1, 0, 0}}, {{-1, -1, 0}, {1, 0, 0}},
            {{-1, 0, 0}, {1, -1, 0}}, {{0, 0, -1}, {0, -1, 1}}, {{0, -1, -1}, {0, 0, 1}}, {{0, 0, 1}, {1, 0, 0}}, {{0, 0, 1}, {-1, 0, 0}},
            {{0, 0, -1}, {-1, 0, 0}}, {{0, 0, -1}, {1, 0, 0}}};

    /** The point of the rail under (x, y, z) the cart's lengthwise line passes through, or null off the rails. */
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getPos(double x, double y, double z) {
        int i = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(x);
        int j = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(y);
        int k = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(z);
        World w = getEntityWorld();
        if (w == null) return null;
        if (legacyconnector.v1_12_2.net.minecraft.block.BlockRailBase.isRailBlock(w, new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(i, j - 1, k))) --j;
        var pos = new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(i, j, k);
        var state = w.getBlockState(pos);
        if (!legacyconnector.v1_12_2.net.minecraft.block.BlockRailBase.isRailBlock(state)) return null;
        var dir = ((legacyconnector.v1_12_2.net.minecraft.block.BlockRailBase) state.getBlock()).getRailDirection(w, pos, state, this);
        int[][] m = RAIL_MATRIX[dir.getMetadata()];
        double d0 = i + 0.5D + m[0][0] * 0.5D;
        double d1 = j + 0.0625D + m[0][1] * 0.5D;
        double d2 = k + 0.5D + m[0][2] * 0.5D;
        double d3 = i + 0.5D + m[1][0] * 0.5D;
        double d4 = j + 0.0625D + m[1][1] * 0.5D;
        double d5 = k + 0.5D + m[1][2] * 0.5D;
        double d6 = d3 - d0;
        double d7 = (d4 - d1) * 2.0D;
        double d8 = d5 - d2;
        double d9;
        if (d6 == 0.0D) d9 = z - k;
        else if (d8 == 0.0D) d9 = x - i;
        else d9 = ((x - d0) * d6 + (z - d2) * d8) * 2.0D;
        double ox = d0 + d6 * d9;
        double oy = d1 + d7 * d9;
        double oz = d2 + d8 * d9;
        if (d7 < 0.0D) ++oy;
        if (d7 > 0.0D) oy += 0.5D;
        return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(ox, oy, oz);
    }

    /** {@link #getPos} moved {@code offset} blocks along the rail. */
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getPosOffset(double x, double y, double z, double offset) {
        int i = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(x);
        int j = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(y);
        int k = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(z);
        World w = getEntityWorld();
        if (w == null) return null;
        if (legacyconnector.v1_12_2.net.minecraft.block.BlockRailBase.isRailBlock(w, new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(i, j - 1, k))) --j;
        var pos = new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(i, j, k);
        var state = w.getBlockState(pos);
        if (!legacyconnector.v1_12_2.net.minecraft.block.BlockRailBase.isRailBlock(state)) return null;
        var dir = ((legacyconnector.v1_12_2.net.minecraft.block.BlockRailBase) state.getBlock()).getRailDirection(w, pos, state, this);
        int[][] m = RAIL_MATRIX[dir.getMetadata()];
        y = j;
        if (dir.isAscending()) y = j + 1;
        double d0 = m[1][0] - m[0][0];
        double d1 = m[1][2] - m[0][2];
        double d2 = Math.sqrt(d0 * d0 + d1 * d1);
        d0 /= d2;
        d1 /= d2;
        x += d0 * offset;
        z += d1 * offset;
        if (m[0][1] != 0 && legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(x) - i == m[0][0]
                && legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(z) - k == m[0][2]) {
            y += m[0][1];
        } else if (m[1][1] != 0 && legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(x) - i == m[1][0]
                && legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(z) - k == m[1][2]) {
            y += m[1][1];
        }
        return getPos(x, y, z);
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
