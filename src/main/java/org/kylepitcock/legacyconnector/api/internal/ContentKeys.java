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
            case V1_20_1: {
                // the legacy item IS the modern one: its key is the registry id, damage is the stack's damage value
                return Optional.of(new LegacyItemKey(version, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getDamageValue(), stack.getItem()));
            }
            case V1_7_10:
            case V1_4_7:
            case V1_5_2:
            case V1_6_4: {
                var item = ItemBridge.legacyItemOf(stack);
                if (item == null) return Optional.empty();
                String name = ItemBridge.nameOf(item);
                return Optional.of(new LegacyItemKey(version, name, ItemBridge.legacyDamage(item, stack), item));
            }
            case VB1_7_3: {
                var legacy = org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.toLegacy(stack);
                if (legacy == null) return Optional.empty();
                var item = legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS[legacy.itemId];
                // a stack Beta cannot represent is carried by one reserved item; its key is still the modern id
                return Optional.of(new LegacyItemKey(version, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), legacy.getDamage(), item));
            }
            case V1_12_2:
            default: {
                var legacy = Convert.standIn1122(stack);
                if (legacy.isEmpty()) return Optional.empty();
                var item = legacy.getItem();
                var name = item.getRegistryName();
                return Optional.of(new LegacyItemKey(version, name == null ? null : name.toString(), legacy.getMetadata(), item));
            }
        }
    }
}
