package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemCustomDataPredicate;
import net.minecraft.advancements.critereon.ItemDamagePredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPotionsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;

/**
 * 1.20.1 built an {@code ItemPredicate} from its parts with a constructor ({@code new ItemPredicate(tag, items, count, durability, enchantments,
 * storedEnchantments, potion, nbt)}); in 1.21.1 it is a record of an item set, a count, component predicates and sub-predicates. The same parts, in the new shape.
 * Also the {@code ANY} / {@code NONE} constants the predicate classes had.
 */
public final class PredicateCompat {
    private PredicateCompat() {}

    /** {@code new ItemPredicate(tag, items, count, durability, enchantments, storedEnchantments, potion, nbt)}; a null or ANY part matches everything. */
    public static ItemPredicate newItemPredicate(TagKey<Item> tag, Set<Item> items, MinMaxBounds.Ints count, MinMaxBounds.Ints durability, EnchantmentPredicate[] enchantments,
            EnchantmentPredicate[] storedEnchantments, Potion potion, NbtPredicate nbt) {
        Optional<HolderSet<Item>> set = Optional.empty();
        if (tag != null) {
            set = Optional.of(BuiltInRegistries.ITEM.getOrCreateTag(tag));
        } else if (items != null && !items.isEmpty()) {
            set = Optional.of(HolderSet.direct(Item::builtInRegistryHolder, new ArrayList<>(items)));
        }
        Map<ItemSubPredicate.Type<?>, ItemSubPredicate> subs = new LinkedHashMap<>();
        if (durability != null && !durability.isAny()) subs.put(ItemSubPredicates.DAMAGE, ItemDamagePredicate.durability(durability));
        if (enchantments != null && enchantments.length > 0) subs.put(ItemSubPredicates.ENCHANTMENTS, ItemEnchantmentsPredicate.enchantments(List.of(enchantments)));
        if (storedEnchantments != null && storedEnchantments.length > 0) {
            subs.put(ItemSubPredicates.STORED_ENCHANTMENTS, ItemEnchantmentsPredicate.storedEnchantments(List.of(storedEnchantments)));
        }
        if (potion != null) subs.put(ItemSubPredicates.POTIONS, ItemPotionsPredicate.potions(HolderSet.direct(BuiltInRegistries.POTION.wrapAsHolder(potion))));
        if (nbt != null && !nbt.tag().isEmpty()) subs.put(ItemSubPredicates.CUSTOM_DATA, ItemCustomDataPredicate.customData(nbt));
        return new ItemPredicate(set, count == null ? MinMaxBounds.Ints.ANY : count, DataComponentPredicate.EMPTY, subs);
    }

    /** {@code NbtPredicate.ANY}: matches every tag. */
    public static NbtPredicate nbtAny() {
        return new NbtPredicate(new CompoundTag());
    }

    /** {@code StatePropertiesPredicate.ANY}: no property constraints. */
    public static StatePropertiesPredicate stateAny() {
        return new StatePropertiesPredicate(List.of());
    }

    /** {@code EnchantmentPredicate.NONE}: the empty list of enchantment predicates. */
    public static EnchantmentPredicate[] enchantmentNone() {
        return new EnchantmentPredicate[0];
    }

    /** Helps arrays of predicates come out as lists. */
    static List<EnchantmentPredicate> list(EnchantmentPredicate[] a) {
        return a == null ? List.of() : Arrays.asList(a);
    }
}
