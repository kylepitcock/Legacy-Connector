package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** 1.12.2 slot: one index of an inventory at a position in a container's screen. */
public class Slot {
    private final int slotIndex;
    public final IInventory inventory;
    public int slotNumber;
    public int xPos;
    public int yPos;
    /** Forge: the background icon (a texture sprite name and its atlas). */
    protected String backgroundName;
    protected legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation backgroundLocation;

    public Slot(IInventory inventory, int index, int xPosition, int yPosition) {
        this.inventory = inventory;
        this.slotIndex = index;
        this.xPos = xPosition;
        this.yPos = yPosition;
    }

    /** A stack was shift-clicked out: the difference counts as crafted. */
    public void onSlotChange(ItemStack newStack, ItemStack oldStack) {
        int i = oldStack.getCount() - newStack.getCount();
        if (i > 0) onCrafting(oldStack, i);
    }

    protected void onCrafting(ItemStack stack, int amount) {
    }

    protected void onSwapCraft(int numItemsCrafted) {
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

    public int getSlotIndex() {
        return slotIndex;
    }

    public boolean isSameInventory(Slot other) {
        return inventory == other.inventory;
    }

    public void setBackgroundName(String name) {
        this.backgroundName = name;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getBackgroundLocation() {
        return backgroundLocation != null ? backgroundLocation : new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("textures/atlas/blocks.png");
    }

    public void setBackgroundLocation(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation location) {
        this.backgroundLocation = location;
    }
}
