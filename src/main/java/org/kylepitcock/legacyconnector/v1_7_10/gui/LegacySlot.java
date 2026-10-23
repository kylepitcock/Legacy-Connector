package org.kylepitcock.legacyconnector.v1_7_10.gui;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/** Modern slot delegating every decision to a legacy slot (custom validation, stack limits, pickup hooks). */
public final class LegacySlot extends Slot {
    private final legacyconnector.v1_7_10.net.minecraft.inventory.Slot legacy;

    public LegacySlot(legacyconnector.v1_7_10.net.minecraft.inventory.Slot legacy) {
        super(LegacyInventoryContainer.of(legacy.inventory), legacy.getSlotIndex(), legacy.xDisplayPosition, legacy.yDisplayPosition);
        this.legacy = legacy;
    }

    public legacyconnector.v1_7_10.net.minecraft.inventory.Slot legacy() {
        return legacy;
    }

    private static EntityPlayer player(Player p) {
        return (EntityPlayer) EntityBridge.wrap(p);
    }

    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        try {
            return ItemStack.mcc$toModern(legacy.getStack());
        } catch (RuntimeException | LinkageError e) {
            // a slot whose inventory is smaller than the container assumed (client/server mismatch) shows as empty
            logOnce(e);
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    private boolean logged;

    private void logOnce(Throwable e) {
        if (!logged) {
            logged = true;
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Gui").warn("Legacy slot {} of {} cannot be read: {}", legacy.slotNumber, legacy.getClass().getName(), e.toString());
        }
    }

    @Override
    public void set(net.minecraft.world.item.ItemStack stack) {
        legacy.putStack(ItemStack.mcc$wrap(stack));
    }

    @Override
    public void setByPlayer(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemStack previous) {
        set(stack);
    }

    @Override
    public net.minecraft.world.item.ItemStack remove(int amount) {
        return ItemStack.mcc$toModern(legacy.decrStackSize(amount));
    }

    @Override
    public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
        return legacy.isItemValid(ItemStack.mcc$wrap(stack));
    }

    @Override
    public int getMaxStackSize() {
        return legacy.getSlotStackLimit();
    }

    @Override
    public boolean mayPickup(Player player) {
        return legacy.canTakeStack(player(player));
    }

    @Override
    public void onTake(Player player, net.minecraft.world.item.ItemStack stack) {
        legacy.onPickupFromSlot(player(player), ItemStack.mcc$wrap(stack));
    }

    @Override
    public void setChanged() {
        legacy.onSlotChanged();
    }

    @Override
    public boolean isActive() {
        return legacy.canBeHovered();
    }
}
