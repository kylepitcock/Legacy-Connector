package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryBasic;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Forge's container slot over one slot of an {@link IItemHandler} (the item handler decides what fits and what may be taken). */
public class SlotItemHandler extends Slot {
    private static final IInventory emptyInventory = new InventoryBasic("[Null]", true, 0);
    private final IItemHandler itemHandler;
    private final int index;

    public SlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(emptyInventory, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty() || !itemHandler.isItemValid(index, stack)) return false;
        IItemHandler handler = getItemHandler();
        ItemStack remainder;
        if (handler instanceof IItemHandlerModifiable modifiable) {
            ItemStack current = modifiable.getStackInSlot(index);
            modifiable.setStackInSlot(index, ItemStack.EMPTY);
            remainder = modifiable.insertItem(index, stack, true);
            modifiable.setStackInSlot(index, current);
        } else {
            remainder = handler.insertItem(index, stack, true);
        }
        return remainder.getCount() < stack.getCount();
    }

    @Override
    public ItemStack getStack() {
        return getItemHandler().getStackInSlot(index);
    }

    @Override
    public void putStack(ItemStack stack) {
        if (getItemHandler() instanceof IItemHandlerModifiable modifiable) modifiable.setStackInSlot(index, stack);
        onSlotChanged();
    }

    @Override
    public void onSlotChange(ItemStack p_75220_1_, ItemStack p_75220_2_) {
    }

    @Override
    public int getSlotStackLimit() {
        return itemHandler.getSlotLimit(index);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        ItemStack maxAdd = stack.copy();
        int maxInput = stack.getMaxStackSize();
        maxAdd.setCount(maxInput);
        IItemHandler handler = getItemHandler();
        ItemStack current = handler.getStackInSlot(index);
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(index, ItemStack.EMPTY);
            ItemStack remainder = modifiable.insertItem(index, maxAdd, true);
            modifiable.setStackInSlot(index, current);
            return maxInput - remainder.getCount();
        }
        ItemStack remainder = handler.insertItem(index, maxAdd, true);
        return current.getCount() + maxInput - remainder.getCount();
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        return !getItemHandler().extractItem(index, 1, true).isEmpty();
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        return getItemHandler().extractItem(index, amount, false);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return other instanceof SlotItemHandler s && s.getItemHandler() == itemHandler;
    }
}
