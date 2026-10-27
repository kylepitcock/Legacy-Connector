package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 slot (vanilla and Forge). Purely legacy logic over its inventory; a modern menu hosting the container wraps each slot in a
 * modern slot that defers to these methods, so overridden {@link #isItemValid}, {@link #getItemStackLimit}, {@link #onTake} keep working.
 */
public class Slot {
    private final int slotIndex;
    public final IInventory inventory;
    public int slotNumber;
    public int xPos;
    public int yPos;
    /** Forge: the background texture sheet and sprite name of an empty slot. */
    protected ResourceLocation backgroundLocation;
    protected String backgroundName;

    public Slot(IInventory inventory, int index, int xPosition, int yPosition) {
        this.inventory = inventory;
        this.slotIndex = index;
        this.xPos = xPosition;
        this.yPos = yPosition;
    }

    /** Called when the player shift-clicks out of a result slot: the difference counts as crafted. */
    public void onSlotChange(ItemStack oldStack, ItemStack newStack) {
        int i = newStack.getCount() - oldStack.getCount();
        if (i > 0) onCrafting(newStack, i);
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
        return inventory.getStackInSlot(slotIndex);
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

    /** The sprite name drawn in the empty slot (armour and shield outlines), or null. */
    public String getSlotTexture() {
        return backgroundName;
    }

    public ItemStack decrStackSize(int amount) {
        return inventory.decrStackSize(slotIndex, amount);
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

    // ------------------------------------------------------------------ Forge additions

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
        return inventory == other.inventory;
    }
}
