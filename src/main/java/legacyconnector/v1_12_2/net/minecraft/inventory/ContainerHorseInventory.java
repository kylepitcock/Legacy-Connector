package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.passive.AbstractChestHorse;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.AbstractHorse;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The horse GUI's container: saddle and armour slots, the saddle bags of a chested horse, then the player's inventory. */
public class ContainerHorseInventory extends Container {
    private final IInventory horseInventory;
    public final AbstractHorse horse;

    public ContainerHorseInventory(IInventory playerInventory, IInventory horseInventoryIn, final AbstractHorse horse, EntityPlayer player) {
        this.horseInventory = horseInventoryIn;
        this.horse = horse;
        horseInventoryIn.openInventory(player);
        addSlotToContainer(new Slot(horseInventoryIn, 0, 8, 18) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.SADDLE && !getHasStack() && horse.canBeSaddled();
            }
        });
        addSlotToContainer(new Slot(horseInventoryIn, 1, 8, 36) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return horse.isArmor(stack);
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });
        if (horse instanceof AbstractChestHorse chested && chested.hasChest()) {
            for (int j = 0; j < 3; ++j) {
                for (int k = 0; k < 5; ++k) addSlotToContainer(new Slot(horseInventoryIn, 2 + k + j * 5, 80 + k * 18, 18 + j * 18));
            }
        }
        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < 9; ++j1) addSlotToContainer(new Slot(playerInventory, j1 + l * 9 + 9, 8 + j1 * 18, 84 + l * 18));
        }
        for (int i1 = 0; i1 < 9; ++i1) addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return horseInventory.isUsableByPlayer(playerIn) && horse.isEntityAlive() && horse.getDistance(playerIn) < 8.0F;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            int own = horseInventory.getSizeInventory();
            if (index < own) {
                if (!mergeItemStack(itemstack1, own, inventorySlots.size(), true)) return ItemStack.EMPTY;
            } else if (getSlot(1).isItemValid(itemstack1) && !getSlot(1).getHasStack()) {
                if (!mergeItemStack(itemstack1, 1, 2, false)) return ItemStack.EMPTY;
            } else if (getSlot(0).isItemValid(itemstack1)) {
                if (!mergeItemStack(itemstack1, 0, 1, false)) return ItemStack.EMPTY;
            } else if (own <= 2 || !mergeItemStack(itemstack1, 2, own, false)) {
                int start = own;
                int hotbarStart = start + 27;
                int end = hotbarStart + 9;
                if (index >= hotbarStart && index < end) {
                    if (!mergeItemStack(itemstack1, start, hotbarStart, false)) return ItemStack.EMPTY;
                } else if (index >= start && index < hotbarStart) {
                    if (!mergeItemStack(itemstack1, hotbarStart, end, false)) return ItemStack.EMPTY;
                } else if (!mergeItemStack(itemstack1, start, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
        }
        return itemstack;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        horseInventory.closeInventory(playerIn);
    }
}
