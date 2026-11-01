package legacyconnector.v1_12_2.baubles.api.inv;

import legacyconnector.v1_12_2.baubles.api.cap.IBaublesItemHandler;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import org.kylepitcock.legacyconnector.compat.curios.CuriosBaubles1122;

/** Baubles 1.12's IInventory view of a player's Baubles slots (what the deprecated {@code BaublesApi.getBaubles} returns): the Curios-backed inventory. */
public class BaublesInventoryWrapper implements IInventory {
    private final IInventory inner;

    public BaublesInventoryWrapper(IBaublesItemHandler handler, EntityPlayer player) {
        this.inner = player == null ? null : CuriosBaubles1122.inventory(player);
    }

    public int getSizeInventory() { return inner == null ? 0 : inner.getSizeInventory(); }

    public boolean isEmpty() { return inner == null || inner.isEmpty(); }

    public ItemStack getStackInSlot(int index) { return inner == null ? ItemStack.EMPTY : inner.getStackInSlot(index); }

    public ItemStack decrStackSize(int index, int count) { return inner == null ? ItemStack.EMPTY : inner.decrStackSize(index, count); }

    public ItemStack removeStackFromSlot(int index) { return inner == null ? ItemStack.EMPTY : inner.removeStackFromSlot(index); }

    public void setInventorySlotContents(int index, ItemStack stack) { if (inner != null) inner.setInventorySlotContents(index, stack); }

    public int getInventoryStackLimit() { return 1; }

    public void markDirty() { if (inner != null) inner.markDirty(); }

    public boolean isUsableByPlayer(EntityPlayer player) { return true; }

    public void openInventory(EntityPlayer player) { }

    public void closeInventory(EntityPlayer player) { }

    public boolean isItemValidForSlot(int index, ItemStack stack) { return inner != null && inner.isItemValidForSlot(index, stack); }

    public int getField(int id) { return 0; }

    public void setField(int id, int value) { }

    public int getFieldCount() { return 0; }

    public void clear() { if (inner != null) inner.clear(); }

    public String getName() { return "baubles"; }

    public boolean hasCustomName() { return false; }

    public ITextComponent getDisplayName() { return inner == null ? null : inner.getDisplayName(); }
}
