package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 slot: one index of an inventory at a GUI position. Hosted by a modern slot ({@code LegacySlot1122}) that asks this one for
 * its stack, validity, limits and pickup hooks.
 */
public class Slot {
    private final int slotIndex;
    public final IInventory inventory;
    public int slotNumber;
    public int xPos;
    public int yPos;
    /** Forge: the sprite drawn in the empty slot (an atlas name such as {@code minecraft:items/empty_armor_slot_shield}). */
    protected String backgroundName;
    protected ResourceLocation backgroundLocation;
    protected Object backgroundMap;

    public Slot(IInventory inventory, int index, int xPosition, int yPosition) {
        this.inventory = inventory;
        this.slotIndex = index;
        this.xPos = xPosition;
        this.yPos = yPosition;
    }

    /** Called after a shift-click moved {@code moved} out of {@code original}: the difference counts as crafted. */
    public void onSlotChange(ItemStack moved, ItemStack original) {
        int i = original.getCount() - moved.getCount();
        if (i > 0) onCrafting(original, i);
    }

    protected void onCrafting(ItemStack stack, int amount) {
    }

    protected void onSwapCraft(int amount) {
    }

    protected void onCrafting(ItemStack stack) {
    }

    public ItemStack onTake(EntityPlayer player, ItemStack stack) {
        onSlotChanged();
        return stack;
    }

    public boolean isItemValid(ItemStack stack) {
        return true;
    }

    public ItemStack getStack() {
        ItemStack s = inventory.getStackInSlot(slotIndex);
        return s == null ? ItemStack.EMPTY : s;
    }

    public boolean getHasStack() {
        return !getStack().isEmpty();
    }

    public void putStack(ItemStack stack) {
        inventory.setInventorySlotContents(slotIndex, stack);
        onSlotChanged();
    }

    public void onSlotChanged() {
        inventory.markDirty();
    }

    public int getSlotStackLimit() {
        return inventory.getInventoryStackLimit();
    }

    public int getItemStackLimit(ItemStack stack) {
        return getSlotStackLimit();
    }

    public String getSlotTexture() {
        return backgroundName;
    }

    public ItemStack decrStackSize(int amount) {
        ItemStack s = inventory.decrStackSize(slotIndex, amount);
        return s == null ? ItemStack.EMPTY : s;
    }

    public boolean isHere(IInventory inv, int slotIn) {
        return inv == inventory && slotIn == slotIndex;
    }

    public boolean canTakeStack(EntityPlayer player) {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }

    /** The texture sheet the background sprite is on (the blocks atlas unless a mod set its own). */
    public ResourceLocation getBackgroundLocation() {
        return backgroundLocation == null ? new ResourceLocation("textures/atlas/blocks.png") : backgroundLocation;
    }

    public void setBackgroundLocation(ResourceLocation texture) {
        this.backgroundLocation = texture;
    }

    public void setBackgroundName(String name) {
        this.backgroundName = name;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public boolean isSameInventory(Slot other) {
        return other != null && inventory == other.inventory;
    }
}
