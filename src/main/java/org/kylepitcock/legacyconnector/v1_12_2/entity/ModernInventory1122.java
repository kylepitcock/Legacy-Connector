package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyInventory1122;

/**
 * 1.12.2 {@link IInventory} view of a modern {@link net.minecraft.world.Container} (a vanilla chest, the ender chest, the slots of a vanilla
 * menu). Stacks come back as live views, so a mod that shrinks a stack it got from here changes the real one.
 */
public class ModernInventory1122 implements IInventory {
    private static final Map<net.minecraft.world.Container, ModernInventory1122> VIEWS = new WeakHashMap<>();
    protected final net.minecraft.world.Container modern;
    private final String name;

    protected ModernInventory1122(net.minecraft.world.Container modern, String name) {
        this.modern = modern;
        this.name = name;
    }

    /** The legacy view of a modern container (the legacy inventory itself when it is a legacy inventory's modern view). */
    public static synchronized IInventory of(net.minecraft.world.Container modern) {
        if (modern == null) return null;
        if (modern instanceof LegacyInventory1122 l && l.legacy() != null) return l.legacy();
        return VIEWS.computeIfAbsent(modern, m -> new ModernInventory1122(m, defaultName(m)));
    }

    /** The modern container behind a legacy inventory (its modern view when it is a mod's own inventory). */
    public static net.minecraft.world.Container modern(IInventory inv) {
        if (inv == null) return null;
        if (inv instanceof ModernInventory1122 m) return m.modern;
        return LegacyInventory1122.of(inv);
    }

    private static String defaultName(net.minecraft.world.Container modern) {
        if (modern instanceof net.minecraft.world.Nameable n) return n.getName().getString();
        if (modern instanceof net.minecraft.world.inventory.PlayerEnderChestContainer) return "container.enderchest";
        return "container.inventory";
    }

    public net.minecraft.world.Container mcc$modern() {
        return modern;
    }

    @Override
    public int getSizeInventory() {
        return modern.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return modern.isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= modern.getContainerSize()) return ItemStack.EMPTY;
        return Stacks1122.view(modern.getItem(index));
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return Stacks1122.fromModern(modern.removeItem(index, count));
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return Stacks1122.fromModern(modern.removeItemNoUpdate(index));
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        modern.setItem(index, Stacks1122.toModernLive(stack));
    }

    @Override
    public int getInventoryStackLimit() {
        return modern.getMaxStackSize();
    }

    @Override
    public void markDirty() {
        Stacks1122.flushViews();
        modern.setChanged();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return player == null || !(player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) || modern.stillValid(p);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        if (player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) modern.startOpen(p);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        if (player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) modern.stopOpen(p);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return modern.canPlaceItem(index, Stacks1122.toModern(stack));
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        modern.clearContent();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasCustomName() {
        return modern instanceof net.minecraft.world.Nameable n && n.hasCustomName();
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        return hasCustomName() ? new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(getName())
                : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation(getName());
    }
}
