package legacyconnector.v1_7_10.net.minecraft.dispenser;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

public interface IBehaviorDispenseItem {
    /** Dispenses one use of {@code stack}; returns what is left in the dispenser slot. */
    ItemStack dispense(IBlockSource source, ItemStack stack);
}
