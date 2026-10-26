package legacyconnector.v1_7_10.net.minecraft.tileentity;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * The hopper's tile entity: five slots and a transfer cooldown. A view of a vanilla hopper reads and writes the modern hopper (its
 * slots and cooldown); the static helpers are 1.7.10's item-transfer rules, which mods use to push items into any inventory.
 */
public class TileEntityHopper extends TileEntity implements IHopper {
    private ItemStack[] field_145900_a = new ItemStack[5];
    private String field_145902_i;
    private int transferCooldown = -1;
    private final net.minecraft.world.level.block.entity.HopperBlockEntity mcc$hopper;

    public TileEntityHopper() {
        this.mcc$hopper = null;
    }

    /** View of a vanilla hopper. */
    public TileEntityHopper(net.minecraft.world.level.block.entity.HopperBlockEntity hopper) {
        this.mcc$hopper = hopper;
    }

    // ------------------------------------------------------------------ cooldown

    /** {@code func_145896_c}: the ticks until the hopper moves items again. */
    public void setTransferCooldown(int ticks) {
        if (mcc$hopper != null) VanillaOps.setInt(mcc$hopper, net.minecraft.world.level.block.entity.HopperBlockEntity.class, "cooldownTime", ticks);
        else transferCooldown = ticks;
    }

    /** {@code func_145888_j}. */
    public boolean isOnTransferCooldown() {
        if (mcc$hopper != null) return VanillaOps.getInt(mcc$hopper, net.minecraft.world.level.block.entity.HopperBlockEntity.class, "cooldownTime", 0) > 0;
        return transferCooldown > 0;
    }

    // ------------------------------------------------------------------ inventory

