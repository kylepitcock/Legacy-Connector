package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.IMinecartCollisionHandler;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;

/**
 * Base of minecarts (rideable 0, chest 1, furnace 2, TNT 3, spawner 4, hopper 5, command block 6) and of mods' carts. A mod's cart
 * keeps 1.7.10's watcher slots (17 rolling amplitude, 18 rolling direction, 19 damage, 20 display tile, 21 its offset, 22 whether
 * it is shown); a view of a vanilla cart reads and writes the modern cart's own hurt time, hurt direction, damage and display block.
 */
public abstract class EntityMinecart extends Entity {
    private String entityName;
    private boolean hasCustomName;
    /** Forge: the collision handler every cart defers to (installed as NeoForge's, which still has the hook). */
    private static IMinecartCollisionHandler collisionHandler;
    private float currentSpeedRail = getMaxCartSpeedOnRail();

    public EntityMinecart(World world) {
        super(world);
        setSize(0.98F, 0.7F);
        yOffset = 0.35F;
    }

    public EntityMinecart(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y + yOffset, z);
    }

    /** View of a modern vanilla cart. */
    protected EntityMinecart(AbstractMinecart modern) {
        super(modern);
    }

    private AbstractMinecart mcc$cart() {
        return mcc$entity instanceof AbstractMinecart c ? c : null;
    }

    /** The cart type, from {@link #func_94087_l} in 1.7.10; subclasses return their own. */
    public abstract int getMinecartType();

    public void setMinecartName(String name) {
        this.entityName = name;
        this.hasCustomName = name != null && !name.isEmpty();
        var cart = mcc$cart();
        if (cart != null) cart.setCustomName(hasCustomName ? net.minecraft.network.chat.Component.literal(name) : null);
    }

    /** Carts can be hit and pushed while they exist (a vanilla cart answers for itself). */
    @Override
    public boolean canBeCollidedWith() {
        return mcc$cart() != null ? super.canBeCollidedWith() : isEntityAlive();
    }

    @Override
    public boolean canBePushed() {
        return mcc$cart() != null ? super.canBePushed() : true;
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
    protected void entityInit() {
        dataWatcher.addObject(17, Integer.valueOf(0));
        dataWatcher.addObject(18, Integer.valueOf(1));
        dataWatcher.addObject(19, Float.valueOf(0.0F));
        dataWatcher.addObject(20, Integer.valueOf(0));
        dataWatcher.addObject(21, Integer.valueOf(6));
        dataWatcher.addObject(22, Byte.valueOf((byte) 0));
    }

    // ------------------------------------------------------------------ Forge: collision handler and rail speed

    public static IMinecartCollisionHandler getCollisionHandler() {
        return collisionHandler;
    }

    public static void setCollisionHandler(IMinecartCollisionHandler handler) {
        collisionHandler = handler;
        org.kylepitcock.legacyconnector.v1_7_10.bridge.MinecartCollisionBridge.install(handler);
    }

    @Override
    public void applyEntityCollision(Entity other) {
        if (collisionHandler != null && mcc$cart() == null) {
            collisionHandler.onEntityCollision(this, other);
            return;
        }
        super.applyEntityCollision(other);
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
        ItemStack stack = new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.minecart, 1);
        if (entityName != null) stack.setStackDisplayName(entityName);
        entityDropItem(stack, 0.0F);
    }

    /** Client: the wobble shown when the server reports a hit. */
    public void performHurtAnimation() {
        setRollingDirection(-getRollingDirection());
        setRollingAmplitude(10);
        setDamage(getDamage() + getDamage() * 10.0F);
    }

    /** A legacy cart's wobble and damage wear off by one per tick (a vanilla cart's own tick does this). */
    @Override
    public void onUpdate() {
        if (mcc$cart() == null) {
            if (getRollingAmplitude() > 0) setRollingAmplitude(getRollingAmplitude() - 1);
            if (getDamage() > 0.0F) setDamage(getDamage() - 1.0F);
        }
        super.onUpdate();
    }

    /** Rolling friction for one tick: less with a rider. */
    protected void applyDrag() {
        double drag = mcc$get$riddenByEntity(this) != null ? 0.997D : 0.96D;
        mcc$set$motionX(this, mcc$get$motionX(this) * drag);
        mcc$set$motionY(this, 0.0D);
        mcc$set$motionZ(this, mcc$get$motionZ(this) * drag);
    }

    // ------------------------------------------------------------------ the block shown inside the cart

    public Block getDisplayTile() {
        var cart = mcc$cart();
        if (cart != null) return BlockBridge.legacyBlock(cart.getDisplayBlockState());
        if (!hasDisplayTile()) return getDefaultDisplayTile();
        return Block.getBlockById(dataWatcher.getWatchableObjectInt(20) & 0xFFFF);
    }

    public Block getDefaultDisplayTile() {
        return legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
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
