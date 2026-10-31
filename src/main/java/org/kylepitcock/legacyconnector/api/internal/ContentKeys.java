package org.kylepitcock.legacyconnector.api.internal;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.api.LegacyContentHooks.LegacyItemKey;
import org.kylepitcock.legacyconnector.api.LegacyVersion;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge;

/** Builds {@link LegacyItemKey}s from the item bridges. */
public final class ContentKeys {
    private ContentKeys() {}

    public static Optional<LegacyItemKey> itemKey(ItemStack stack, LegacyVersion version) {
        switch (version) {
            case V1_7_10: {
                var item = ItemBridge.legacyItemOf(stack);
                if (item == null) return Optional.empty();
                String name = ItemBridge.nameOf(item);
                return Optional.of(new LegacyItemKey(version, name, ItemBridge.legacyDamage(item, stack), item));
            }
            case V1_12_2:
            default: {
                var legacy = Stacks1122.fromModern(stack);
                if (legacy.isEmpty()) return Optional.empty();
                var item = legacy.getItem();
                var name = item.getRegistryName();
                return Optional.of(new LegacyItemKey(version, name == null ? null : name.toString(), legacy.getMetadata(), item));
            }
        }
    }
}
