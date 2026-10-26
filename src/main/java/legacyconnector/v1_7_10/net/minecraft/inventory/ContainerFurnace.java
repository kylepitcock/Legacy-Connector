package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFurnace;

/**
 * The 1.7.10 furnace container, ported from vanilla: input slot 0, fuel slot 1, output slot 2, then the player's inventory
 * (3-29) and hotbar (30-38). Cook time, burn time and the current fuel's burn time are synced as progress bars 0, 1 and 2.
 */
public class ContainerFurnace extends Container {
    private TileEntityFurnace tileFurnace;
    private int lastCookTime;
    private int lastBurnTime;
    private int lastItemBurnTime;

    public ContainerFurnace(InventoryPlayer inventory, TileEntityFurnace furnace) {
        this.tileFurnace = furnace;
        addSlotToContainer(new Slot(furnace, 0, 56, 17));
        addSlotToContainer(new Slot(furnace, 1, 56, 53));
        addSlotToContainer(new SlotFurnace(inventory.player, furnace, 2, 116, 35));
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; ++col) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 142));
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        crafter.sendProgressBarUpdate(this, 0, tileFurnace.furnaceCookTime);
        crafter.sendProgressBarUpdate(this, 1, tileFurnace.furnaceBurnTime);
        crafter.sendProgressBarUpdate(this, 2, tileFurnace.currentItemBurnTime);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (Object o : crafters) {
            ICrafting crafter = (ICrafting) o;
            if (lastCookTime != tileFurnace.furnaceCookTime) crafter.sendProgressBarUpdate(this, 0, tileFurnace.furnaceCookTime);
            if (lastBurnTime != tileFurnace.furnaceBurnTime) crafter.sendProgressBarUpdate(this, 1, tileFurnace.furnaceBurnTime);
            if (lastItemBurnTime != tileFurnace.currentItemBurnTime) crafter.sendProgressBarUpdate(this, 2, tileFurnace.currentItemBurnTime);
        }
        lastCookTime = tileFurnace.furnaceCookTime;
        lastBurnTime = tileFurnace.furnaceBurnTime;
        lastItemBurnTime = tileFurnace.currentItemBurnTime;
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) tileFurnace.furnaceCookTime = value;
        if (id == 1) tileFurnace.furnaceBurnTime = value;
        if (id == 2) tileFurnace.currentItemBurnTime = value;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tileFurnace.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack copy = null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            copy = stack.copy();
            if (index == 2) {
                if (!mergeItemStack(stack, 3, 39, true)) return null;
                slot.onSlotChange(stack, copy);
            } else if (index != 1 && index != 0) {
                if (FurnaceRecipes.smelting().getSmeltingResult(stack) != null) {
                    if (!mergeItemStack(stack, 0, 1, false)) return null;
                } else if (TileEntityFurnace.isItemFuel(stack)) {
                    if (!mergeItemStack(stack, 1, 2, false)) return null;
                } else if (index >= 3 && index < 30) {
                    if (!mergeItemStack(stack, 30, 39, false)) return null;
                } else if (index >= 30 && index < 39 && !mergeItemStack(stack, 3, 30, false)) {
                    return null;
                }
            } else if (!mergeItemStack(stack, 3, 39, false)) {
                return null;
            }
            if (stack.mcc$count() == 0) slot.putStack(null);
            else slot.onSlotChanged();
            if (stack.mcc$count() == copy.mcc$count()) return null;
            slot.onPickupFromSlot(player, stack);
        }
        return copy;
    }
}
