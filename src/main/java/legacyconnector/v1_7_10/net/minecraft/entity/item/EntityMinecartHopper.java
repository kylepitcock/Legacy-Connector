package legacyconnector.v1_7_10.net.minecraft.entity.item;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.vehicle.MinecartHopper;

/**
 * The hopper minecart (5 slots): every 4 ticks it pulls one item out of the inventory above it or picks up an item lying on it,
 * unless a powered activator rail locked it. Constructing this exact class makes a vanilla hopper cart.
 */
public class EntityMinecartHopper extends EntityMinecartContainer {
    private boolean isBlocked = true;
    private int transferTicker = -1;

    public EntityMinecartHopper(World world) {
        super(world);
    }

    public EntityMinecartHopper(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    private MinecartHopper mcc$hopper() {
        return mcc$entity instanceof MinecartHopper h ? h : null;
    }

    @Override
    public int getMinecartType() {
        return 5;
    }

    @Override
    public Block getDefaultDisplayTile() {
        return Blocks.hopper;
    }

    @Override
    public int getDefaultDisplayTileOffset() {
        return 1;
    }

    @Override
    public int getSizeInventory() {
        return 5;
    }

    /** Opens the hopper window (unless a MinecartInteractEvent handler took the click). */
    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartInteractEvent(this, player))) {
            return true;
        }
        if (!mcc$world().isRemote) player.displayGUIHopperMinecart(this);
        return true;
    }

    /** A powered activator rail locks the hopper, an unpowered one unlocks it. */
    @Override
    public void onActivatorRailPass(int x, int y, int z, boolean powered) {
        boolean unlocked = !powered;
        if (unlocked != getBlocked()) setBlocked(unlocked);
    }

    /** Whether it moves items (true unless locked). */
    public boolean getBlocked() {
        var h = mcc$hopper();
        return h != null ? h.isEnabled() : isBlocked;
    }

    public void setBlocked(boolean enabled) {
        var h = mcc$hopper();
        if (h != null) h.setEnabled(enabled);
        else isBlocked = enabled;
    }

    public World getWorldObj() {
        return mcc$world();
    }

    public double getXPos() {
        return mcc$get$posX(this);
    }

    public double getYPos() {
        return mcc$get$posY(this);
    }

    public double getZPos() {
        return mcc$get$posZ(this);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (mcc$hopper() != null || mcc$entity == null) return;
        if (!mcc$world().isRemote && isEntityAlive() && getBlocked()) {
            --transferTicker;
            if (!canTransfer()) {
                setTransferTicker(0);
                if (func_96112_aD()) {
                    setTransferTicker(4);
                    markDirty();
                }
            }
        }
    }

    /** Pulls one item from the inventory above, or picks up an item lying here; true when an item was pulled. */
    public boolean func_96112_aD() {
        World world = mcc$world();
        IInventory above = mcc$inventoryAbove(world);
        if (above != null) return mcc$pullFrom(above);
        List<?> items = world.getEntitiesWithinAABB(EntityItem.class, mcc$get$boundingBox(this).expand(0.25D, 0.0D, 0.25D));
        for (Object o : items) {
            if (o instanceof EntityItem item && item.isEntityAlive()) {
                mcc$pickUp(item);
                break;
            }
        }
        return false;
    }

    private IInventory mcc$inventoryAbove(World world) {
        int x = MathHelper.floor_double(getXPos());
        int y = MathHelper.floor_double(getYPos() + 1.0D);
        int z = MathHelper.floor_double(getZPos());
        return world.getTileEntity(x, y, z) instanceof IInventory inv ? inv : null;
    }

    private boolean mcc$pullFrom(IInventory from) {
        int[] slots;
        ISidedInventory sided = from instanceof ISidedInventory s ? s : null;
        if (sided != null) {
            slots = sided.getAccessibleSlotsFromSide(0);
        } else {
            slots = new int[from.getSizeInventory()];
            for (int i = 0; i < slots.length; i++) slots[i] = i;
        }
        for (int slot : slots) {
            ItemStack stack = from.getStackInSlot(slot);
            if (stack == null || stack.mcc$count() <= 0 || sided != null && !sided.canExtractItem(slot, stack, 0)) continue;
            ItemStack one = stack.copy();
            one.mcc$setCount(1);
            if (mcc$insert(one)) {
                from.decrStackSize(slot, 1);
                from.markDirty();
                return true;
            }
        }
        return false;
    }

    private void mcc$pickUp(EntityItem item) {
        ItemStack stack = item.getEntityItem();
        if (stack == null) return;
        ItemStack rest = stack.copy();
        while (rest.mcc$count() > 0) {
            ItemStack one = rest.copy();
            one.mcc$setCount(1);
            if (!mcc$insert(one)) break;
            rest.mcc$setCount(rest.mcc$count() - 1);
        }
        if (rest.mcc$count() <= 0) item.setDead();
        else item.setEntityItemStack(rest);
    }

    /** Puts one item into the first slot that takes it. */
    private boolean mcc$insert(ItemStack one) {
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack here = getStackInSlot(i);
            if (here == null) {
                setInventorySlotContents(i, one);
                return true;
            }
            if (here.isItemEqual(one) && ItemStack.areItemStackTagsEqual(here, one) && here.mcc$count() < Math.min(here.getMaxStackSize(), getInventoryStackLimit())) {
                here.mcc$setCount(here.mcc$count() + 1);
                return true;
            }
        }
        return false;
    }

    /** Breaks the cart; the hopper drops too. */
    @Override
    public void killMinecart(DamageSource source) {
        super.killMinecart(source);
        dropItemWithOffset(Item.getItemFromBlock(Blocks.hopper), 1, 0.0F);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("TransferCooldown", transferTicker);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        transferTicker = tag.getInteger("TransferCooldown");
    }

    public void setTransferTicker(int ticks) {
        transferTicker = ticks;
    }

    public boolean canTransfer() {
        return transferTicker > 0;
    }
}
