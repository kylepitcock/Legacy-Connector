package legacyconnector.v1_12_2.net.minecraftforge.items.wrapper;

import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Forge: the four armour slots of a player's inventory (boots first); only armour for the slot goes in. */
public class PlayerArmorInvWrapper extends RangedWrapper {
    private final InventoryPlayer inventoryPlayer;

    public PlayerArmorInvWrapper(InventoryPlayer inv) {
        super(new InvWrapper(inv), 36, 40);
        this.inventoryPlayer = inv;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        EntityEquipmentSlot equ = null;
        for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) {
            if (s.getSlotType() == EntityEquipmentSlot.Type.ARMOR && s.getIndex() == slot) {
                equ = s;
                break;
            }
        }
        if (equ != null && slot < 4 && !stack.isEmpty() && stack.getItem().isValidArmor(stack, equ, inventoryPlayer.player)) {
            return super.insertItem(slot, stack, simulate);
        }
        return stack;
    }

    public InventoryPlayer getInventoryPlayer() {
        return inventoryPlayer;
    }
}
