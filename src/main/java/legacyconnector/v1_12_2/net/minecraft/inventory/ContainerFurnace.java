package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/**
 * The furnace GUI's container: input, fuel and output over the player's inventory; the furnace's four fields (burn time, item burn
 * time, cook time, total cook time) go to the client as window properties.
 */
public class ContainerFurnace extends Container {
    private final IInventory tileFurnace;
    private int cookTime;
    private int totalCookTime;
    private int furnaceBurnTime;
    private int currentItemBurnTime;

    public ContainerFurnace(InventoryPlayer playerInventory, IInventory furnaceInventory) {
        this.tileFurnace = furnaceInventory;
        addSlotToContainer(new Slot(furnaceInventory, 0, 56, 17));
        addSlotToContainer(new SlotFurnaceFuel(furnaceInventory, 1, 56, 53));
        addSlotToContainer(new SlotFurnaceOutput(playerInventory.player, furnaceInventory, 2, 116, 35));
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        }
        for (int k = 0; k < 9; ++k) addSlotToContainer(new Slot(playerInventory, k, 8 + k * 18, 142));
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, tileFurnace);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (IContainerListener l : listeners) {
            if (cookTime != tileFurnace.getField(2)) l.sendWindowProperty(this, 2, tileFurnace.getField(2));
            if (furnaceBurnTime != tileFurnace.getField(0)) l.sendWindowProperty(this, 0, tileFurnace.getField(0));
            if (currentItemBurnTime != tileFurnace.getField(1)) l.sendWindowProperty(this, 1, tileFurnace.getField(1));
            if (totalCookTime != tileFurnace.getField(3)) l.sendWindowProperty(this, 3, tileFurnace.getField(3));
        }
        cookTime = tileFurnace.getField(2);
        furnaceBurnTime = tileFurnace.getField(0);
        currentItemBurnTime = tileFurnace.getField(1);
        totalCookTime = tileFurnace.getField(3);
    }

    @Override
    public void updateProgressBar(int id, int data) {
        tileFurnace.setField(id, data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tileFurnace.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index == 2) {
                if (!mergeItemStack(itemstack1, 3, 39, true)) return ItemStack.EMPTY;
                slot.onSlotChange(itemstack1, itemstack);
            } else if (index != 1 && index != 0) {
                if (!legacyconnector.v1_12_2.net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingResult(itemstack1).isEmpty()) {
                    if (!mergeItemStack(itemstack1, 0, 1, false)) return ItemStack.EMPTY;
                } else if (org.kylepitcock.legacyconnector.v1_12_2.bridge.SynthItems1122.burnTime(itemstack1) > 0) {
                    if (!mergeItemStack(itemstack1, 1, 2, false)) return ItemStack.EMPTY;
                } else if (index >= 3 && index < 30) {
                    if (!mergeItemStack(itemstack1, 30, 39, false)) return ItemStack.EMPTY;
                } else if (index >= 30 && index < 39 && !mergeItemStack(itemstack1, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }
}
