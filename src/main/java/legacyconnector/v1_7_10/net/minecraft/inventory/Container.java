package legacyconnector.v1_7_10.net.minecraft.inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

/**
 * 1.7.10 container. Hosted by a modern {@code LegacyMenu}, which performs clicking and syncing with modern code
 * and calls back here for {@link #transferStackInSlot}, {@link #canInteractWith}, {@link #onContainerClosed} and
 * {@link #detectAndSendChanges} (progress bars). {@link #mergeItemStack} is the verbatim 1.7.10 algorithm.
 */
public abstract class Container {
    public List inventoryItemStacks = new ArrayList();
    public List inventorySlots = new ArrayList();
    public int windowId;
    protected List crafters = new ArrayList();
    private final Set<EntityPlayer> playerList = new HashSet<>();
    private short transactionID;

    protected Slot addSlotToContainer(Slot slot) {
        slot.slotNumber = inventorySlots.size();
        inventorySlots.add(slot);
        inventoryItemStacks.add(null);
        return slot;
    }

    public void addCraftingToCrafters(ICrafting crafter) {
        if (crafters.contains(crafter)) throw new IllegalArgumentException("Listener already listening");
        crafters.add(crafter);
        crafter.sendContainerAndContentsToPlayer(this, getInventory());
        detectAndSendChanges();
    }

    public void removeCraftingFromCrafters(ICrafting crafter) {
        crafters.remove(crafter);
    }

    public List getInventory() {
        List<ItemStack> out = new ArrayList<>();
        for (Object o : inventorySlots) out.add(((Slot) o).getStack());
        return out;
    }

    /** Tracks slot changes; actual slot syncing is done by the modern menu. Mods override to send progress bars. */
    public void detectAndSendChanges() {
        for (int i = 0; i < inventorySlots.size(); ++i) {
            ItemStack current = ((Slot) inventorySlots.get(i)).getStack();
            ItemStack previous = (ItemStack) inventoryItemStacks.get(i);
            if (!ItemStack.areItemStacksEqual(previous, current)) {
                previous = current == null ? null : current.copy();
                inventoryItemStacks.set(i, previous);
                for (Object c : crafters) ((ICrafting) c).sendSlotContents(this, i, previous);
            }
        }
    }

    public boolean enchantItem(EntityPlayer player, int id) {
        return false;
    }

    public Slot getSlotFromInventory(IInventory inv, int index) {
        for (Object o : inventorySlots) {
            Slot s = (Slot) o;
            if (s.isSlotInInventory(inv, index)) return s;
        }
        return null;
    }

    public Slot getSlot(int index) {
        return (Slot) inventorySlots.get(index);
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        return slot != null ? slot.getStack() : null;
    }

    public void onContainerClosed(EntityPlayer player) {
        InventoryPlayer inv = player.inventory;
        if (inv.getItemStack() != null) {
            player.dropPlayerItemWithRandomChoice(inv.getItemStack(), false);
            inv.setItemStack(null);
        }
    }

    public void onCraftMatrixChanged(IInventory inv) {
        detectAndSendChanges();
    }

    public void putStackInSlot(int slot, ItemStack stack) {
        getSlot(slot).putStack(stack);
    }

    public void putStacksInSlots(ItemStack[] stacks) {
        for (int i = 0; i < stacks.length; ++i) getSlot(i).putStack(stacks[i]);
    }

    /** Client side: receives values sent through {@link ICrafting#sendProgressBarUpdate}. */
    public void updateProgressBar(int id, int value) {
    }

    public short getNextTransactionID(InventoryPlayer inv) {
        return ++transactionID;
    }

    public boolean isPlayerNotUsingContainer(EntityPlayer player) {
        return !playerList.contains(player);
    }

    public void setPlayerIsPresent(EntityPlayer player, boolean present) {
        if (present) playerList.remove(player);
        else playerList.add(player);
    }

    public abstract boolean canInteractWith(EntityPlayer player);

    /** Verbatim 1.7.10 shift-click merge: first into matching stacks, then into empty slots. */
    protected boolean mergeItemStack(ItemStack stack, int start, int end, boolean reverse) {
        boolean flag = false;
        int k = reverse ? end - 1 : start;
        Slot slot;
        ItemStack target;
        if (stack.isStackable()) {
            while (stack.mcc$count() > 0 && (!reverse && k < end || reverse && k >= start)) {
                slot = (Slot) inventorySlots.get(k);
                target = slot.getStack();
                if (target != null && target.getItem() == stack.getItem() && (!stack.getHasSubtypes() || stack.getItemDamage() == target.getItemDamage())
                        && ItemStack.areItemStackTagsEqual(stack, target)) {
                    int l = target.mcc$count() + stack.mcc$count();
                    if (l <= stack.getMaxStackSize()) {
                        stack.mcc$setCount(0);
                        target.mcc$setCount(l);
                        slot.onSlotChanged();
                        flag = true;
                    } else if (target.mcc$count() < stack.getMaxStackSize()) {
                        stack.mcc$setCount(stack.mcc$count() - (stack.getMaxStackSize() - target.mcc$count()));
                        target.mcc$setCount(stack.getMaxStackSize());
                        slot.onSlotChanged();
                        flag = true;
                    }
                }
                k += reverse ? -1 : 1;
            }
        }
        if (stack.mcc$count() > 0) {
            k = reverse ? end - 1 : start;
            while (!reverse && k < end || reverse && k >= start) {
                slot = (Slot) inventorySlots.get(k);
                target = slot.getStack();
                if (target == null && slot.isItemValid(stack)) {
                    slot.putStack(stack.copy());
                    slot.onSlotChanged();
                    stack.mcc$setCount(0);
                    flag = true;
                    break;
                }
                k += reverse ? -1 : 1;
            }
        }
        return flag;
    }

    public static boolean canAddItemToSlot(Slot slot, ItemStack stack, boolean ignoreSize) {
        boolean flag = slot == null || !slot.getHasStack();
        if (slot != null && slot.getHasStack() && stack != null && stack.isItemEqual(slot.getStack()) && ItemStack.areItemStackTagsEqual(slot.getStack(), stack)) {
            int i = ignoreSize ? 0 : stack.mcc$count();
            flag |= slot.getStack().mcc$count() + i <= stack.getMaxStackSize();
        }
        return flag;
    }

    public boolean canDragIntoSlot(Slot slot) {
        return true;
    }

    public boolean func_94530_a(ItemStack stack, Slot slot) {
        return true;
    }

    public static int calcRedstoneFromInventory(IInventory inv) {
        if (inv == null) return 0;
        int filled = 0;
        float fraction = 0.0F;
        for (int j = 0; j < inv.getSizeInventory(); ++j) {
            ItemStack s = inv.getStackInSlot(j);
            if (s != null) {
                fraction += (float) s.mcc$count() / (float) Math.min(inv.getInventoryStackLimit(), s.getMaxStackSize());
                ++filled;
            }
        }
        fraction /= inv.getSizeInventory();
        return MathHelper.floor_float(fraction * 14.0F) + (filled > 0 ? 1 : 0);
    }

    /** The modern menu hosting this container (set by the bridge). */
    public Object mcc$menu;
}
