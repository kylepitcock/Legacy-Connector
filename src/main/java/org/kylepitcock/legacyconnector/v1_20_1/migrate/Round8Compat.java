package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;

/** Common-side 1.20.1 members that 1.21.1 reshaped, ATM 9 gap round 8 (rows in {@code mc-calls.tsv}). */
public final class Round8Compat {
    private Round8Compat() {
    }

    /**
     * {@code new InventoryChangeTrigger.TriggerInstance(player, occupied, full, empty, items)}: the player predicate is optional and the three slot bounds
     * are one {@code Slots} record now; the item list is a list.
     */
    public static InventoryChangeTrigger.TriggerInstance newInventoryChange(ContextAwarePredicate player, MinMaxBounds.Ints occupied, MinMaxBounds.Ints full,
            MinMaxBounds.Ints empty, ItemPredicate[] items) {
        return new InventoryChangeTrigger.TriggerInstance(Optional.ofNullable(player),
                new InventoryChangeTrigger.TriggerInstance.Slots(occupied, full, empty), List.of(items));
    }
}
