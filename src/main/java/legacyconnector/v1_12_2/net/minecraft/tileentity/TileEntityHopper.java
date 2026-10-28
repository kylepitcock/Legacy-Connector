package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.inventory.ISidedInventory;
import legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ITickable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's hopper: five slots, every eight ticks one item pushed into the inventory it faces and one pulled from the inventory above.
 * Mods' hoppers extend it; a hopper of the game is seen through a view over its slots and cooldown.
 */
public class TileEntityHopper extends TileEntityLockableLoot implements IHopper, ITickable, org.kylepitcock.legacyconnector.v1_12_2.bridge.TileViews1122.Refreshable {
    private NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
    private int transferCooldown = -1;
    private long tickedGameTime;
    private final net.minecraft.world.level.block.entity.HopperBlockEntity mcc$hopper;

    public TileEntityHopper() {
        this.mcc$hopper = null;
    }

    /** A view of a game hopper. */
    public TileEntityHopper(net.minecraft.world.level.block.entity.HopperBlockEntity hopper) {
        this.mcc$hopper = hopper;
        this.inventory = new org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernStackList1122(hopper);
    }

    private static java.lang.reflect.Field cooldownField;

    private static java.lang.reflect.Field cooldown() {
        if (cooldownField == null) {
            try {
                var f = net.minecraft.world.level.block.entity.HopperBlockEntity.class.getDeclaredField("cooldownTime");
                f.setAccessible(true);
                cooldownField = f;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return cooldownField;
    }

    @Override
    public void mcc$refresh() {
        if (mcc$hopper == null) return;
        var f = cooldown();
        try {
            if (f != null) transferCooldown = f.getInt(mcc$hopper);
        } catch (IllegalAccessException e) {
            // keep the last value
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (mcc$hopper == null) inventory = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        if (!checkLootAndRead(compound)) ItemStackHelper.loadAllItems(compound, inventory);
        if (compound.hasKey("CustomName", 8)) customName = compound.getString("CustomName");
        transferCooldown = compound.getInteger("TransferCooldown");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (!checkLootAndWrite(compound)) ItemStackHelper.saveAllItems(compound, inventory);
        compound.setInteger("TransferCooldown", transferCooldown);
        if (hasCustomName()) compound.setString("CustomName", customName);
        return compound;
    }

    @Override
    public int getSizeInventory() {
        return inventory.size();
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        fillWithLoot(null);
        return ItemStackHelper.getAndSplit(getItems(), index, count);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        fillWithLoot(null);
        getItems().set(index, stack);
        if (stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
    }

    @Override
    public String getName() {
        return hasCustomName() ? customName : "container.hopper";
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void update() {
        if (mcc$hopper != null || world == null || world.isRemote) return;
        --transferCooldown;
        tickedGameTime = world.getTotalWorldTime();
        if (!isOnTransferCooldown()) {
            setTransferCooldown(0);
            updateHopper();
        }
    }

    protected boolean updateHopper() {
        if (world == null || world.isRemote || isOnTransferCooldown() || !isEnabled()) return false;
        boolean moved = false;
        if (!isInventoryEmpty()) moved = transferItemsOut();
        if (!isFull()) moved = pullItems(this) || moved;
        if (moved) {
            setTransferCooldown(8);
            markDirty();
            return true;
        }
        return false;
    }

    /** Enabled unless powered (the block's ENABLED bit, metadata 8). */
    private boolean isEnabled() {
        return (getBlockMetadata() & 8) == 0;
    }

    private EnumFacing facing() {
        return EnumFacing.byIndex(getBlockMetadata() & 7);
    }

    private boolean isInventoryEmpty() {
        for (ItemStack s : inventory) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public boolean isEmpty() {
        return isInventoryEmpty();
    }

    private boolean isFull() {
        for (ItemStack s : inventory) if (s.isEmpty() || s.getCount() != s.getMaxStackSize()) return false;
        return true;
    }

    private boolean transferItemsOut() {
        IInventory target = getInventoryForHopperTransfer();
        if (target == null) return false;
        EnumFacing side = facing().getOpposite();
        if (isInventoryFull(target, side)) return false;
        for (int i = 0; i < getSizeInventory(); ++i) {
            if (!getStackInSlot(i).isEmpty()) {
                ItemStack copy = getStackInSlot(i).copy();
                ItemStack rest = putStackInInventoryAllSlots(this, target, decrStackSize(i, 1), side);
                if (rest.isEmpty()) {
                    target.markDirty();
                    return true;
                }
                setInventorySlotContents(i, copy);
            }
        }
        return false;
    }

    private static boolean isInventoryFull(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory sided) {
            for (int k : sided.getSlotsForFace(side)) {
                ItemStack s = sided.getStackInSlot(k);
                if (s.isEmpty() || s.getCount() != s.getMaxStackSize()) return false;
            }
        } else {
            for (int j = 0; j < inventoryIn.getSizeInventory(); ++j) {
                ItemStack s = inventoryIn.getStackInSlot(j);
                if (s.isEmpty() || s.getCount() != s.getMaxStackSize()) return false;
            }
        }
        return true;
    }

    private static boolean isInventoryEmpty(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory sided) {
            for (int i : sided.getSlotsForFace(side)) if (!sided.getStackInSlot(i).isEmpty()) return false;
        } else {
            for (int k = 0; k < inventoryIn.getSizeInventory(); ++k) if (!inventoryIn.getStackInSlot(k).isEmpty()) return false;
        }
        return true;
    }

    /** Pulls one item from the inventory above the hopper (dropped items above are left to the game's hoppers). */
    public static boolean pullItems(IHopper hopper) {
        IInventory source = getSourceInventory(hopper);
        if (source == null) return false;
        EnumFacing side = EnumFacing.DOWN;
        if (isInventoryEmpty(source, side)) return false;
        if (source instanceof ISidedInventory sided) {
            for (int i : sided.getSlotsForFace(side)) if (pullItemFromSlot(hopper, source, i, side)) return true;
        } else {
            for (int j = 0; j < source.getSizeInventory(); ++j) if (pullItemFromSlot(hopper, source, j, side)) return true;
        }
        return false;
    }

    private static boolean pullItemFromSlot(IHopper hopper, IInventory inventoryIn, int index, EnumFacing direction) {
        ItemStack stack = inventoryIn.getStackInSlot(index);
        if (!stack.isEmpty() && canExtractItemFromSlot(inventoryIn, stack, index, direction)) {
            ItemStack copy = stack.copy();
            ItemStack rest = putStackInInventoryAllSlots(inventoryIn, hopper, inventoryIn.decrStackSize(index, 1), null);
            if (rest.isEmpty()) {
                inventoryIn.markDirty();
                return true;
            }
            inventoryIn.setInventorySlotContents(index, copy);
        }
        return false;
    }

    /** Puts a stack into an inventory, filling matching stacks and empty slots the side allows; returns what did not fit. */
    public static ItemStack putStackInInventoryAllSlots(IInventory source, IInventory destination, ItemStack stack, EnumFacing direction) {
        if (destination instanceof ISidedInventory sided && direction != null) {
            int[] slots = sided.getSlotsForFace(direction);
            for (int k = 0; k < slots.length && !stack.isEmpty(); ++k) stack = insertStack(source, destination, stack, slots[k], direction);
        } else {
            int i = destination.getSizeInventory();
            for (int j = 0; j < i && !stack.isEmpty(); ++j) stack = insertStack(source, destination, stack, j, direction);
        }
        return stack;
    }

    private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side) {
        if (!inventoryIn.isItemValidForSlot(index, stack)) return false;
        return !(inventoryIn instanceof ISidedInventory sided) || sided.canInsertItem(index, stack, side);
    }

    private static boolean canExtractItemFromSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side) {
        return !(inventoryIn instanceof ISidedInventory sided) || sided.canExtractItem(index, stack, side);
    }

    private static ItemStack insertStack(IInventory source, IInventory destination, ItemStack stack, int index, EnumFacing direction) {
        ItemStack inSlot = destination.getStackInSlot(index);
        if (canInsertItemInSlot(destination, stack, index, direction)) {
            boolean moved = false;
            boolean wasEmpty = destination.isEmpty();
            if (inSlot.isEmpty()) {
                destination.setInventorySlotContents(index, stack);
                stack = ItemStack.EMPTY;
                moved = true;
            } else if (canCombine(inSlot, stack)) {
                int i = stack.getMaxStackSize() - inSlot.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.shrink(j);
                inSlot.grow(j);
                moved = j > 0;
            }
            if (moved) {
                if (wasEmpty && destination instanceof TileEntityHopper hopper && !hopper.mayTransfer()) {
                    int k = 0;
                    if (source instanceof TileEntityHopper from && hopper.tickedGameTime >= from.tickedGameTime) k = 1;
                    hopper.setTransferCooldown(8 - k);
                }
                destination.markDirty();
            }
        }
        return stack;
    }

    private IInventory getInventoryForHopperTransfer() {
        EnumFacing f = facing();
        return getInventoryAtPosition(world, getXPos() + (double) f.getXOffset(), getYPos() + (double) f.getYOffset(), getZPos() + (double) f.getZOffset());
    }

    public static IInventory getSourceInventory(IHopper hopper) {
        return getInventoryAtPosition(hopper.getWorld(), hopper.getXPos(), hopper.getYPos() + 1.0D, hopper.getZPos());
    }

    /** The inventory of the tile entity at a point (a chest, a furnace, a mod's machine), or null. */
    public static IInventory getInventoryAtPosition(World worldIn, double x, double y, double z) {
        if (worldIn == null) return null;
        BlockPos at = new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        TileEntity te = worldIn.getTileEntity(at);
        return te instanceof IInventory inv ? inv : null;
    }

    private static boolean canCombine(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() != stack2.getItem()) return false;
        if (stack1.getMetadata() != stack2.getMetadata()) return false;
        if (stack1.getCount() > stack1.getMaxStackSize()) return false;
        return ItemStack.areItemStackTagsEqual(stack1, stack2);
    }

    @Override
    public double getXPos() {
        return (double) pos.getX() + 0.5D;
    }

    @Override
    public double getYPos() {
        return (double) pos.getY() + 0.5D;
    }

    @Override
    public double getZPos() {
        return (double) pos.getZ() + 0.5D;
    }

    public void setTransferCooldown(int ticks) {
        transferCooldown = ticks;
        if (mcc$hopper != null) {
            var f = cooldown();
            try {
                if (f != null) f.setInt(mcc$hopper, ticks);
            } catch (IllegalAccessException e) {
                // the game hopper keeps its own
            }
        }
    }

    private boolean isOnTransferCooldown() {
        return transferCooldown > 0;
    }

    public boolean mayTransfer() {
        return transferCooldown > 8;
    }

    @Override
    public String getGuiID() {
        return "minecraft:hopper";
    }

    /** 1.12's ContainerHopper is not part of this layer yet. */
    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return null;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inventory;
    }

    /** Forge: the hopper's item handler respects its transfer cooldown. */
    @Override
    protected legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler createUnSidedHandler() {
        return new legacyconnector.v1_12_2.net.minecraftforge.items.VanillaHopperItemHandler(this);
    }

    public long getLastUpdateTime() {
        return tickedGameTime;
    }
}
