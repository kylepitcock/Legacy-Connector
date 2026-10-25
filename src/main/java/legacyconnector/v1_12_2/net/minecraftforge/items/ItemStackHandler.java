package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraftforge.common.util.INBTSerializable;

/** A plain list of stacks as an item handler. */
public class ItemStackHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<NBTTagCompound> {
    protected NonNullList<ItemStack> stacks;

    public ItemStackHandler() {
        this(1);
    }

    public ItemStackHandler(int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStackHandler(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public void setSize(int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!isItemValid(slot, stack)) return stack;
        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        int limit = getStackLimit(slot, stack);
        if (!existing.isEmpty()) {
            if (!ItemStack.areItemsEqual(stack, existing) || !ItemStack.areItemStackTagsEqual(stack, existing)) return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0) return stack;
        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                ItemStack put = reachedLimit ? stack.copy() : stack;
                if (reachedLimit) put.setCount(limit);
                stacks.set(slot, put.copy());
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }
        if (!reachedLimit) return ItemStack.EMPTY;
        ItemStack rest = stack.copy();
        rest.setCount(stack.getCount() - limit);
        return rest;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
            }
            return existing;
        }
        if (!simulate) {
            stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
            onContentsChanged(slot);
        }
        return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Slot", i);
                stacks.get(i).writeToNBT(tag);
                items.appendTag(tag);
            }
        }
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Items", items);
        nbt.setInteger("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        setSize(nbt.hasKey("Size", 3) ? nbt.getInteger("Size") : stacks.size());
        NBTTagList items = nbt.getTagList("Items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound tag = items.getCompoundTagAt(i);
            int slot = tag.getInteger("Slot");
            if (slot >= 0 && slot < stacks.size()) stacks.set(slot, new ItemStack(tag));
        }
        onLoad();
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size()) throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
    }

    protected void onLoad() {
    }

    protected void onContentsChanged(int slot) {
    }
}
