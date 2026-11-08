package legacyconnector.v1_7_10.net.minecraft.dispenser;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

public interface IBehaviorDispenseItem {
    /** 1.6.4: the behaviour that does nothing and leaves the stack as it is. */
    IBehaviorDispenseItem itemDispenseBehaviorProvider = (source, stack) -> stack;

    /** Dispenses one use of {@code stack}; returns what is left in the dispenser slot. */
    ItemStack dispense(IBlockSource source, ItemStack stack);
}
