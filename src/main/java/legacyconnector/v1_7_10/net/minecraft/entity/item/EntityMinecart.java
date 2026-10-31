package legacyconnector.v1_7_10.net.minecraft.entity.item;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.BlockRailBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.IMinecartCollisionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;

/**
 * Base of minecarts (rideable 0, chest 1, furnace 2, TNT 3, spawner 4, hopper 5, command block 6) and of mods' carts. A mod's cart
 * keeps 1.7.10's watcher slots (17 rolling amplitude, 18 rolling direction, 19 damage, 20 display tile, 21 its offset, 22 whether
 * it is shown) and runs 1.7.10's (Forge-patched) rail physics; a view of a vanilla cart reads and writes the modern cart's own hurt
 * time, hurt direction, damage and display block, and moves by the modern physics.
 *
 * <p>The legacy {@code posY} of a cart is the middle of its box (1.7.10's {@code yOffset} 0.35 above the bottom), which the physics,
 * the renderers and the rider placement are written against; the modern entity's y is the bottom ({@link #mcc$posYShift}).</p>
 *
 * <p>Rails: a mod's rail ({@link BlockRailBase}) is asked through its Forge hooks (shape, maximum speed, onMinecartPass); a vanilla
 * or modern rail is read from its block state (shape, powered). Modern detector rails only see modern carts.</p>
 */
public abstract class EntityMinecart extends Entity {
    /** Track directions per shape (0-9): the two ends, as block offsets (x, y, z). */
    private static final int[][][] matrix = {{{0, 0, -1}, {0, 0, 1}}, {{-1, 0, 0}, {1, 0, 0}}, {{-1, -1, 0}, {1, 0, 0}}, {{-1, 0, 0}, {1, -1, 0}},
            {{0, 0, -1}, {0, -1, 1}}, {{0, -1, -1}, {0, 0, 1}}, {{0, 0, 1}, {1, 0, 0}}, {{0, 0, 1}, {-1, 0, 0}}, {{0, 0, -1}, {-1, 0, 0}},
            {{0, 0, -1}, {1, 0, 0}}};

    private boolean isInReverse;
    private String entityName;
    private boolean hasCustomName;
    private int turnProgress;
    private double minecartX;
    private double minecartY;
    private double minecartZ;
    private double minecartYaw;
    private double minecartPitch;

    /* Forge: minecart physics properties (defaults and per-cart values). */
    public static float defaultMaxSpeedAirLateral = 0.4F;
    public static float defaultMaxSpeedAirVertical = -1.0F;
    public static double defaultDragAir = 0.94999998807907104D;
    protected boolean canUseRail = true;
    protected boolean canBePushed = true;
    /** Forge: the collision handler every cart defers to (installed as NeoForge's, which still has the hook). */
    private static IMinecartCollisionHandler collisionHandler;
    private float currentSpeedRail = getMaxCartSpeedOnRail();
    protected float maxSpeedAirLateral = defaultMaxSpeedAirLateral;
    protected float maxSpeedAirVertical = defaultMaxSpeedAirVertical;
    protected double dragAir = defaultDragAir;

    public EntityMinecart(World world) {
        super(world);
        preventEntitySpawning = true;
        setSize(0.98F, 0.7F);
        yOffset = 0.35F;
        mcc$posYShift = yOffset;
    }

