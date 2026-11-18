package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The beacon GUI container: the payment slot, then the player inventory. */
public class ContainerBeacon extends Container {
    private final IInventory tileBeacon;
    private final Slot beaconSlot;

    public ContainerBeacon(IInventory playerInventory, IInventory tileBeaconIn) {
        this.tileBeacon = tileBeaconIn;
        this.beaconSlot = new Slot(tileBeaconIn, 0, 136, 110) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                String name = String.valueOf(stack.getItem().getRegistryName());
                return name.equals("minecraft:emerald") || name.equals("minecraft:diamond") || name.equals("minecraft:gold_ingot") || name.equals("minecraft:iron_ingot");
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        };
        addSlotToContainer(beaconSlot);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 36 + j * 18, 137 + i * 18));
        }
        for (int k = 0; k < 9; ++k) addSlotToContainer(new Slot(playerInventory, k, 36 + k * 18, 195));
    }

    public IInventory getTileEntity() {
        return tileBeacon;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        if (!playerIn.getEntityWorld().isRemote) {
            ItemStack stack = beaconSlot.decrStackSize(beaconSlot.getSlotStackLimit());
            if (!stack.isEmpty()) playerIn.dropItem(stack, false);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tileBeacon.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            boolean failed;
            if (index == 0) {
                failed = !mergeItemStack(itemstack1, 1, 37, true);
                if (!failed) slot.onSlotChange(itemstack1, itemstack);
            } else if (beaconSlot.isItemValid(itemstack1) && itemstack1.getCount() == 1) {
                failed = !mergeItemStack(itemstack1, 0, 1, false);
            } else if (index < 28) {
                failed = !mergeItemStack(itemstack1, 28, 37, false);
            } else {
                failed = !mergeItemStack(itemstack1, 1, 28, false);
            }
            if (failed) return ItemStack.EMPTY;
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }
}
