package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityHorse;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * The menu of a horse's inventory (1.7.10 {@code ContainerHorseInventory}): saddle slot, armour slot (horses only), the chest slots of a
 * chested horse, then the player inventory.
 */
public class ContainerHorseInventory extends Container {
    private final IInventory field_111243_a;
    private final EntityHorse field_111242_f;

    public ContainerHorseInventory(IInventory playerInventory, IInventory horseInventory, EntityHorse horse) {
        this.field_111243_a = horseInventory;
        this.field_111242_f = horse;
        horseInventory.openInventory();
        addSlotToContainer(new Slot(horseInventory, 0, 8, 18) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return super.isItemValid(stack) && stack.getItem() == Items.saddle && !getHasStack();
            }
        });
        addSlotToContainer(new Slot(horseInventory, 1, 8, 36) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return super.isItemValid(stack) && field_111242_f.getHorseType() == 0 && isHorseArmor(stack);
            }
        });
        if (horse.isChested()) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 5; col++) addSlotToContainer(new Slot(horseInventory, 2 + col + row * 5, 80 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, row * 18 + 102));
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 160));
    }

    private static boolean isHorseArmor(ItemStack stack) {
        return stack != null && (stack.getItem() == Items.iron_horse_armor || stack.getItem() == Items.golden_horse_armor
                || stack.getItem() == Items.diamond_horse_armor);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return field_111243_a.isUseableByPlayer(player) && field_111242_f.isEntityAlive() && field_111242_f.getDistanceToEntity(player) < 8.0F;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < field_111243_a.getSizeInventory()) {
                if (!mergeItemStack(stack, field_111243_a.getSizeInventory(), inventorySlots.size(), true)) return null;
            } else if (getSlot(1).isItemValid(stack) && !getSlot(1).getHasStack()) {
                if (!mergeItemStack(stack, 1, 2, false)) return null;
            } else if (getSlot(0).isItemValid(stack)) {
                if (!mergeItemStack(stack, 0, 1, false)) return null;
            } else if (field_111243_a.getSizeInventory() <= 2 || !mergeItemStack(stack, 2, field_111243_a.getSizeInventory(), false)) {
                return null;
            }
            if (stack.mcc$count() == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }
        return result;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        field_111243_a.closeInventory();
    }
}
