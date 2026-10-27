package org.kylepitcock.legacyconnector.v1_12_2.gui;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * Modern slot delegating every decision to a 1.12.2 slot (validation, stack limits, pickup hooks, enabled state).
 *
 * <p>1.12 stacks are converted, not shared, so the modern stack handed out by {@link #getItem} is cached while the legacy stack stays
 * the same: modern menu code changes that stack in place and then calls {@link #setChanged}, which writes the change back.</p>
 */
public final class LegacySlot1122 extends Slot {
    private final legacyconnector.v1_12_2.net.minecraft.inventory.Slot legacy;
    private ItemStack cachedLegacy;
    private int cachedCount;
    private int cachedMeta;
    private net.minecraft.world.item.ItemStack cachedModern = net.minecraft.world.item.ItemStack.EMPTY;
    private int handedCount;
    private boolean logged;

    public LegacySlot1122(legacyconnector.v1_12_2.net.minecraft.inventory.Slot legacy) {
        super(LegacyInventory1122.of(legacy.inventory), legacy.getSlotIndex(), legacy.xPos, legacy.yPos);
        this.legacy = legacy;
    }

    public legacyconnector.v1_12_2.net.minecraft.inventory.Slot legacy() {
        return legacy;
    }

    private static EntityPlayer player(Player p) {
        return EntityBridge1122.wrap(p) instanceof EntityPlayer ep ? ep : null;
    }

    private ItemStack legacyStack() {
        try {
            ItemStack s = legacy.getStack();
            return s == null ? ItemStack.EMPTY : s;
        } catch (RuntimeException | LinkageError e) {
            // a slot whose inventory is smaller than the container assumed (client/server mismatch) shows as empty
            if (!logged) {
                logged = true;
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Gui").warn("Legacy slot {} of {} cannot be read: {}", legacy.slotNumber, legacy.getClass().getName(), e.toString());
            }
            return ItemStack.EMPTY;
        }
    }

    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        ItemStack s = legacyStack();
        // while the legacy stack is unchanged the same modern stack is returned, also after modern code changed it in place
        if (s != cachedLegacy || s.getCount() != cachedCount || s.getMetadata() != cachedMeta) {
            cachedLegacy = s;
            cachedCount = s.getCount();
            cachedMeta = s.getMetadata();
            cachedModern = Stacks1122.toModern(s);
            handedCount = cachedModern.getCount();
        }
        return cachedModern;
    }

    @Override
    public void set(net.minecraft.world.item.ItemStack stack) {
        legacy.putStack(Stacks1122.fromModern(stack));
        cachedLegacy = null;
    }

    @Override
    public void setByPlayer(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemStack previous) {
        set(stack);
    }

    @Override
    public net.minecraft.world.item.ItemStack remove(int amount) {
        cachedLegacy = null;
        return Stacks1122.toModern(legacy.decrStackSize(amount));
    }

    @Override
    public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
        return legacy.isItemValid(Stacks1122.fromModern(stack));
    }

    @Override
    public int getMaxStackSize() {
        return legacy.getSlotStackLimit();
    }

    @Override
    public int getMaxStackSize(net.minecraft.world.item.ItemStack stack) {
        return Math.min(legacy.getItemStackLimit(Stacks1122.fromModern(stack)), stack.getMaxStackSize());
    }

    @Override
    public boolean mayPickup(Player player) {
        EntityPlayer p = player(player);
        return p == null || legacy.canTakeStack(p);
    }

    @Override
    public void onTake(Player player, net.minecraft.world.item.ItemStack stack) {
        EntityPlayer p = player(player);
        if (p != null) legacy.onTake(p, Stacks1122.fromModern(stack));
    }

    @Override
    public void onQuickCraft(net.minecraft.world.item.ItemStack moved, net.minecraft.world.item.ItemStack original) {
        legacy.onSlotChange(Stacks1122.fromModern(moved), Stacks1122.fromModern(original));
    }

    /** Writes a stack the modern code changed in place back into the legacy inventory before telling the legacy slot. */
    @Override
    public void setChanged() {
        if (cachedLegacy != null && cachedLegacy == legacyStack() && cachedModern.getCount() != handedCount) {
            net.minecraft.world.item.ItemStack changed = cachedModern;
            cachedLegacy = null;
            legacy.putStack(changed.isEmpty() ? ItemStack.EMPTY : Stacks1122.fromModern(changed));
            return;
        }
        legacy.onSlotChanged();
    }

    @Override
    public boolean isActive() {
        return legacy.isEnabled();
    }

    /** Forge's background sprite name ({@code modid:items/x}) on the block atlas, with vanilla's renamed folders. */
    @Override
    public com.mojang.datafixers.util.Pair<net.minecraft.resources.ResourceLocation, net.minecraft.resources.ResourceLocation> getNoItemIcon() {
        String name = legacy.getSlotTexture();
        if (name == null || name.isEmpty()) return null;
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(name.toLowerCase(java.util.Locale.ROOT));
        if (rl == null) return null;
        String path = rl.getPath().startsWith("items/") ? "item/" + rl.getPath().substring(6)
                : rl.getPath().startsWith("blocks/") ? "block/" + rl.getPath().substring(7) : rl.getPath();
        return com.mojang.datafixers.util.Pair.of(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), path));
    }
}
