package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.CraftingManager;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The 1.7.10 crafting table container, ported from vanilla: result slot 0, the 3x3 grid in slots 1-9, the player's inventory in
 * 10-36 and the hotbar in 37-45. The result comes from {@link CraftingManager#findMatchingRecipe} (legacy recipes, then the modern
 * recipe book). Mods subclass it for their own tables (Avaritia, Botania's crafting halo) and read {@link #craftMatrix} directly.
 */
public class ContainerWorkbench extends Container {
    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
    public IInventory craftResult = new InventoryCraftResult();
    private World worldObj;
    private int posX;
    private int posY;
    private int posZ;

    public ContainerWorkbench(InventoryPlayer inventory, World world, int x, int y, int z) {
        this.worldObj = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        addSlotToContainer(new SlotCrafting(inventory.player, craftMatrix, craftResult, 0, 124, 35));
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) addSlotToContainer(new Slot(craftMatrix, col + row * 3, 30 + col * 18, 17 + row * 18));
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; ++col) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 142));
        onCraftMatrixChanged(craftMatrix);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inv) {
        craftResult.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(craftMatrix, worldObj));
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (worldObj != null && !worldObj.isRemote) {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = craftMatrix.getStackInSlotOnClosing(i);
                if (stack != null) player.dropPlayerItemWithRandomChoice(stack, false);
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return worldObj.getBlock(posX, posY, posZ) == Blocks.crafting_table
                && player.getDistanceSq(posX + 0.5D, posY + 0.5D, posZ + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack copy = null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            copy = stack.copy();
            if (index == 0) {
                if (!mergeItemStack(stack, 10, 46, true)) return null;
                slot.onSlotChange(stack, copy);
            } else if (index >= 10 && index < 37) {
                if (!mergeItemStack(stack, 37, 46, false)) return null;
            } else if (index >= 37 && index < 46) {
                if (!mergeItemStack(stack, 10, 37, false)) return null;
            } else if (!mergeItemStack(stack, 10, 46, false)) {
                return null;
            }
            if (stack.mcc$count() == 0) slot.putStack(null);
            else slot.onSlotChanged();
            if (stack.mcc$count() == copy.mcc$count()) return null;
            slot.onPickupFromSlot(player, stack);
        }
        return copy;
    }

    @Override
    public boolean func_94530_a(ItemStack stack, Slot slot) {
        return slot.inventory != craftResult && super.func_94530_a(stack, slot);
    }
}
