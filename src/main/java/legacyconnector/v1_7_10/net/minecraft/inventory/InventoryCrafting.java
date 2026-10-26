package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;

/**
 * Either a view over a modern crafting grid (handed to custom legacy IRecipe implementations), or the legacy
 * standalone grid that mod containers create with {@code new InventoryCrafting(container, 3, 3)}.
 */
public class InventoryCrafting implements IInventory {
    private final CraftingContainer mcc$grid;
    /** Legacy mode storage; null in view mode. */
    private final ItemStack[] stackList;
    private final int inventoryWidth;
    private final int inventoryHeight;
    private final Container eventHandler;

    public InventoryCrafting(CraftingContainer grid) {
        this.mcc$grid = grid;
        this.stackList = null;
        this.inventoryWidth = grid.getWidth();
        this.inventoryHeight = grid.getHeight();
        this.eventHandler = null;
    }

    public InventoryCrafting(Container eventHandler, int width, int height) {
        this.mcc$grid = null;
        this.stackList = new ItemStack[width * height];
        this.inventoryWidth = width;
        this.inventoryHeight = height;
        this.eventHandler = eventHandler;
    }

    @Override
    public int getSizeInventory() {
        return stackList != null ? stackList.length : mcc$grid.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= getSizeInventory()) return null;
        return stackList != null ? stackList[slot] : ItemStack.mcc$wrap(mcc$grid.getItem(slot));
    }

    public ItemStack getStackInRowAndColumn(int row, int column) {
        if (row < 0 || row >= inventoryWidth || column < 0 || column >= inventoryHeight) return null;
        return getStackInSlot(row + column * inventoryWidth);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (stackList == null) return ItemStack.mcc$wrap(mcc$grid.removeItem(slot, amount));
        ItemStack s = stackList[slot];
        if (s == null) return null;
        ItemStack out;
        if (ItemStack.mcc$get$stackSize(s) <= amount) {
            out = s;
            stackList[slot] = null;
        } else {
            out = s.splitStack(amount);
            if (ItemStack.mcc$get$stackSize(s) == 0) stackList[slot] = null;
        }
        changed();
        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (stackList == null) return ItemStack.mcc$wrap(mcc$grid.removeItemNoUpdate(slot));
        ItemStack s = stackList[slot];
        stackList[slot] = null;
        return s;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (stackList == null) {
            mcc$grid.setItem(slot, ItemStack.mcc$toModern(stack));
            return;
        }
        stackList[slot] = stack;
        changed();
    }

    /** The grid as a modern crafting input (for the modern recipe manager and the modern special recipes); null when it is empty. */
    public net.minecraft.world.item.crafting.CraftingInput mcc$craftingInput() {
        int w = inventoryWidth;
        int h = inventoryHeight;
        if (w <= 0 || h <= 0 || w * h != getSizeInventory()) return null;
        java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>(w * h);
        boolean any = false;
        for (int i = 0; i < w * h; i++) {
            net.minecraft.world.item.ItemStack s = ItemStack.mcc$toModern(getStackInSlot(i));
            any |= !s.isEmpty();
            items.add(s);
        }
        return any ? net.minecraft.world.item.crafting.CraftingInput.of(w, h, items) : null;
    }

    public int mcc$width() {
        return inventoryWidth;
    }

    public int mcc$height() {
        return inventoryHeight;
    }

    private void changed() {
        if (eventHandler != null) eventHandler.onCraftMatrixChanged(this);
    }

    @Override
    public String getInventoryName() {
        return "container.crafting";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        if (mcc$grid != null) mcc$grid.setChanged();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
}
