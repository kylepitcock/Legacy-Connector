package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A minecart with an inventory (chest and hopper carts, mods' storage carts). A mod's cart keeps its 36 slots here and spills
 * them when it dies; a view of a vanilla chest or hopper cart reads and writes the modern cart's own inventory.
 */
public abstract class EntityMinecartContainer extends EntityMinecart implements IInventory {
    private ItemStack[] minecartContainerItems = new ItemStack[36];
    private boolean dropContentsWhenDead = true;

    public EntityMinecartContainer(World world) {
        super(world);
    }

    public EntityMinecartContainer(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    /** View of a modern vanilla chest or hopper cart. */
    protected EntityMinecartContainer(net.minecraft.world.entity.vehicle.AbstractMinecartContainer modern) {
        super(modern);
    }

    private net.minecraft.world.Container mcc$container() {
        return mcc$entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecartContainer c ? c : null;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        var container = mcc$container();
        if (container != null) return slot >= 0 && slot < container.getContainerSize() ? ItemStack.mcc$wrap(container.getItem(slot)) : null;
        return slot >= 0 && slot < minecartContainerItems.length ? minecartContainerItems[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        var container = mcc$container();
        if (container != null) {
            var taken = container.removeItem(slot, amount);
            return taken.isEmpty() ? null : ItemStack.mcc$wrap(taken);
        }
        ItemStack stack = minecartContainerItems[slot];
        if (stack == null) return null;
        if (stack.mcc$count() <= amount) {
            minecartContainerItems[slot] = null;
            return stack;
        }
        ItemStack out = stack.splitStack(amount);
        if (stack.mcc$count() == 0) minecartContainerItems[slot] = null;
        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        var container = mcc$container();
        if (container != null) {
            var taken = container.removeItemNoUpdate(slot);
            return taken.isEmpty() ? null : ItemStack.mcc$wrap(taken);
        }
        ItemStack stack = minecartContainerItems[slot];
        minecartContainerItems[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        var container = mcc$container();
        if (container != null) {
            container.setItem(slot, ItemStack.mcc$toModern(stack));
            return;
        }
        minecartContainerItems[slot] = stack;
        if (stack != null && stack.mcc$count() > getInventoryStackLimit()) stack.mcc$setCount(getInventoryStackLimit());
    }

    @Override
    public void markDirty() {
        var container = mcc$container();
        if (container != null) container.setChanged();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return isEntityAlive() && player.getDistanceSqToEntity(this) <= 64.0D;
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
    public String getInventoryName() {
        return hasCustomInventoryName() ? func_95999_t() : "container.minecart";
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    /** A cart leaving for another dimension is copied there, so this copy must not spill its items. */
    @Override
    public void travelToDimension(int dimension) {
        dropContentsWhenDead = false;
        super.travelToDimension(dimension);
    }

    /** A dying mod cart spills its contents in small random stacks (a vanilla cart drops its own). */
    @Override
    public void setDead() {
        if (dropContentsWhenDead && mcc$container() == null && !mcc$world().isRemote) {
            for (int i = 0; i < getSizeInventory(); ++i) {
                ItemStack stack = getStackInSlot(i);
                if (stack == null) continue;
                float dx = rand.nextFloat() * 0.8F + 0.1F;
                float dy = rand.nextFloat() * 0.8F + 0.1F;
                float dz = rand.nextFloat() * 0.8F + 0.1F;
                while (stack.mcc$count() > 0) {
                    int n = Math.min(rand.nextInt(21) + 10, stack.mcc$count());
                    ItemStack part = stack.splitStack(n);
                    EntityItem item = new EntityItem(mcc$world(), mcc$x() + dx, mcc$y() + dy, mcc$z() + dz, part);
                    item.setVelocity(rand.nextGaussian() * 0.05F, rand.nextGaussian() * 0.05F + 0.2F, rand.nextGaussian() * 0.05F);
                    mcc$world().spawnEntityInWorld(item);
                }
                minecartContainerItems[i] = null;
            }
        }
        super.setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < minecartContainerItems.length; ++i) {
            if (minecartContainerItems[i] == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte("Slot", (byte) i);
            minecartContainerItems[i].writeToNBT(entry);
            items.appendTag(entry);
        }
        tag.setTag("Items", items);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        NBTTagList items = tag.getTagList("Items", 10);
        minecartContainerItems = new ItemStack[Math.max(getSizeInventory(), 0)];
        for (int i = 0; i < items.tagCount(); ++i) {
            NBTTagCompound entry = items.getCompoundTagAt(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < minecartContainerItems.length) minecartContainerItems[slot] = ItemStack.loadItemStackFromNBT(entry);
        }
    }

    /** Opens the cart's inventory (unless a MinecartInteractEvent handler took the click). */
    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartInteractEvent(this, player))) {
            return true;
        }
        if (!mcc$world().isRemote) player.displayGUIChest(this);
        return true;
    }

    /** Fuller carts roll further: drag eases off with the comparator signal of the contents. */
    @Override
    protected void applyDrag() {
        int empty = 15 - legacyconnector.v1_7_10.net.minecraft.inventory.Container.calcRedstoneFromInventory(this);
        float drag = 0.98F + empty * 0.001F;
        mcc$set$motionX(this, mcc$get$motionX(this) * drag);
        mcc$set$motionY(this, 0.0D);
        mcc$set$motionZ(this, mcc$get$motionZ(this) * drag);
    }
}
