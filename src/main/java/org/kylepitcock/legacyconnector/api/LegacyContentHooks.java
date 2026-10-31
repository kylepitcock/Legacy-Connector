package org.kylepitcock.legacyconnector.api;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.kylepitcock.legacyconnector.api.internal.ContentKeys;

/**
 * Resolves modern registry ids to the keys a legacy mod expects, so an addon can feed modern content into a legacy mod's data tables
 * (Thaumcraft's {@code registerObjectTag}, ore dictionary entries, recipe inputs) with the right legacy stack.
 */
public final class LegacyContentHooks {
    private LegacyContentHooks() {}

    /**
     * The identity a legacy mod sees for one modern item.
     *
     * @param version    the legacy version the key is for
     * @param legacyName the legacy registry name of the item (a stand-in's name is the modern id, e.g. {@code minecraft:copper_ingot})
     * @param damage     the legacy damage value / metadata (0 for items without subtypes)
     * @param legacyItem the legacy {@code Item} object (opaque)
     */
    public record LegacyItemKey(LegacyVersion version, String legacyName, int damage, Object legacyItem) {}

    /** The key of a modern item as seen by the legacy version; empty if the item does not exist. */
    public static Optional<LegacyItemKey> itemKey(ResourceLocation modernItemId, LegacyVersion version) {
        Item item = BuiltInRegistries.ITEM.getOptional(modernItemId).orElse(null);
        return item == null ? Optional.empty() : ContentKeys.itemKey(new ItemStack(item), version);
    }

    /** As {@link #itemKey(ResourceLocation, LegacyVersion)} for a concrete stack (its damage and variant count). */
    public static Optional<LegacyItemKey> itemKey(ItemStack stack, LegacyVersion version) {
        return stack.isEmpty() ? Optional.empty() : ContentKeys.itemKey(stack, version);
    }

    /**
     * The legacy stack (opaque) for a modern item id: what to pass to a legacy API that takes an {@code ItemStack}. Empty if the item
     * does not exist.
     */
    public static Optional<Object> legacyStack(ResourceLocation modernItemId, int count, LegacyVersion version) {
        Item item = BuiltInRegistries.ITEM.getOptional(modernItemId).orElse(null);
        if (item == null) return Optional.empty();
        return Optional.ofNullable(LegacyItems.toLegacy(new ItemStack(item, Math.max(1, count)), version));
    }

    /**
     * The legacy stack for the item of a modern block (e.g. {@code minecraft:deepslate}), or empty when the block has no item or does
     * not exist.
     */
    public static Optional<Object> legacyBlockStack(ResourceLocation modernBlockId, int count, LegacyVersion version) {
        Block block = BuiltInRegistries.BLOCK.getOptional(modernBlockId).orElse(null);
        if (block == null || block.asItem() == net.minecraft.world.item.Items.AIR) return Optional.empty();
        return Optional.ofNullable(LegacyItems.toLegacy(new ItemStack(block, Math.max(1, count)), version));
    }

    /** The modern item id of a legacy item object plus damage; empty when it has no modern counterpart. */
    public static Optional<ResourceLocation> modernItemId(Object legacyStack) {
        ItemStack modern = LegacyItems.fromLegacy(legacyStack);
        return modern.isEmpty() ? Optional.empty() : Optional.of(BuiltInRegistries.ITEM.getKey(modern.getItem()));
    }
}
