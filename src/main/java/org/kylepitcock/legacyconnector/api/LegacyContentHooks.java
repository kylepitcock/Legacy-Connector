package org.kylepitcock.legacyconnector.api;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.kylepitcock.legacyconnector.api.internal.ContentKeys;
import org.kylepitcock.legacyconnector.api.internal.Convert;

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

    // ------------------------------------------------------------------ numeric ids (added in API 0.2)

    /**
     * The numeric block id a Beta 1.7.3 mod sees (its fixed id 0-255; Beta mods keep the ids they claimed, from API 0.3), or a 1.4.7, 1.5.2 or 1.6.4 mod sees for a legacy block object (each version has its own id space; a block nobody
     * claimed gets the next free id). Empty for other versions or objects.
     */
    public static java.util.OptionalInt numericBlockId(Object legacyBlock, LegacyVersion version) {
        if (version.isBeta()) return Convert.numericBlockId(legacyBlock, version);
        return version.usesNumericIds() ? Convert.numericBlockId(legacyBlock, version) : java.util.OptionalInt.empty();
    }

    /** As {@link #numericBlockId} for a legacy {@code Item} or {@code ItemStack} (an ItemBlock has its block's id). */
    public static java.util.OptionalInt numericItemId(Object legacyItemOrStack, LegacyVersion version) {
        if (version.isBeta()) return Convert.numericItemId(legacyItemOrStack, version);
        return version.usesNumericIds() ? Convert.numericItemId(legacyItemOrStack, version) : java.util.OptionalInt.empty();
    }

    /** {@code Block.blocksList[id]} of a numeric-id version; empty for an unused id or another version. */
    public static Optional<Object> legacyBlockById(int id, LegacyVersion version) {
        if (version.isBeta()) return Convert.legacyBlockById(id, version);
        return version.usesNumericIds() ? Convert.legacyBlockById(id, version) : Optional.empty();
    }

    /** {@code Item.itemsList[id]} of a numeric-id version; empty for an unused id or another version. */
    public static Optional<Object> legacyItemById(int id, LegacyVersion version) {
        if (version.isBeta()) return Convert.legacyItemById(id, version);
        return version.usesNumericIds() ? Convert.legacyItemById(id, version) : Optional.empty();
    }
}
