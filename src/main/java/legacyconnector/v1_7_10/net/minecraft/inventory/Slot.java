package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * 1.7.10 slot. Purely legacy logic; the modern menu wraps each one in a modern slot that defers to these methods,
 * so overridden {@link #isItemValid}, {@link #getSlotStackLimit} etc. keep working.
 */
public class Slot {
    private final int slotIndex;
    public final IInventory inventory;
    public int slotNumber;
    public int xDisplayPosition;
    public int yDisplayPosition;
    protected IIcon backgroundIcon;
    protected ResourceLocation texture;

    public Slot(IInventory inventory, int index, int x, int y) {
        this.inventory = inventory;
        this.slotIndex = index;
        this.xDisplayPosition = x;
        this.yDisplayPosition = y;
    }

    public void onSlotChange(ItemStack a, ItemStack b) {
        if (a != null && b != null && a.getItem() == b.getItem()) {
            int i = b.mcc$count() - a.mcc$count();
            if (i > 0) onCrafting(a, i);
        }
    }

    protected void onCrafting(ItemStack stack, int amount) {
    }

    protected void onCrafting(ItemStack stack) {
    }

    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
        onSlotChanged();
    }

    public boolean isItemValid(ItemStack stack) {
        return true;
    }

    public ItemStack getStack() {
        return inventory.getStackInSlot(slotIndex);
    }

    public boolean getHasStack() {
        return getStack() != null;
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

    public IIcon getBackgroundIconIndex() {
        return backgroundIcon;
    }

    public ItemStack decrStackSize(int amount) {
        return inventory.decrStackSize(slotIndex, amount);
    }

    public boolean isSlotInInventory(IInventory inv, int index) {
        return inv == inventory && index == slotIndex;
    }

    public boolean canTakeStack(EntityPlayer player) {
        return true;
    }

    public boolean canBeHovered() {
        return true;
    }

    public ResourceLocation getBackgroundIconTexture() {
        return texture == null ? new ResourceLocation("textures/atlas/items.png") : texture;
    }

    public void setBackgroundIcon(IIcon icon) {
        backgroundIcon = icon;
    }

    public void setBackgroundIconTexture(ResourceLocation tex) {
        texture = tex;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
