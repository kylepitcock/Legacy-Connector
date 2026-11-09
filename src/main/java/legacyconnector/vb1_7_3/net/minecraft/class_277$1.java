package legacyconnector.vb1_7_3.net.minecraft;

import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.PlayerScreenHandler;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;

/** The mapped anonymous armor slot of Beta's player container. */
public class class_277$1 extends Slot {
    public final int field_1127;
    public final PlayerScreenHandler field_1128;
    public class_277$1(PlayerScreenHandler handler, Inventory inventory, int index, int x, int y, int type) {
        super(inventory, index, x, y);
        field_1128 = handler;
        field_1127 = type;
    }
    @Override public int getMaxItemCount() { return 1; }
    @Override public boolean canInsert(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        if (stack.getItem() instanceof ArmorItem armor) return armor.equipmentSlot == field_1127;
        return stack.itemId == 86 && field_1127 == 0; // Beta's vanilla pumpkin helmet.
    }
}
