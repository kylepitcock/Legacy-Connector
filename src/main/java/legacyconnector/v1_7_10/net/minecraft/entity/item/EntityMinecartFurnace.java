package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.vehicle.MinecartFurnace;

/**
 * The powered minecart: coal gives it 3600 ticks of fuel, during which it pushes itself the way the player pushed it (pushX/pushZ).
 * Constructing this exact class makes a vanilla furnace cart, whose push direction is the modern cart's.
 */
public class EntityMinecartFurnace extends EntityMinecart {
    private int fuel;

    /** Phantom: the push direction (redirected to {@code mcc$get$pushX}). */
    @Deprecated
    public double pushX;
    private double mcc$local_pushX;
    /** Phantom: the push direction (redirected to {@code mcc$get$pushZ}). */
    @Deprecated
    public double pushZ;
    private double mcc$local_pushZ;

    public EntityMinecartFurnace(World world) {
        super(world);
    }

    public EntityMinecartFurnace(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    private MinecartFurnace mcc$furnace() {
        return mcc$entity instanceof MinecartFurnace f ? f : null;
    }

    public static double mcc$get$pushX(EntityMinecartFurnace e) {
        var f = e.mcc$furnace();
        return f != null ? f.xPush : e.mcc$local_pushX;
    }

    public static void mcc$set$pushX(EntityMinecartFurnace e, double v) {
        var f = e.mcc$furnace();
        if (f != null) f.xPush = v;
        else e.mcc$local_pushX = v;
    }

    public static double mcc$get$pushZ(EntityMinecartFurnace e) {
        var f = e.mcc$furnace();
        return f != null ? f.zPush : e.mcc$local_pushZ;
    }

    public static void mcc$set$pushZ(EntityMinecartFurnace e, double v) {
        var f = e.mcc$furnace();
        if (f != null) f.zPush = v;
        else e.mcc$local_pushZ = v;
    }

    @Override
    public int getMinecartType() {
        return 2;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, Byte.valueOf((byte) 0));
    }

    /** Burns fuel; out of fuel it stops pushing. Smokes while lit. */
    @Override
    public void onUpdate() {
        super.onUpdate();
        if (mcc$furnace() != null) return;
        if (fuel > 0) --fuel;
        if (fuel <= 0) {
            mcc$set$pushX(this, 0.0D);
            mcc$set$pushZ(this, 0.0D);
        }
        setMinecartPowered(fuel > 0);
        if (isMinecartPowered() && rand.nextInt(4) == 0) {
            mcc$world().spawnParticle("largesmoke", mcc$get$posX(this), mcc$get$posY(this) + 0.8D, mcc$get$posZ(this), 0.0D, 0.0D, 0.0D);
        }
    }

    /** Breaks the cart; the furnace drops too unless an explosion broke it. */
    @Override
    public void killMinecart(DamageSource source) {
        super.killMinecart(source);
        if (source == null || !source.isExplosion()) entityDropItem(new ItemStack(Blocks.furnace, 1), 0.0F);
    }

    /** On a rail the push follows the cart's direction of travel (and is dropped when the cart is going the other way). */
    @Override
    protected void func_145821_a(int x, int y, int z, double maxSpeed, double slope, Block block, int shape) {
        super.func_145821_a(x, y, z, maxSpeed, slope, block, shape);
        double px = mcc$get$pushX(this);
        double pz = mcc$get$pushZ(this);
        double mx = mcc$get$motionX(this);
        double mz = mcc$get$motionZ(this);
        double len = px * px + pz * pz;
        if (len > 1.0E-4D && mx * mx + mz * mz > 0.001D) {
            len = Math.sqrt(len);
            px /= len;
            pz /= len;
            if (px * mx + pz * mz < 0.0D) {
                px = 0.0D;
                pz = 0.0D;
            } else {
                px = mx;
                pz = mz;
            }
            mcc$set$pushX(this, px);
            mcc$set$pushZ(this, pz);
        }
    }

    /** Pushing: slows by 0.8 then adds 0.05 along the push; unpushed it rolls on with 0.98 (then the base drag). */
    @Override
    protected void applyDrag() {
        double px = mcc$get$pushX(this);
        double pz = mcc$get$pushZ(this);
        double len = px * px + pz * pz;
        if (len > 1.0E-4D) {
            len = Math.sqrt(len);
            px /= len;
            pz /= len;
            mcc$set$pushX(this, px);
            mcc$set$pushZ(this, pz);
            setVelocity(mcc$get$motionX(this) * 0.800000011920929D + px * 0.05D, 0.0D, mcc$get$motionZ(this) * 0.800000011920929D + pz * 0.05D);
        } else {
            setVelocity(mcc$get$motionX(this) * 0.9800000190734863D, 0.0D, mcc$get$motionZ(this) * 0.9800000190734863D);
        }
        super.applyDrag();
    }

    /** Coal adds 3600 ticks of fuel; any click sets the push away from the player. */
    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartInteractEvent(this, player))) {
            return true;
        }
        if (mcc$furnace() != null) return super.interactFirst(player);
        ItemStack held = player.inventory.getCurrentItem();
        if (held != null && held.getItem() == Items.coal) {
            if (!PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) {
                held.mcc$setCount(held.mcc$count() - 1);
                if (held.mcc$count() <= 0) player.inventory.setInventorySlotContents(legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer.mcc$get$currentItem(player.inventory), null);
            }
            fuel += 3600;
        }
        mcc$set$pushX(this, mcc$get$posX(this) - mcc$get$posX(player));
        mcc$set$pushZ(this, mcc$get$posZ(this) - mcc$get$posZ(player));
        return true;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setDouble("PushX", mcc$get$pushX(this));
        tag.setDouble("PushZ", mcc$get$pushZ(this));
        tag.setShort("Fuel", (short) fuel);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        mcc$set$pushX(this, tag.getDouble("PushX"));
        mcc$set$pushZ(this, tag.getDouble("PushZ"));
        fuel = tag.getShort("Fuel");
    }

    /** Whether it burns (watcher slot 16; a vanilla furnace cart: whether it is still pushing). */
    protected boolean isMinecartPowered() {
        var f = mcc$furnace();
        if (f != null) return f.xPush != 0.0D || f.zPush != 0.0D;
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    protected void setMinecartPowered(boolean powered) {
        if (mcc$furnace() != null) return;
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, Byte.valueOf((byte) (powered ? flags | 1 : flags & -2)));
    }

    @Override
    public Block getDefaultDisplayTile() {
        return Blocks.lit_furnace;
    }

    @Override
    public int getDefaultDisplayTileData() {
        return 2;
    }
}
