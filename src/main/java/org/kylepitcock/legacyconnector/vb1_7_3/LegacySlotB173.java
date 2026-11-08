package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A modern slot over a b1.7.3 slot of a mod's container (a machine's input, a furnace output, a crafting result): reading and writing go
 * through the legacy slot, so its inventory, {@code isItemValid}, stack limit and {@code onPickupFromSlot} (crafting uses up the grid)
 * apply. Items b1.7.3 did not have cannot be put in.
 */
public final class LegacySlotB173 extends net.minecraft.world.inventory.Slot {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");

    private final Slot legacy;
    private final SyncedStackB173 sync = new SyncedStackB173();
    private boolean logged;

    public LegacySlotB173(Slot legacy) {
        super(LegacyInventoryB173.of(legacy.inventory), legacy.index, legacy.x, legacy.y);
        this.legacy = legacy;
    }

    public Slot legacy() {
        return legacy;
    }

    private ItemStack legacyStack() {
        try {
            return legacy.getStack();
        } catch (RuntimeException | LinkageError e) {
            fail("getStack", e);
            return null;
        }
    }

    private void fail(String what, Throwable e) {
        if (logged) return;
        logged = true;
        LOG.warn("b1.7.3 slot {} ({}) failed in {}: {}", legacy.id, legacy.getClass().getName(), what, e.toString());
    }

    private static void use(Player player) {
        if (player != null) Minecraft.mcc$use(World.mcc$wrap(player.level()));
    }

    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        return sync.get(legacyStack());
    }

    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }

    @Override
    public void set(net.minecraft.world.item.ItemStack stack) {
        sync.invalidate();
        try {
            legacy.setStack(stack == null || stack.isEmpty() ? null : StacksB173.toLegacy(stack));
        } catch (RuntimeException | LinkageError e) {
            fail("putStack", e);
        }
    }

    @Override
    public void setByPlayer(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemStack previous) {
        set(stack);
    }

    @Override
    public net.minecraft.world.item.ItemStack remove(int amount) {
        sync.invalidate();
        try {
            return StacksB173.toModern(legacy.takeStack(amount));
        } catch (RuntimeException | LinkageError e) {
            fail("decrStackSize", e);
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    @Override
    public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
        ItemStack l = StacksB173.toLegacy(stack);
        if (l == null) return false;
        try {
            return legacy.canInsert(l);
        } catch (RuntimeException | LinkageError e) {
            fail("isItemValid", e);
            return false;
        }
    }

    @Override
    public int getMaxStackSize() {
        try {
            return legacy.getMaxItemCount();
        } catch (RuntimeException | LinkageError e) {
            return 64;
        }
    }

    @Override
    public void onTake(Player player, net.minecraft.world.item.ItemStack stack) {
        use(player);
        try {
            legacy.onTakeItem(StacksB173.toLegacy(stack));
        } catch (RuntimeException | LinkageError e) {
            fail("onPickupFromSlot", e);
        }
        sync.invalidate();
    }

    /** Writes back a stack the modern code changed in place, else tells the legacy slot it changed. */
    @Override
    public void setChanged() {
        if (!flush()) {
            try {
                legacy.markDirty();
            } catch (RuntimeException | LinkageError e) {
                fail("onSlotChanged", e);
            }
        }
    }

    /** Writes a stack changed in place back into the legacy inventory; false if there was none. */
    boolean flush() {
        ItemStack now = legacyStack();
        if (!sync.changedInPlace(now)) return false;
        net.minecraft.world.item.ItemStack changed = sync.modern();
        sync.invalidate();
        try {
            legacy.setStack(changed.isEmpty() ? null : StacksB173.toLegacy(changed));
        } catch (RuntimeException | LinkageError e) {
            fail("putStack", e);
        }
        return true;
    }

    @Override
    public boolean isSameInventory(net.minecraft.world.inventory.Slot other) {
        return other instanceof LegacySlotB173 o && o.legacy.inventory == legacy.inventory;
    }
}
