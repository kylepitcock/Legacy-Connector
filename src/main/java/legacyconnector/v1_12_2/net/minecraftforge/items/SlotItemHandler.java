package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryBasic;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Forge: a container slot over one slot of an {@link IItemHandler} (validity and limits come from simulated inserts). */
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
        if (stack.isEmpty()) return false;
        IItemHandler handler = getItemHandler();
        ItemStack remainder;
        if (handler instanceof IItemHandlerModifiable handlerModifiable) {
            ItemStack currentStack = handlerModifiable.getStackInSlot(index);
            handlerModifiable.setStackInSlot(index, ItemStack.EMPTY);
            remainder = handlerModifiable.insertItem(index, stack, true);
            handlerModifiable.setStackInSlot(index, currentStack);
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
        ((IItemHandlerModifiable) getItemHandler()).setStackInSlot(index, stack);
        onSlotChanged();
    }

    @Override
    public void onSlotChange(ItemStack moved, ItemStack original) {
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
        ItemStack currentStack = handler.getStackInSlot(index);
        if (handler instanceof IItemHandlerModifiable handlerModifiable) {
            handlerModifiable.setStackInSlot(index, ItemStack.EMPTY);
            ItemStack remainder = handlerModifiable.insertItem(index, maxAdd, true);
            handlerModifiable.setStackInSlot(index, currentStack);
            return maxInput - remainder.getCount();
        }
        ItemStack remainder = handler.insertItem(index, maxAdd, true);
        int current = currentStack.getCount();
        int added = maxInput - remainder.getCount();
        return current + added;
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