    @Override
    public int getSizeInventory() {
        return mcc$hopper != null ? mcc$hopper.getContainerSize() : field_145900_a.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (mcc$hopper != null) return ItemStack.mcc$wrap(mcc$hopper.getItem(slot));
        return slot >= 0 && slot < field_145900_a.length ? field_145900_a[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (mcc$hopper != null) return ItemStack.mcc$wrap(mcc$hopper.removeItem(slot, amount));
        ItemStack stack = field_145900_a[slot];
        if (stack == null) return null;
        if (stack.mcc$count() <= amount) {
            field_145900_a[slot] = null;
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (stack.mcc$count() == 0) field_145900_a[slot] = null;
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (mcc$hopper != null) return ItemStack.mcc$wrap(mcc$hopper.removeItemNoUpdate(slot));
        ItemStack stack = field_145900_a[slot];
        field_145900_a[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (mcc$hopper != null) {
            mcc$hopper.setItem(slot, ItemStack.mcc$toModern(stack));
            return;
        }
        field_145900_a[slot] = stack;
        if (stack != null && stack.mcc$count() > getInventoryStackLimit()) stack.mcc$setCount(getInventoryStackLimit());
    }

    @Override
    public String getInventoryName() {
        return hasCustomInventoryName() ? field_145902_i : "container.hopper";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return field_145902_i != null && !field_145902_i.isEmpty();
    }

    public void func_145886_a(String name) {
        this.field_145902_i = name;
    }

    @Override
    public int getInventoryStackLimit() {
        return mcc$hopper != null ? mcc$hopper.getMaxStackSize() : 64;
    }

    @Override
    public void markDirty() {
        if (mcc$hopper != null) mcc$hopper.setChanged();
        else super.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (mcc$hopper != null) return mcc$hopper.stillValid(player.mcc$player());
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public double getXPos() {
        return xCoord;
    }

    @Override
    public double getYPos() {
        return yCoord;
    }

    @Override
    public double getZPos() {
        return zCoord;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagList items = tag.getTagList("Items", 10);
        field_145900_a = new ItemStack[getSizeInventory()];
        if (tag.hasKey("CustomName", 8)) field_145902_i = tag.getString("CustomName");
        transferCooldown = tag.getInteger("TransferCooldown");
        for (int i = 0; i < items.tagCount(); ++i) {
            NBTTagCompound item = items.getCompoundTagAt(i);
            int slot = item.getByte("Slot");
            if (slot >= 0 && slot < field_145900_a.length) field_145900_a[slot] = ItemStack.loadItemStackFromNBT(item);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < field_145900_a.length; ++i) {
            if (field_145900_a[i] != null) {
                NBTTagCompound item = new NBTTagCompound();
                item.setByte("Slot", (byte) i);
                field_145900_a[i].writeToNBT(item);
                items.appendTag(item);
            }
        }
        tag.setTag("Items", items);
        tag.setInteger("TransferCooldown", transferCooldown);
        if (hasCustomInventoryName()) tag.setString("CustomName", field_145902_i);
    }

    // ------------------------------------------------------------------ 1.7.10 transfer rules

    /**
     * Puts as much of {@code stack} as fits into the inventory (through the slots open on {@code side} for a sided inventory, -1 for
     * any slot) and returns the rest, null when everything went in.
     */
    public static ItemStack func_145889_a(IInventory inventory, ItemStack stack, int side) {
        if (inventory == null || stack == null) return stack;
        if (inventory instanceof ISidedInventory sided && side > -1) {
            int[] slots = sided.getAccessibleSlotsFromSide(side);
            for (int i = 0; slots != null && i < slots.length && stack != null && stack.mcc$count() > 0; ++i) {
                stack = mcc$insertIntoSlot(inventory, stack, slots[i], side);
            }
        } else {
            int size = inventory.getSizeInventory();
            for (int i = 0; i < size && stack != null && stack.mcc$count() > 0; ++i) stack = mcc$insertIntoSlot(inventory, stack, i, side);
        }
        if (stack != null && stack.mcc$count() == 0) stack = null;
        return stack;
    }

    private static boolean mcc$canInsert(IInventory inventory, ItemStack stack, int slot, int side) {
        if (!inventory.isItemValidForSlot(slot, stack)) return false;
        return !(inventory instanceof ISidedInventory sided) || sided.canInsertItem(slot, stack, side);
    }

    private static boolean mcc$canStack(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage() && a.mcc$count() <= a.getMaxStackSize()
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static ItemStack mcc$insertIntoSlot(IInventory inventory, ItemStack stack, int slot, int side) {
        ItemStack existing = inventory.getStackInSlot(slot);
        if (!mcc$canInsert(inventory, stack, slot, side)) return stack;
        boolean moved = false;
        if (existing == null) {
            int max = Math.min(stack.getMaxStackSize(), inventory.getInventoryStackLimit());
            if (max >= stack.mcc$count()) {
                inventory.setInventorySlotContents(slot, stack);
                stack = null;
            } else {
                inventory.setInventorySlotContents(slot, stack.splitStack(max));
            }
            moved = true;
        } else if (mcc$canStack(existing, stack)) {
            int max = Math.min(stack.getMaxStackSize(), inventory.getInventoryStackLimit());
            if (max > existing.mcc$count()) {
                int n = Math.min(stack.mcc$count(), max - existing.mcc$count());
                stack.mcc$setCount(stack.mcc$count() - n);
                existing.mcc$setCount(existing.mcc$count() + n);
                moved = n > 0;
            }
        }
        if (moved) {
            if (inventory instanceof TileEntityHopper hopper) hopper.setTransferCooldown(8);
            inventory.markDirty();
        }
        return stack;
    }

    /**
     * {@code func_145893_b}: the inventory at a position: a tile entity's (a whole double chest for a chest), else a random entity
     * with an inventory (minecarts) in that block space.
     */
    public static IInventory func_145893_b(World world, double x, double y, double z) {
        if (world == null) return null;
        int bx = legacyconnector.v1_7_10.net.minecraft.util.MathHelper.floor_double(x);
        int by = legacyconnector.v1_7_10.net.minecraft.util.MathHelper.floor_double(y);
        int bz = legacyconnector.v1_7_10.net.minecraft.util.MathHelper.floor_double(z);
        IInventory inventory = null;
        TileEntity te = world.getTileEntity(bx, by, bz);
        if (te instanceof IInventory inv) {
            inventory = inv;
            if (inv instanceof TileEntityChest && world.getBlock(bx, by, bz) instanceof legacyconnector.v1_7_10.net.minecraft.block.BlockChest chest) {
                IInventory whole = chest.getInventory(world, bx, by, bz);
                if (whole != null) inventory = whole;
            }
        }
        if (inventory == null) {
            List<?> entities = world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D));
            List<IInventory> inventories = new java.util.ArrayList<>();
            for (Object o : entities) {
                if (o instanceof IInventory inv && o instanceof legacyconnector.v1_7_10.net.minecraft.entity.Entity e && e.isEntityAlive()) inventories.add(inv);
            }
            if (!inventories.isEmpty()) inventory = inventories.get(world.rand.nextInt(inventories.size()));
        }
        return inventory;
    }

    /** {@code func_145897_a}: an item entity in the block space at the position, or null. */
    public static EntityItem func_145897_a(World world, double x, double y, double z) {
        if (world == null) return null;
        List<?> items = world.getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D));
        for (Object o : items) {
            if (o instanceof EntityItem item && item.isEntityAlive()) return item;
        }
        return null;
    }

    /** {@code func_145898_a}: moves an item entity's stack into the inventory; true when all of it went in (the entity is then removed). */
    public static boolean func_145898_a(IInventory inventory, EntityItem entity) {
        if (entity == null) return false;
        ItemStack stack = entity.getEntityItem();
        if (stack == null) return false;
        ItemStack rest = func_145889_a(inventory, stack.copy(), -1);
        if (rest != null && rest.mcc$count() != 0) {
            entity.setEntityItemStack(rest);
            return false;
        }
        entity.setDead();
        return true;
    }
}
