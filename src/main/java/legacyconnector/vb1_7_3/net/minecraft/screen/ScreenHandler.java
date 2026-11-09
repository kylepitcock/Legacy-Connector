package legacyconnector.vb1_7_3.net.minecraft.screen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;

/**
 * Beta 1.7.3 {@code Container}: its slots, the stacks last reported to listeners and the listeners themselves. On the server it is wrapped
 * by a modern menu ({@code LegacyB173Menu}), whose clicks reach the slots; progress values a mod reports to listeners
 * ({@code updateCraftingInventoryInfo}) become synced menu data and come back as {@link #setProperty} on the client.
 */
public abstract class ScreenHandler {
    public List<ItemStack> trackedStacks = new ArrayList<>();
    public List<Slot> slots = new ArrayList<>();
    public int syncId;
    private short revision;
    protected List<ScreenHandlerListener> listeners = new ArrayList<>();
    private Set<PlayerEntity> players = new HashSet<>();

    public ScreenHandler() {}

    protected void addSlot(Slot slot) {
        slot.id = slots.size();
        slots.add(slot);
        trackedStacks.add(null);
    }

    /** MCP {@code updateCraftingResults}: reports changed slots to the listeners (mods add their progress values). */
    public void sendContentUpdates() {
        for (int i = 0; i < slots.size(); i++) {
            ItemStack now = slots.get(i).getStack();
            ItemStack before = trackedStacks.get(i);
            if (!ItemStack.areEqual(before, now)) {
                before = now == null ? null : now.copy();
                trackedStacks.set(i, before);
                for (ScreenHandlerListener l : listeners) l.onSlotUpdate(this, i, before);
            }
        }
    }

    public Slot getSlot(int index) {
        return slots.get(index);
    }

    /** MCP {@code transferStackInSlot} (shift click); the original's default only answered the slot's stack. */
    public ItemStack quickMove(int index) {
        Slot slot = index >= 0 && index < slots.size() ? slots.get(index) : null;
        return slot != null ? slot.getStack() : null;
    }

    /** Original virtual click entrypoint. Native-backed views reuse the shared slot transaction engine. */
    public ItemStack onSlotClick(int slot, int button, boolean quick, PlayerEntity player) {
        return org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Menu.transactionClick(this, player, slot, button, quick);
    }

    public void onClosed(PlayerEntity player) {}

    /** MCP {@code onCraftMatrixChanged}. */
    public void onSlotUpdate(Inventory inventory) {
        sendContentUpdates();
    }

    public void setStackInSlot(int index, ItemStack stack) {
        getSlot(index).setStack(stack);
    }

    public void updateSlotStacks(ItemStack[] stacks) {
        for (int i = 0; i < stacks.length; i++) getSlot(i).setStack(stacks[i]);
    }

    /** MCP {@code updateProgressBar}: a progress value the server sent. */
    public void setProperty(int id, int value) {}

    public short nextRevision(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory inventory) {
        return ++revision;
    }

    public void onAcknowledgementAccepted(short revision) {}

    public void onAcknowledgementDenied(short revision) {}

    public abstract boolean canUse(PlayerEntity player);

    /** MCP {@code mergeItemStack}: fills the slots start..end (backwards if {@code reverse}), first the matching stacks, then empty slots. */
    protected void insertItem(ItemStack stack, int start, int end, boolean reverse) {
        int k = reverse ? end - 1 : start;
        if (stack.isStackable()) {
            while (stack.count > 0 && (!reverse && k < end || reverse && k >= start)) {
                Slot slot = slots.get(k);
                ItemStack s = slot.getStack();
                if (s != null && s.itemId == stack.itemId && (!stack.hasSubtypes() || stack.getDamage() == s.getDamage())
                        && org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.sameComponents(s, stack)) {
                    int total = s.count + stack.count;
                    if (total <= stack.getMaxCount()) {
                        stack.count = 0;
                        s.count = total;
                        slot.markDirty();
                    } else if (s.count < stack.getMaxCount()) {
                        stack.count -= stack.getMaxCount() - s.count;
                        s.count = stack.getMaxCount();
                        slot.markDirty();
                    }
                }
                k += reverse ? -1 : 1;
            }
        }
        if (stack.count > 0) {
            k = reverse ? end - 1 : start;
            while (!reverse && k < end || reverse && k >= start) {
                Slot slot = slots.get(k);
                if (slot.getStack() == null) {
                    slot.setStack(stack.copy());
                    slot.markDirty();
                    stack.count = 0;
                    break;
                }
                k += reverse ? -1 : 1;
            }
        }
    }

    public void addListener(ScreenHandlerListener listener) {
        if (listeners.contains(listener)) throw new IllegalArgumentException("Listener already listening");
        listeners.add(listener);
        listener.onContentsUpdate(this, getStacks());
        sendContentUpdates();
    }

    public void mcc$removeListener(ScreenHandlerListener listener) {
        listeners.remove(listener);
    }

    public List<ItemStack> getStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Slot s : slots) out.add(s.getStack());
        return out;
    }

    public Slot getSlot(Inventory inventory, int index) {
        for (Slot s : slots) if (s.equals(inventory, index)) return s;
        return null;
    }

    public boolean canOpen(PlayerEntity player) {
        return !players.contains(player);
    }

    public void updatePlayerList(PlayerEntity player, boolean add) {
        if (add) players.remove(player);
        else players.add(player);
    }
}
