package legacyconnector.v1_12_2.net.minecraft.inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 container. Hosted by a modern menu ({@code LegacyMenu1122}) that performs clicking, dragging and slot syncing with modern code
 * and calls back here for {@link #transferStackInSlot}, {@link #canInteractWith}, {@link #onContainerClosed}, {@link #enchantItem} and
 * {@link #detectAndSendChanges} (progress bars through the listeners). {@link #mergeItemStack} is the verbatim 1.12.2 algorithm.
 */
public abstract class Container {
    public NonNullList<ItemStack> inventoryItemStacks = NonNullList.create();
    public List<Slot> inventorySlots = new ArrayList<>();
    public int windowId;
    private short transactionID;
    protected int dragMode = -1;
    protected int dragEvent;
    protected final Set<Slot> dragSlots = new HashSet<>();
    protected List<IContainerListener> listeners = new ArrayList<>();
    private final Set<EntityPlayer> playerList = new HashSet<>();
    /** The modern menu hosting this container (set by the bridge). */
    public Object mcc$menu;

    protected Slot addSlotToContainer(Slot slot) {
        slot.slotNumber = inventorySlots.size();
        inventorySlots.add(slot);
        inventoryItemStacks.add(ItemStack.EMPTY);
        return slot;
    }

    public void addListener(IContainerListener listener) {
        if (listeners.contains(listener)) throw new IllegalArgumentException("Listener already listening");
        listeners.add(listener);
        listener.sendAllContents(this, getInventory());
        detectAndSendChanges();
    }

    public void removeListener(IContainerListener listener) {
        listeners.remove(listener);
    }

    public NonNullList<ItemStack> getInventory() {
        NonNullList<ItemStack> out = NonNullList.create();
        for (Slot s : inventorySlots) out.add(s.getStack());
        return out;
    }

    /**
     * Remembers slot contents and tells listeners about changed slots. The player's own slot syncing is done by the modern menu, so the
     * player listener is not sent slots twice; mods override this to send progress bars through {@code listeners}.
     */
    public void detectAndSendChanges() {
        for (int i = 0; i < inventorySlots.size(); ++i) {
            ItemStack current = inventorySlots.get(i).getStack();
            ItemStack previous = inventoryItemStacks.get(i);
            if (!ItemStack.areItemStacksEqual(previous, current)) {
                previous = current.isEmpty() ? ItemStack.EMPTY : current.copy();
                inventoryItemStacks.set(i, previous);
                for (IContainerListener l : listeners) if (!(l instanceof EntityPlayerMP)) l.sendSlotContents(this, i, previous);
            }
        }
    }

    public boolean enchantItem(EntityPlayer player, int id) {
        return false;
    }

    public Slot getSlotFromInventory(IInventory inv, int index) {
        for (Slot s : inventorySlots) if (s.isHere(inv, index)) return s;
        return null;
    }

    public Slot getSlot(int index) {
        return inventorySlots.get(index);
    }

    /** Shift-click. 1.12.2 returned the slot's stack, which loops forever in the click code; nothing moves unless a mod overrides it. */
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * A click in the container. The behaviour is the modern menu's; containers that override this (ghost slots, filters) are called by
     * the hosting menu for every click and reach it through {@code super.slotClick}.
     */
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        ItemStack before = slotId >= 0 && slotId < inventorySlots.size() ? inventorySlots.get(slotId).getStack().copy() : ItemStack.EMPTY;
        if (mcc$menu instanceof org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122 menu) menu.mcc$modernClick(slotId, dragType, clickType);
        return before;
    }

    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return true;
    }

    /** The carried stack is dropped by the hosting modern menu when it closes. */
    public void onContainerClosed(EntityPlayer player) {
    }

    /** Gives an inventory's contents back to the player (or drops them when the player is gone), as crafting tables do on closing. */
    protected void clearContainer(EntityPlayer player, World world, IInventory inventory) {
        var modern = player == null ? null : player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack legacy = inventory.removeStackFromSlot(i);
            if (legacy == null || legacy.isEmpty() || modern == null) continue;
            var stack = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(legacy);
            boolean gone = !modern.isAlive() || modern instanceof net.minecraft.server.level.ServerPlayer sp && sp.hasDisconnected();
            if (gone) modern.drop(stack, false);
            else modern.getInventory().placeItemBackInInventory(stack);
        }
    }

    public void onCraftMatrixChanged(IInventory inv) {
        detectAndSendChanges();
    }

    public void putStackInSlot(int slot, ItemStack stack) {
        getSlot(slot).putStack(stack);
    }

    public void setAll(List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size() && i < inventorySlots.size(); ++i) getSlot(i).putStack(stacks.get(i));
    }

    /** Client side: receives the values sent through {@link IContainerListener#sendWindowProperty}. */
    public void updateProgressBar(int id, int data) {
    }

    public boolean getCanCraft(EntityPlayer player) {
        return !playerList.contains(player);
    }

    public void setCanCraft(EntityPlayer player, boolean canCraft) {
        if (canCraft) playerList.remove(player);
        else playerList.add(player);
    }

    public abstract boolean canInteractWith(EntityPlayer player);

    /** Verbatim 1.12.2 shift-click merge: first into matching stacks, then into the first empty valid slot. */
    protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = reverseDirection ? endIndex - 1 : startIndex;
        if (stack.isStackable()) {
            while (!stack.isEmpty()) {
                if (reverseDirection ? i < startIndex : i >= endIndex) break;
                Slot slot = inventorySlots.get(i);
                ItemStack target = slot.getStack();
                if (!target.isEmpty() && target.getItem() == stack.getItem() && (!stack.getHasSubtypes() || stack.getMetadata() == target.getMetadata())
                        && ItemStack.areItemStackTagsEqual(stack, target)) {
                    int j = target.getCount() + stack.getCount();
                    int maxSize = Math.min(slot.getSlotStackLimit(), stack.getMaxStackSize());
                    if (j <= maxSize) {
                        stack.setCount(0);
                        target.setCount(j);
                        slot.onSlotChanged();
                        flag = true;
                    } else if (target.getCount() < maxSize) {
                        stack.shrink(maxSize - target.getCount());
                        target.setCount(maxSize);
                        slot.onSlotChanged();
                        flag = true;
                    }
                }
                i += reverseDirection ? -1 : 1;
            }
        }
        if (!stack.isEmpty()) {
            i = reverseDirection ? endIndex - 1 : startIndex;
            while (true) {
                if (reverseDirection ? i < startIndex : i >= endIndex) break;
                Slot slot = inventorySlots.get(i);
                ItemStack target = slot.getStack();
                if (target.isEmpty() && slot.isItemValid(stack)) {
                    slot.putStack(stack.splitStack(Math.min(stack.getCount(), slot.getSlotStackLimit())));
                    slot.onSlotChanged();
                    flag = true;
                    break;
                }
                i += reverseDirection ? -1 : 1;
            }
        }
        return flag;
    }

    public static int extractDragMode(int eventButton) {
        return eventButton >> 2 & 3;
    }

    public static int getDragEvent(int clickedButton) {
        return clickedButton & 3;
    }

    public static int getQuickcraftMask(int event, int mode) {
        return event & 3 | (mode & 3) << 2;
    }

    public static boolean isValidDragMode(int dragMode, EntityPlayer player) {
        if (dragMode == 0 || dragMode == 1) return true;
        return dragMode == 2 && player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild;
    }

    protected void resetDrag() {
        dragEvent = 0;
        dragSlots.clear();
    }

    public static boolean canAddItemToSlot(Slot slot, ItemStack stack, boolean stackSizeMatters) {
        boolean flag = slot == null || !slot.getHasStack();
        if (!flag && stack.isItemEqual(slot.getStack()) && ItemStack.areItemStackTagsEqual(slot.getStack(), stack)) {
            return slot.getStack().getCount() + (stackSizeMatters ? 0 : stack.getCount()) <= stack.getMaxStackSize();
        }
        return flag;
    }

    public static void computeStackSize(Set<Slot> dragSlots, int dragMode, ItemStack stack, int slotStackSize) {
        switch (dragMode) {
            case 0 -> stack.setCount((int) Math.floor((float) stack.getCount() / (float) dragSlots.size()));
            case 1 -> stack.setCount(1);
            case 2 -> stack.setCount(stack.getItem().getItemStackLimit(stack));
            default -> {
            }
        }
        stack.grow(slotStackSize);
    }

    public boolean canDragIntoSlot(Slot slot) {
        return true;
    }

    public static int calcRedstone(legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity te) {
        return te instanceof IInventory inv ? calcRedstoneFromInventory(inv) : 0;
    }

    public static int calcRedstoneFromInventory(IInventory inv) {
        if (inv == null) return 0;
        int filled = 0;
        float fraction = 0.0F;
        for (int j = 0; j < inv.getSizeInventory(); ++j) {
            ItemStack s = inv.getStackInSlot(j);
            if (s != null && !s.isEmpty()) {
                fraction += (float) s.getCount() / (float) Math.min(inv.getInventoryStackLimit(), s.getMaxStackSize());
                ++filled;
            }
        }
        fraction /= (float) inv.getSizeInventory();
        return (int) Math.floor(fraction * 14.0F) + (filled > 0 ? 1 : 0);
    }

    /** The next transaction number (1.12's window confirmations; the modern game confirms by state id). */
    public short mcc$nextTransactionID() {
        return ++transactionID;
    }
}
