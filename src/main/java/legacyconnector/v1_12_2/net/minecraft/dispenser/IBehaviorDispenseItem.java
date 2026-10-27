package legacyconnector.v1_12_2.net.minecraft.dispenser;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** What a dispenser does with an item. */
public interface IBehaviorDispenseItem {
    IBehaviorDispenseItem DEFAULT_BEHAVIOR = (source, stack) -> stack;

    ItemStack dispense(IBlockSource source, ItemStack stack);

    /** This behaviour as the modern dispenser runs it. */
    static net.minecraft.core.dispenser.DispenseItemBehavior mcc$modern(IBehaviorDispenseItem behavior) {
        return (source, stack) -> {
            ItemStack legacy = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(stack);
            ItemStack out = behavior.dispense(BlockSourceImpl.mcc$of(source), legacy);
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(out == null ? ItemStack.EMPTY : out);
        };
    }
}
