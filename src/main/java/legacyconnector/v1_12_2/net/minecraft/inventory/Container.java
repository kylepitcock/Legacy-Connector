package legacyconnector.v1_12_2.net.minecraft.inventory;

import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/**
 * 1.12.2 container: the server-side half of a GUI, a list of slots over inventories, the stacks last sent to the listeners (the players
 * looking at it) and the shift-click and merge rules. This is the legacy logic itself; showing it to a player is the menu bridge's job.
 * Clicks ({@code slotClick}) are handled by the modern menu standing for the container.
 */
public abstract class Container {
    public NonNullList<ItemStack> inventoryItemStacks = NonNullList.create();
    public List<Slot> inventorySlots = new java.util.ArrayList<>();
    public int windowId;
    private short transactionID;
    private int dragMode = -1;
    private int dragEvent;
    private final Set<Slot> dragSlots = new java.util.HashSet<>();
    protected List<IContainerListener> listeners = new java.util.ArrayList<>();
    private final Set<EntityPlayer> playerList = new java.util.HashSet<>();

    public Container() {
    }

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

    /** Sends the slots whose stacks changed since the last call to every listener. */
    public void detectAndSendChanges() {
        for (int i = 0; i < inventorySlots.size(); i++) {
            ItemStack now = inventorySlots.get(i).getStack();
            ItemStack before = inventoryItemStacks.get(i);
            if (!ItemStack.areItemStacksEqual(before, now)) {
                boolean clientStackChanged = !ItemStack.areItemStacksEqualUsingNBTShareTag(before, now);
                before = now.isEmpty() ? ItemStack.EMPTY : now.copy();
                inventoryItemStacks.set(i, before);
                if (clientStackChanged) for (IContainerListener l : listeners) l.sendSlotContents(this, i, before);
            }
        }
    }

    public boolean enchantItem(EntityPlayer player, int id) {
        return false;
    }

    public Slot getSlotFromInventory(IInventory inv, int slotIn) {
        for (Slot s : inventorySlots) if (s.isHere(inv, slotIn)) return s;
        return null;
    }

    public Slot getSlot(int slotId) {
        return inventorySlots.get(slotId);
    }

    /** Shift-click: containers override it; the default moves nothing. */
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        return slot != null ? slot.getStack() : ItemStack.EMPTY;
    }

    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return true;
    }

    /** The modern menu returns the carried stack itself when the screen closes. */
    public void onContainerClosed(EntityPlayer player) {
    }

    /** Gives the stacks of a crafting grid back to the player (dropped when the player is gone). */
    protected void clearContainer(EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.world.World world, IInventory inventory) {
        var modern = player == null ? null : player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack s = inventory.removeStackFromSlot(i);
            if (s.isEmpty() || modern == null) continue;
            var stack = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(s);
            if (!modern.isAlive() || modern instanceof net.minecraft.server.level.ServerPlayer sp && sp.hasDisconnected()) modern.drop(stack, false);
            else modern.getInventory().placeItemBackInInventory(stack);
        }
    }

    public void onCraftMatrixChanged(IInventory inventory) {
        detectAndSendChanges();
    }

    public void putStackInSlot(int slotId, ItemStack stack) {
        getSlot(slotId).putStack(stack);
    }

    public void setAll(List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) getSlot(i).putStack(stacks.get(i));
    }

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

    /** Moves as much of the stack as fits into the slots [start, end): first onto matching stacks, then into empty slots. */
    protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = reverseDirection ? endIndex - 1 : startIndex;
        if (stack.isStackable()) {
            while (!stack.isEmpty()) {
                if (reverseDirection ? i < startIndex : i >= endIndex) break;
                Slot slot = inventorySlots.get(i);
                ItemStack itemstack = slot.getStack();
                if (!itemstack.isEmpty() && itemstack.getItem() == stack.getItem() && (!stack.getHasSubtypes() || stack.getMetadata() == itemstack.getMetadata())
                        && ItemStack.areItemStackTagsEqual(stack, itemstack)) {
                    int j = itemstack.getCount() + stack.getCount();
                    int maxSize = Math.min(slot.getSlotStackLimit(), stack.getMaxStackSize());
                    if (j <= maxSize) {
                        stack.setCount(0);
                        itemstack.setCount(j);
                        slot.onSlotChanged();
                        flag = true;
                    } else if (itemstack.getCount() < maxSize) {
                        stack.shrink(maxSize - itemstack.getCount());
                        itemstack.setCount(maxSize);
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
                Slot slot1 = inventorySlots.get(i);
                ItemStack itemstack1 = slot1.getStack();
                if (itemstack1.isEmpty() && slot1.isItemValid(stack)) {
                    if (stack.getCount() > slot1.getSlotStackLimit()) slot1.putStack(stack.splitStack(slot1.getSlotStackLimit()));
                    else slot1.putStack(stack.splitStack(stack.getCount()));
                    slot1.onSlotChanged();
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

    public static int getQuickcraftMask(int buttonIn, int dragModeIn) {
        return buttonIn & 3 | (dragModeIn & 3) << 2;
    }

    public static boolean isValidDragMode(int dragModeIn, EntityPlayer player) {
        if (dragModeIn == 0 || dragModeIn == 1) return true;
        return dragModeIn == 2 && player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild;
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

    public static void computeStackSize(Set<Slot> dragSlotsIn, int dragModeIn, ItemStack stack, int slotStackSize) {
        switch (dragModeIn) {
            case 0 -> stack.setCount((int) Math.floor((float) stack.getCount() / (float) dragSlotsIn.size()));
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

    /** The comparator signal of a tile entity that is an inventory. */
    public static int calcRedstone(legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity te) {
        return te instanceof IInventory inv ? calcRedstoneFromInventory(inv) : 0;
    }

    public static int calcRedstoneFromInventory(IInventory inv) {
        if (inv == null) return 0;
        int i = 0;
        float f = 0.0F;
        for (int j = 0; j < inv.getSizeInventory(); j++) {
            ItemStack itemstack = inv.getStackInSlot(j);
            if (!itemstack.isEmpty()) {
                f += (float) itemstack.getCount() / (float) Math.min(inv.getInventoryStackLimit(), itemstack.getMaxStackSize());
                i++;
            }
        }
        f = f / (float) inv.getSizeInventory();
        return (int) Math.floor(f * 14.0F) + (i > 0 ? 1 : 0);
    }

    /** The next transaction number (1.12's window confirmations; the modern game confirms by state id). */
    public short mcc$nextTransactionID() {
        return ++transactionID;
    }
}