    public EntityMinecart(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y + yOffset, z);
        setVelocity(0.0D, 0.0D, 0.0D);
        mcc$set$prevPosX(this, x);
        mcc$set$prevPosY(this, y);
        mcc$set$prevPosZ(this, z);
    }

    /** View of a modern vanilla cart. */
    protected EntityMinecart(AbstractMinecart modern) {
        super(modern);
        yOffset = 0.35F;
        mcc$posYShift = yOffset;
    }

    /**
     * A cart's legacy posY is its bottom plus its CURRENT {@code yOffset}: mods' carts set their own after the constructor (Traincraft's
     * rolling stock 0.65, the base 0.35) and derive their position from the box with it, so a fixed 0.35 put them (and their riders and
     * particles) 0.3 too high.
     */
    @Override
    public double mcc$yShift() {
        return yOffset;
    }

    private AbstractMinecart mcc$cart() {
        return mcc$entity instanceof AbstractMinecart c ? c : null;
    }

    /** A cart of the given vanilla type (0 rideable, 1 chest, 2 furnace, 3 TNT, 4 spawner, 5 hopper, 6 command block). */
    public static EntityMinecart createMinecart(World world, double x, double y, double z, int type) {
        return switch (type) {
            case 1 -> new EntityMinecartChest(world, x, y, z);
            case 2 -> new EntityMinecartFurnace(world, x, y, z);
            case 3 -> new EntityMinecartTNT(world, x, y, z);
            case 4 -> new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityMinecartMobSpawner(world, x, y, z);
            case 5 -> new EntityMinecartHopper(world, x, y, z);
            case 6 -> new legacyconnector.v1_7_10.net.minecraft.entity.EntityMinecartCommandBlock(world, x, y, z);
            default -> new EntityMinecartEmpty(world, x, y, z);
        };
    }

    /** The cart type, from {@link #func_94087_l} in 1.7.10; subclasses return their own. */
    public abstract int getMinecartType();

    public void setMinecartName(String name) {
        this.entityName = name;
        this.hasCustomName = name != null && !name.isEmpty();
        var cart = mcc$cart();
        if (cart != null) cart.setCustomName(hasCustomName ? net.minecraft.network.chat.Component.literal(name) : null);
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    /** Carts can be hit while they exist (a vanilla cart answers for itself). */
    @Override
    public boolean canBeCollidedWith() {
        return mcc$cart() != null ? super.canBeCollidedWith() : isEntityAlive();
    }

    @Override
    public boolean canBePushed() {
        return mcc$cart() != null ? super.canBePushed() : canBePushed;
    }

    /** The box another entity collides with: its own box when it can be pushed (the collision handler decides when there is one). */
    @Override
    public AxisAlignedBB getCollisionBox(Entity other) {
        if (collisionHandler != null) return collisionHandler.getCollisionBox(this, other);
        return other != null && other.canBePushed() ? mcc$get$boundingBox(other) : null;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        if (collisionHandler != null) return collisionHandler.getBoundingBox(this);
        return null;
    }

    /** A rider sits 0.3 below the cart's middle. */
    @Override
    public double getMountedYOffset() {
        return mcc$get$height(this) * 0.0D - 0.30000001192092896D;
    }

    /** 1.7.10's seat: the rider at the cart's posY plus the mounted offset plus the rider's own y offset (a vanilla cart seats itself). */
    @Override
    public void updateRiderPosition() {
        if (mcc$cart() != null) {
            super.updateRiderPosition();
            return;
        }
        Entity rider = mcc$get$riddenByEntity(this);
        if (rider == null || rider.mcc$entity == null || mcc$entity == null) return;
        rider.setPosition(mcc$get$posX(this), mcc$get$posY(this) + getMountedYOffset() + rider.getYOffset(), mcc$get$posZ(this));
    }

    public String func_95999_t() {
        var cart = mcc$cart();
        if (cart != null) return cart.hasCustomName() ? cart.getCustomName().getString() : null;
        return entityName;
    }

    public boolean hasCustomMinecartName() {
        return hasCustomInventoryName();
    }

    public boolean hasCustomInventoryName() {
        var cart = mcc$cart();
        return cart != null ? cart.hasCustomName() : hasCustomName;
    }

    @Override
    public String getCommandSenderName() {
        return entityName != null && mcc$cart() == null ? entityName : super.getCommandSenderName();
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(17, Integer.valueOf(0));
        dataWatcher.addObject(18, Integer.valueOf(1));
        dataWatcher.addObject(19, Float.valueOf(0.0F));
        dataWatcher.addObject(20, Integer.valueOf(0));
        dataWatcher.addObject(21, Integer.valueOf(6));
        dataWatcher.addObject(22, Byte.valueOf((byte) 0));
    }

    // ------------------------------------------------------------------ Forge: collision handler, rail use and speed

    public static IMinecartCollisionHandler getCollisionHandler() {
        return collisionHandler;
    }

    public static void setCollisionHandler(IMinecartCollisionHandler handler) {
        collisionHandler = handler;
        org.kylepitcock.legacyconnector.v1_7_10.bridge.MinecartCollisionBridge.install(handler);
    }

    /** The item this cart drops and is picked with (the plain minecart for mods' carts that do not say). */
    public ItemStack getCartItem() {
        if (this instanceof EntityMinecartChest) return new ItemStack(Items.chest_minecart);
        if (this instanceof EntityMinecartTNT) return new ItemStack(Items.tnt_minecart);
        if (this instanceof EntityMinecartFurnace) return new ItemStack(Items.furnace_minecart);
        if (this instanceof EntityMinecartHopper) return new ItemStack(Items.hopper_minecart);
        if (this instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityMinecartCommandBlock) return new ItemStack(Items.command_block_minecart);
        return new ItemStack(Items.minecart);
    }

    /** Whether this cart pushes others (a furnace cart). */
    public boolean isPoweredCart() {
        return getMinecartType() == 2;
    }

    /** Whether mobs it rolls into get in (only the rideable cart). */
    public boolean canBeRidden() {
        return this instanceof EntityMinecartEmpty;
    }

    /** Whether the cart follows rails at all (Railcraft's elevator tracks switch it off). */
    public boolean canUseRail() {
        var cart = mcc$cart();
        return cart != null ? cart.canUseRail() : canUseRail;
    }

    public void setCanUseRail(boolean use) {
        canUseRail = use;
        var cart = mcc$cart();
        if (cart != null) cart.setCanUseRail(use);
    }

    /** Whether rails act on this cart (boosters, brakes, onMinecartPass). */
    public boolean shouldDoRailFunctions() {
        return true;
    }

    /** The highest speed any rail allows this cart. */
    public float getMaxCartSpeedOnRail() {
        return 1.2F;
    }

    public final float getCurrentCartSpeedCapOnRail() {
        var cart = mcc$cart();
        return cart != null ? cart.getCurrentCartSpeedCapOnRail() : currentSpeedRail;
    }

    /** Caps the cart's rail speed (at most {@link #getMaxCartSpeedOnRail}); Railcraft's high-speed and limiter tracks set it. */
    public final void setCurrentCartSpeedCapOnRail(float value) {
        value = Math.min(value, getMaxCartSpeedOnRail());
        var cart = mcc$cart();
        if (cart != null) cart.setCurrentCartSpeedCapOnRail(value);
        else currentSpeedRail = value;
    }

    public float getMaxSpeedAirLateral() {
        var cart = mcc$cart();
        return cart != null ? cart.getMaxSpeedAirLateral() : maxSpeedAirLateral;
    }

    public void setMaxSpeedAirLateral(float value) {
        maxSpeedAirLateral = value;
        var cart = mcc$cart();
        if (cart != null) cart.setMaxSpeedAirLateral(value);
    }

    public float getMaxSpeedAirVertical() {
        var cart = mcc$cart();
        return cart != null ? cart.getMaxSpeedAirVertical() : maxSpeedAirVertical;
    }

    public void setMaxSpeedAirVertical(float value) {
        maxSpeedAirVertical = value;
        var cart = mcc$cart();
        if (cart != null) cart.setMaxSpeedAirVertical(value);
    }

    public double getDragAir() {
        var cart = mcc$cart();
        return cart != null ? cart.getDragAir() : dragAir;
    }

    public void setDragAir(double value) {
        dragAir = value;
        var cart = mcc$cart();
        if (cart != null) cart.setDragAir(value);
    }

    /** Speed gained per tick going down a slope. */
    public double getSlopeAdjustment() {
        return 0.0078125D;
    }

    // ------------------------------------------------------------------ damage and the hurt wobble

    public void setDamage(float damage) {
        var cart = mcc$cart();
        if (cart != null) cart.setDamage(damage);
        else dataWatcher.updateObject(19, Float.valueOf(damage));
    }

    public float getDamage() {
        var cart = mcc$cart();
        return cart != null ? cart.getDamage() : dataWatcher.getWatchableObjectFloat(19);
    }

    /** Ticks the hurt wobble still lasts (the modern hurt time). */
    public void setRollingAmplitude(int amplitude) {
        var cart = mcc$cart();
        if (cart != null) cart.setHurtTime(amplitude);
        else dataWatcher.updateObject(17, Integer.valueOf(amplitude));
    }

    public int getRollingAmplitude() {
        var cart = mcc$cart();
        return cart != null ? cart.getHurtTime() : dataWatcher.getWatchableObjectInt(17);
    }

    /** Side the wobble swings to, 1 or -1 (the modern hurt direction). */
    public void setRollingDirection(int direction) {
        var cart = mcc$cart();
        if (cart != null) cart.setHurtDir(direction);
        else dataWatcher.updateObject(18, Integer.valueOf(direction));
    }

    public int getRollingDirection() {
        var cart = mcc$cart();
        return cart != null ? cart.getHurtDir() : dataWatcher.getWatchableObjectInt(18);
    }

    /** Vanilla cart damage: wobble, add ten times the damage, and break past 40 (at once for a creative player). */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (mcc$cart() != null) return super.attackEntityFrom(source, amount);
        if (mcc$world().isRemote || !isEntityAlive()) return true;
        if (isEntityInvulnerable()) return false;
        setRollingDirection(-getRollingDirection());
        setRollingAmplitude(10);
        setBeenAttacked();
        setDamage(getDamage() + amount * 10.0F);
        boolean creative = source != null && source.getEntity() instanceof EntityPlayer p && PlayerCapabilities.mcc$get$isCreativeMode(p.capabilities);
        if (creative || getDamage() > 40.0F) {
            Entity rider = mcc$get$riddenByEntity(this);
            if (rider != null) rider.mountEntity(null);
            if (creative && !hasCustomInventoryName()) setDead();
            else killMinecart(source);
        }
        return true;
    }

    /** Breaks the cart and drops it as an item (named after the cart). */
    public void killMinecart(DamageSource source) {
        setDead();
        ItemStack stack = new ItemStack(Items.minecart, 1);
        if (entityName != null) stack.setStackDisplayName(entityName);
        entityDropItem(stack, 0.0F);
    }

    /** Client: the wobble shown when the server reports a hit. */
    public void performHurtAnimation() {
        setRollingDirection(-getRollingDirection());
        setRollingAmplitude(10);
        setDamage(getDamage() + getDamage() * 10.0F);
    }

    @Override
    public float getShadowSize() {
        return 0.0F;
    }

    // ------------------------------------------------------------------ ticking: 1.7.10's cart physics

    /**
     * One tick of a mod's cart: the wobble and damage wear off, the client eases towards the server position, and the server moves
     * the cart along the rail under it (or lets it fall and roll off rails), turns it to its direction of travel, bumps other carts
     * and posts the MinecartUpdateEvent. A view of a vanilla cart is ticked by the modern cart itself.
     */
    @Override
    public void onUpdate() {
        if (mcc$cart() != null || mcc$entity == null) {
            super.onUpdate();
            return;
        }
        if (getRollingAmplitude() > 0) setRollingAmplitude(getRollingAmplitude() - 1);
        if (getDamage() > 0.0F) setDamage(getDamage() - 1.0F);
        // the modern base tick: portals, fire, fluids and falling out of the world (1.7.10's cart did its own portal handling)
        var host = mcc$host();
        if (host != null) host.mcc$superBaseTick();
        if (!isEntityAlive()) return;
        World world = mcc$world();
        if (world.isRemote) {
            if (turnProgress > 0) {
                double x = mcc$get$posX(this) + (minecartX - mcc$get$posX(this)) / turnProgress;
                double y = mcc$get$posY(this) + (minecartY - mcc$get$posY(this)) / turnProgress;
                double z = mcc$get$posZ(this) + (minecartZ - mcc$get$posZ(this)) / turnProgress;
                double dYaw = MathHelper.wrapAngleTo180_double(minecartYaw - mcc$get$rotationYaw(this));
                float yaw = (float) (mcc$get$rotationYaw(this) + dYaw / turnProgress);
                float pitch = (float) (mcc$get$rotationPitch(this) + (minecartPitch - mcc$get$rotationPitch(this)) / turnProgress);
                --turnProgress;
                setPosition(x, y, z);
                setRotation(yaw, pitch);
            }
            return;
        }
        mcc$set$prevPosX(this, mcc$get$posX(this));
        mcc$set$prevPosY(this, mcc$get$posY(this));
        mcc$set$prevPosZ(this, mcc$get$posZ(this));
        mcc$set$motionY(this, mcc$get$motionY(this) - 0.03999999910593033D);
        int x = MathHelper.floor_double(mcc$get$posX(this));
        int y = MathHelper.floor_double(mcc$get$posY(this));
        int z = MathHelper.floor_double(mcc$get$posZ(this));
        if (mcc$railShape(world, world.getBlock(x, y - 1, z), x, y - 1, z) >= 0) --y;
        Block block = world.getBlock(x, y, z);
        int shape = mcc$railShape(world, block, x, y, z);
        if (canUseRail() && shape >= 0) {
            double maxSpeed = Math.min(mcc$railMaxSpeed(world, block, x, y, z), getCurrentCartSpeedCapOnRail());
            func_145821_a(x, y, z, maxSpeed, getSlopeAdjustment(), block, shape);
            if (mcc$isActivatorRail(world, block, x, y, z)) onActivatorRailPass(x, y, z, mcc$railPowered(world, block, x, y, z));
        } else {
            func_94088_b(mcc$get$onGround(this) ? 0.4D : getMaxSpeedAirLateral());
        }
        doBlockCollisions();
        mcc$set$rotationPitch(this, 0.0F);
        double dx = mcc$get$prevPosX(this) - mcc$get$posX(this);
        double dz = mcc$get$prevPosZ(this) - mcc$get$posZ(this);
        float yaw = mcc$get$rotationYaw(this);
        if (dx * dx + dz * dz > 0.001D) {
            yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI);
            if (isInReverse) yaw += 180.0F;
        }
        double turn = MathHelper.wrapAngleTo180_float(yaw - mcc$get$prevRotationYaw(this));
        if (turn < -170.0D || turn >= 170.0D) {
            yaw += 180.0F;
            isInReverse = !isInReverse;
        }
        setRotation(yaw, 0.0F);
        AxisAlignedBB box = collisionHandler != null ? collisionHandler.getMinecartCollisionBox(this) : mcc$get$boundingBox(this).expand(0.2D, 0.0D, 0.2D);
        List<?> list = box == null ? null : world.getEntitiesWithinAABBExcludingEntity(this, box);
        Entity rider = mcc$get$riddenByEntity(this);
        if (list != null) {
            for (Object o : list) {
                if (o instanceof EntityMinecart other && o != rider && other.canBePushed()) other.applyEntityCollision(this);
            }
        }
        legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartUpdateEvent(this, x, y, z));
    }

    /** An activator rail under the cart, powered or not (the hopper cart locks, the TNT cart primes). */
    public void onActivatorRailPass(int x, int y, int z, boolean powered) {
    }

    /** Off the rails: speed capped to {@code max} sideways (and Forge's vertical cap), halved on the ground, air drag in the air. */
    protected void func_94088_b(double max) {
        double mx = MathHelper.clamp_double(mcc$get$motionX(this), -max, max);
        double my = mcc$get$motionY(this);
        double mz = MathHelper.clamp_double(mcc$get$motionZ(this), -max, max);
        double moveY = my;
        if (getMaxSpeedAirVertical() > 0.0F && my > getMaxSpeedAirVertical()) {
            moveY = getMaxSpeedAirVertical();
            if (Math.abs(mx) < 0.3F && Math.abs(mz) < 0.3F) {
                moveY = 0.15F;
                my = moveY;
            }
        }
        if (mcc$get$onGround(this)) {
            mx *= 0.5D;
            my *= 0.5D;
            mz *= 0.5D;
        }
        setVelocity(mx, my, mz);
        moveEntity(mx, moveY, mz);
        if (!mcc$get$onGround(this)) {
            double drag = getDragAir();
            setVelocity(mcc$get$motionX(this) * drag, mcc$get$motionY(this) * drag, mcc$get$motionZ(this) * drag);
        }
    }

    /**
     * On a rail: snaps the cart onto the track of the given shape, gains speed down slopes, turns its speed along the track, lets a
     * rider push it, brakes on an unpowered and boosts on a powered rail, moves it ({@link #moveMinecartOnRail}) and applies drag.
     */
    protected void func_145821_a(int x, int y, int z, double maxSpeed, double slope, Block block, int shape) {
        World world = mcc$world();
        mcc$set$fallDistance(this, 0.0F);
        Vec3 before = func_70489_a(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
        double posX = mcc$get$posX(this);
        double posY = y;
        double posZ = mcc$get$posZ(this);
        boolean boost = false;
        boolean brake = false;
        if (mcc$isPoweredRail(world, block, x, y, z)) {
            boost = mcc$railPowered(world, block, x, y, z);
            brake = !boost;
        }
        if (block instanceof BlockRailBase rail && rail.isPowered()) shape &= 7;
        if (shape < 0 || shape >= matrix.length) shape = 0;
        if (shape >= 2 && shape <= 5) posY = y + 1;
        double mx = mcc$get$motionX(this);
        double my = mcc$get$motionY(this);
        double mz = mcc$get$motionZ(this);
        if (shape == 2) mx -= slope;
        if (shape == 3) mx += slope;
        if (shape == 4) mz += slope;
        if (shape == 5) mz -= slope;
        int[][] ends = matrix[shape];
        double tx = ends[1][0] - ends[0][0];
        double tz = ends[1][2] - ends[0][2];
        double tLen = Math.sqrt(tx * tx + tz * tz);
        if (mx * tx + mz * tz < 0.0D) {
            tx = -tx;
            tz = -tz;
        }
        double speed = Math.min(Math.sqrt(mx * mx + mz * mz), 2.0D);
        mx = speed * tx / tLen;
        mz = speed * tz / tLen;
        Entity rider = mcc$get$riddenByEntity(this);
        if (rider instanceof EntityLivingBase living && EntityLivingBase.mcc$get$moveForward(living) > 0.0F) {
            float riderYaw = mcc$get$rotationYaw(rider);
            double px = -Math.sin(riderYaw * (float) Math.PI / 180.0F);
            double pz = Math.cos(riderYaw * (float) Math.PI / 180.0F);
            if (mx * mx + mz * mz < 0.01D) {
                mx += px * 0.1D;
                mz += pz * 0.1D;
                brake = false;
            }
        }
        if (brake && shouldDoRailFunctions()) {
            if (Math.sqrt(mx * mx + mz * mz) < 0.03D) {
                mx = 0.0D;
                my = 0.0D;
                mz = 0.0D;
            } else {
                mx *= 0.5D;
                my = 0.0D;
                mz *= 0.5D;
            }
        }
        setVelocity(mx, my, mz);
        double x0 = x + 0.5D + ends[0][0] * 0.5D;
        double z0 = z + 0.5D + ends[0][2] * 0.5D;
        double x1 = x + 0.5D + ends[1][0] * 0.5D;
        double z1 = z + 0.5D + ends[1][2] * 0.5D;
        double ddx = x1 - x0;
        double ddz = z1 - z0;
        double along;
        if (ddx == 0.0D) {
            posX = x + 0.5D;
            along = posZ - z;
        } else if (ddz == 0.0D) {
            posZ = z + 0.5D;
            along = posX - x;
        } else {
            along = ((posX - x0) * ddx + (posZ - z0) * ddz) * 2.0D;
        }
        posX = x0 + ddx * along;
        posZ = z0 + ddz * along;
        setPosition(posX, posY + yOffset, posZ);
        moveMinecartOnRail(x, y, z, maxSpeed);
        if (ends[0][1] != 0 && MathHelper.floor_double(mcc$get$posX(this)) - x == ends[0][0] && MathHelper.floor_double(mcc$get$posZ(this)) - z == ends[0][2]) {
            setPosition(mcc$get$posX(this), mcc$get$posY(this) + ends[0][1], mcc$get$posZ(this));
        } else if (ends[1][1] != 0 && MathHelper.floor_double(mcc$get$posX(this)) - x == ends[1][0]
                && MathHelper.floor_double(mcc$get$posZ(this)) - z == ends[1][2]) {
            setPosition(mcc$get$posX(this), mcc$get$posY(this) + ends[1][1], mcc$get$posZ(this));
        }
        applyDrag();
        Vec3 after = func_70489_a(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
        if (after != null && before != null) {
            double gain = (before.yCoord - after.yCoord) * 0.05D;
            mx = mcc$get$motionX(this);
            mz = mcc$get$motionZ(this);
            speed = Math.sqrt(mx * mx + mz * mz);
            if (speed > 0.0D) setVelocity(mx / speed * (speed + gain), mcc$get$motionY(this), mz / speed * (speed + gain));
            setPosition(mcc$get$posX(this), after.yCoord, mcc$get$posZ(this));
        }
        int nx = MathHelper.floor_double(mcc$get$posX(this));
        int nz = MathHelper.floor_double(mcc$get$posZ(this));
        if (nx != x || nz != z) {
            mx = mcc$get$motionX(this);
            mz = mcc$get$motionZ(this);
            speed = Math.sqrt(mx * mx + mz * mz);
            setVelocity(speed * (nx - x), mcc$get$motionY(this), speed * (nz - z));
        }
        if (shouldDoRailFunctions() && block instanceof BlockRailBase rail && block.mcc$vanilla == null) rail.onMinecartPass(world, this, x, y, z);
        if (boost && shouldDoRailFunctions()) {
            mx = mcc$get$motionX(this);
            mz = mcc$get$motionZ(this);
            double s = Math.sqrt(mx * mx + mz * mz);
            if (s > 0.01D) {
                setVelocity(mx + mx / s * 0.06D, mcc$get$motionY(this), mz + mz / s * 0.06D);
            } else if (shape == 1) {
                if (world.getBlock(x - 1, y, z).isNormalCube()) mcc$set$motionX(this, 0.02D);
                else if (world.getBlock(x + 1, y, z).isNormalCube()) mcc$set$motionX(this, -0.02D);
            } else if (shape == 0) {
                if (world.getBlock(x, y, z - 1).isNormalCube()) mcc$set$motionZ(this, 0.02D);
                else if (world.getBlock(x, y, z + 1).isNormalCube()) mcc$set$motionZ(this, -0.02D);
            }
        }
    }

    /** Forge: moves the cart along the rail by its speed, capped to {@code maxSpeed} per axis (three quarters with a rider). */
    public void moveMinecartOnRail(int x, int y, int z, double maxSpeed) {
        double mx = mcc$get$motionX(this);
        double mz = mcc$get$motionZ(this);
        if (mcc$get$riddenByEntity(this) != null) {
            mx *= 0.75D;
            mz *= 0.75D;
        }
        mx = MathHelper.clamp_double(mx, -maxSpeed, maxSpeed);
        mz = MathHelper.clamp_double(mz, -maxSpeed, maxSpeed);
        moveEntity(mx, 0.0D, mz);
    }

    /** Rolling friction for one tick: less with a rider. */
    protected void applyDrag() {
        double drag = mcc$get$riddenByEntity(this) != null ? 0.996999979019165D : 0.9599999785423279D;
        setVelocity(mcc$get$motionX(this) * drag, 0.0D, mcc$get$motionZ(this) * drag);
    }

    /**
     * Client: the point {@code distance} blocks along the rail at the position (renderers draw the cart between the points before
     * and after it), or null off rails.
     */
    public Vec3 func_70495_a(double x, double y, double z, double distance) {
        World world = mcc$world();
        int i = MathHelper.floor_double(x);
        int j = MathHelper.floor_double(y);
        int k = MathHelper.floor_double(z);
        if (mcc$railShape(world, world.getBlock(i, j - 1, k), i, j - 1, k) >= 0) --j;
        int shape = mcc$railShape(world, world.getBlock(i, j, k), i, j, k);
        if (shape < 0 || shape >= matrix.length) return null;
        y = j;
        if (shape >= 2 && shape <= 5) y = j + 1;
        int[][] ends = matrix[shape];
        double dx = ends[1][0] - ends[0][0];
        double dz = ends[1][2] - ends[0][2];
        double len = Math.sqrt(dx * dx + dz * dz);
        dx /= len;
        dz /= len;
        x += dx * distance;
        z += dz * distance;
        if (ends[0][1] != 0 && MathHelper.floor_double(x) - i == ends[0][0] && MathHelper.floor_double(z) - k == ends[0][2]) {
            y += ends[0][1];
        } else if (ends[1][1] != 0 && MathHelper.floor_double(x) - i == ends[1][0] && MathHelper.floor_double(z) - k == ends[1][2]) {
            y += ends[1][1];
        }
        return func_70489_a(x, y, z);
    }

    /** The position on the rail nearest to the point (the cart's posY on it), or null when there is no rail there. */
    public Vec3 func_70489_a(double x, double y, double z) {
        World world = mcc$world();
        int i = MathHelper.floor_double(x);
        int j = MathHelper.floor_double(y);
        int k = MathHelper.floor_double(z);
        if (mcc$railShape(world, world.getBlock(i, j - 1, k), i, j - 1, k) >= 0) --j;
        int shape = mcc$railShape(world, world.getBlock(i, j, k), i, j, k);
        if (shape < 0 || shape >= matrix.length) return null;
        y = j;
        if (shape >= 2 && shape <= 5) y = j + 1;
        int[][] ends = matrix[shape];
        double x0 = i + 0.5D + ends[0][0] * 0.5D;
        double y0 = j + 0.5D + ends[0][1] * 0.5D;
        double z0 = k + 0.5D + ends[0][2] * 0.5D;
        double x1 = i + 0.5D + ends[1][0] * 0.5D;
        double y1 = j + 0.5D + ends[1][1] * 0.5D;
        double z1 = k + 0.5D + ends[1][2] * 0.5D;
        double dx = x1 - x0;
        double dy = (y1 - y0) * 2.0D;
        double dz = z1 - z0;
        double along;
        if (dx == 0.0D) {
            x = i + 0.5D;
            along = z - k;
        } else if (dz == 0.0D) {
            z = k + 0.5D;
            along = x - i;
        } else {
            along = ((x - x0) * dx + (z - z0) * dz) * 2.0D;
        }
        x = x0 + dx * along;
        y = y0 + dy * along;
        z = z0 + dz * along;
        if (dy < 0.0D) ++y;
        if (dy > 0.0D) y += 0.5D;
        return Vec3.createVectorHelper(x, y, z);
    }

    // ------------------------------------------------------------------ rails: mod rails through their hooks, vanilla ones by state

    private static boolean mcc$modRail(Block block) {
        return block instanceof BlockRailBase && block.mcc$vanilla == null;
    }

    private static BlockState mcc$state(World world, int x, int y, int z) {
        var level = world == null ? null : world.mcc$level();
        return level == null ? null : level.getBlockState(new BlockPos(x, y, z));
    }

    /** The shape (0-9) of the rail at the position for this cart, or -1 when there is no rail. */
    private int mcc$railShape(World world, Block block, int x, int y, int z) {
        if (mcc$modRail(block)) return ((BlockRailBase) block).getBasicRailMetadata(world, this, x, y, z);
        BlockState state = mcc$state(world, x, y, z);
        if (state == null || !(state.getBlock() instanceof BaseRailBlock rail)) return -1;
        return rail.getRailDirection(state, world.mcc$level(), new BlockPos(x, y, z), null).ordinal();
    }

    private float mcc$railMaxSpeed(World world, Block block, int x, int y, int z) {
        if (mcc$modRail(block)) return ((BlockRailBase) block).getRailMaxSpeed(world, this, x, y, z);
        return 0.4F;
    }

    /** A golden (powered) rail: boosts when powered, brakes when not. */
    private static boolean mcc$isPoweredRail(World world, Block block, int x, int y, int z) {
        if (mcc$modRail(block)) return false;
        BlockState state = mcc$state(world, x, y, z);
        return state != null && state.getBlock() instanceof PoweredRailBlock p && !p.isActivatorRail();
    }

    private static boolean mcc$isActivatorRail(World world, Block block, int x, int y, int z) {
        if (mcc$modRail(block)) return false;
        BlockState state = mcc$state(world, x, y, z);
        return state != null && state.getBlock() instanceof PoweredRailBlock p && p.isActivatorRail();
    }

    private static boolean mcc$railPowered(World world, Block block, int x, int y, int z) {
        BlockState state = mcc$state(world, x, y, z);
        return state != null && state.hasProperty(PoweredRailBlock.POWERED) && state.getValue(PoweredRailBlock.POWERED);
    }

    // ------------------------------------------------------------------ collisions

    /**
     * Pushes another entity and this cart apart; a mob it rolls into gets in (a rideable cart without a rider); carts pass on their
     * speed (a furnace cart pushes the others). Posts the MinecartCollisionEvent; a collision handler replaces all of it.
     */
    @Override
    public void applyEntityCollision(Entity other) {
        var cart = mcc$cart();
        if (cart != null) {
            if (other != null && other.mcc$entity != null) cart.push(other.mcc$entity);
            return;
        }
        if (other == null) return;
        legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartCollisionEvent(this, other));
        if (collisionHandler != null) {
            collisionHandler.onEntityCollision(this, other);
            return;
        }
        if (mcc$world().isRemote) return;
        Entity rider = mcc$get$riddenByEntity(this);
        if (other == rider) return;
        double mx = mcc$get$motionX(this);
        double mz = mcc$get$motionZ(this);
        if (other instanceof EntityLivingBase && !(other instanceof EntityPlayer)
                && !(other.mcc$entity instanceof net.minecraft.world.entity.animal.IronGolem) && canBeRidden() && mx * mx + mz * mz > 0.01D
                && rider == null && mcc$get$ridingEntity(other) == null) {
            other.mountEntity(this);
        }
        double dx = mcc$get$posX(other) - mcc$get$posX(this);
        double dz = mcc$get$posZ(other) - mcc$get$posZ(this);
        double dist = dx * dx + dz * dz;
        if (dist < 9.999999747378752E-5D) return;
        dist = MathHelper.sqrt_double(dist);
        dx /= dist;
        dz /= dist;
        double scale = Math.min(1.0D / dist, 1.0D);
        dx *= scale * 0.10000000149011612D * (1.0F - entityCollisionReduction) * 0.5D;
        dz *= scale * 0.10000000149011612D * (1.0F - entityCollisionReduction) * 0.5D;
        if (other instanceof EntityMinecart otherCart) {
            Vec3 toOther = Vec3.createVectorHelper(mcc$get$posX(other) - mcc$get$posX(this), 0.0D, mcc$get$posZ(other) - mcc$get$posZ(this)).normalize();
            float yaw = mcc$get$rotationYaw(this) * (float) Math.PI / 180.0F;
            Vec3 facing = Vec3.createVectorHelper(MathHelper.cos(yaw), 0.0D, MathHelper.sin(yaw)).normalize();
            if (Math.abs(toOther.dotProduct(facing)) < 0.800000011920929D) return;
            double omx = mcc$get$motionX(other);
            double omz = mcc$get$motionZ(other);
            double sx = omx + mx;
            double sz = omz + mz;
            if (otherCart.isPoweredCart() && !isPoweredCart()) {
                setVelocity(mx * 0.20000000298023224D, mcc$get$motionY(this), mz * 0.20000000298023224D);
                addVelocity(omx - dx, 0.0D, omz - dz);
                setOtherVelocity(other, omx * 0.949999988079071D, omz * 0.949999988079071D);
            } else if (!otherCart.isPoweredCart() && isPoweredCart()) {
                setOtherVelocity(other, omx * 0.20000000298023224D, omz * 0.20000000298023224D);
                other.addVelocity(mx + dx, 0.0D, mz + dz);
                setVelocity(mx * 0.949999988079071D, mcc$get$motionY(this), mz * 0.949999988079071D);
            } else {
                sx /= 2.0D;
                sz /= 2.0D;
                setVelocity(mx * 0.20000000298023224D, mcc$get$motionY(this), mz * 0.20000000298023224D);
                addVelocity(sx - dx, 0.0D, sz - dz);
                setOtherVelocity(other, omx * 0.20000000298023224D, omz * 0.20000000298023224D);
                other.addVelocity(sx + dx, 0.0D, sz + dz);
            }
        } else {
            addVelocity(-dx, 0.0D, -dz);
            other.addVelocity(dx / 4.0D, 0.0D, dz / 4.0D);
        }
    }

    private static void setOtherVelocity(Entity other, double mx, double mz) {
        mcc$set$motionX(other, mx);
        mcc$set$motionZ(other, mz);
    }

    /** Client: the position from the server, reached over the next ticks by {@link #onUpdate} (a vanilla cart interpolates itself). */
    @Override
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {
        if (mcc$cart() != null || mcc$entity == null) {
            super.setPositionAndRotation2(x, y, z, yaw, pitch, increments);
            return;
        }
        minecartX = x;
        minecartY = y;
        minecartZ = z;
        minecartYaw = yaw;
        minecartPitch = pitch;
        turnProgress = increments + 2;
    }

    // ------------------------------------------------------------------ the block shown inside the cart

    public Block getDisplayTile() {
        var cart = mcc$cart();
        if (cart != null) return BlockBridge.legacyBlock(cart.getDisplayBlockState());
        if (!hasDisplayTile()) return getDefaultDisplayTile();
        return Block.getBlockById(dataWatcher.getWatchableObjectInt(20) & 0xFFFF);
    }

    public Block getDefaultDisplayTile() {
        return Blocks.air;
    }

    public int getDisplayTileData() {
        var cart = mcc$cart();
        if (cart != null) return BlockBridge.legacyMeta(cart.getDisplayBlockState());
        return hasDisplayTile() ? dataWatcher.getWatchableObjectInt(20) >> 16 : getDefaultDisplayTileData();
    }

    public int getDefaultDisplayTileData() {
        return 0;
    }

    public int getDisplayTileOffset() {
        var cart = mcc$cart();
        if (cart != null) return cart.getDisplayOffset();
        return hasDisplayTile() ? dataWatcher.getWatchableObjectInt(21) : getDefaultDisplayTileOffset();
    }

    public int getDefaultDisplayTileOffset() {
        return 6;
    }

    /** Shows the block with this legacy id. */
    public void func_145819_k(int blockId) {
        var cart = mcc$cart();
        if (cart != null) {
            cart.setDisplayBlockState(BlockBridge.toModern(Block.getBlockById(blockId), getDisplayTileData()));
            return;
        }
        dataWatcher.updateObject(20, Integer.valueOf(blockId & 0xFFFF | getDisplayTileData() << 16));
        setHasDisplayTile(true);
    }

    public void setDisplayTileData(int data) {
        var cart = mcc$cart();
        if (cart != null) {
            cart.setDisplayBlockState(BlockBridge.toModern(getDisplayTile(), data));
            return;
        }
        dataWatcher.updateObject(20, Integer.valueOf(Block.getIdFromBlock(getDisplayTile()) & 0xFFFF | data << 16));
        setHasDisplayTile(true);
    }

    public void setDisplayTileOffset(int offset) {
        var cart = mcc$cart();
        if (cart != null) {
            cart.setDisplayOffset(offset);
            return;
        }
        dataWatcher.updateObject(21, Integer.valueOf(offset));
        setHasDisplayTile(true);
    }

    public boolean hasDisplayTile() {
        var cart = mcc$cart();
        return cart != null ? cart.hasCustomDisplay() : dataWatcher.getWatchableObjectByte(22) == 1;
    }

    public void setHasDisplayTile(boolean shown) {
        var cart = mcc$cart();
        if (cart != null) cart.setCustomDisplay(shown);
        else dataWatcher.updateObject(22, Byte.valueOf((byte) (shown ? 1 : 0)));
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        if (tag.getBoolean("CustomDisplayTile")) {
            func_145819_k(tag.getInteger("DisplayTile"));
            setDisplayTileData(tag.getInteger("DisplayData"));
            setDisplayTileOffset(tag.getInteger("DisplayOffset"));
        }
        if (tag.hasKey("CustomName")) setMinecartName(tag.getString("CustomName"));
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        if (hasDisplayTile()) {
            tag.setBoolean("CustomDisplayTile", true);
            tag.setInteger("DisplayTile", getDisplayTile() == null ? 0 : Block.getIdFromBlock(getDisplayTile()));
            tag.setInteger("DisplayData", getDisplayTileData());
            tag.setInteger("DisplayOffset", getDisplayTileOffset());
        }
        if (hasCustomName) tag.setString("CustomName", entityName);
    }
}
