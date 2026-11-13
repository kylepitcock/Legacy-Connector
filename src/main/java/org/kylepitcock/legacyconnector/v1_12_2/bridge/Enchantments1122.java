package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.12.2 mod enchantments -> data-driven modern enchantments. 1.21 enchantments live in a data pack registry, so each mod enchantment is
 * written as an enchantment file of its modern id (its weight, levels, costs, slots and translation key from the legacy object), the items
 * it applies to as an item tag (the legacy {@code canApply} over every item), and it joins the enchanting-table, loot and treasure tags as
 * the legacy flags say. That gives the enchantment its enchanted books (creative tab, loot, villagers) and lets it be put on items; the
 * legacy code reads its level back through {@code EnchantmentHelper}.
 */
public final class Enchantments1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Enchantments1122");

    private Enchantments1122() {}

    /** The modern id of a legacy enchantment's registry name (vanilla ones keep theirs). */
    public static ResourceLocation modernId(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation name) {
        if (name == null) return null;
        return ResourceLocation.tryBuild(Ids.modernModId(name.getNamespace()), Ids.normalizePath(name.getPath()));
    }

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        Map<String, Set<String>> tags = new TreeMap<>();
        int n = 0;
        for (Enchantment enchantment : Registries1122.ENCHANTMENTS.getValues()) {
            var name = enchantment == null ? null : enchantment.getRegistryName();
            if (name == null || "minecraft".equals(name.getNamespace())) continue;
            ResourceLocation id = modernId(name);
            if (id == null) continue;
            try {
                String itemsTag = id.getNamespace() + ":enchantable/" + id.getPath();
                Set<String> items = applicableItems(enchantment);
                JsonObject tag = new JsonObject();
                JsonArray values = new JsonArray();
                for (String item : items) values.add(item);
                tag.add("values", values);
                put(out, "data/" + id.getNamespace() + "/tags/item/enchantable/" + id.getPath() + ".json", tag);
                put(out, "data/" + id.getNamespace() + "/enchantment/" + id.getPath() + ".json", json(enchantment, "#" + itemsTag));
                String key = id.toString();
                boolean treasure = safe(enchantment::isTreasureEnchantment, false);
                if (treasure) tags.computeIfAbsent("treasure", k -> new LinkedHashSet<>()).add(key);
                else if (safe(enchantment::isAllowedOnBooks, true)) tags.computeIfAbsent("in_enchanting_table", k -> new LinkedHashSet<>()).add(key);
                if (safe(enchantment::isAllowedOnBooks, true)) {
                    tags.computeIfAbsent("on_random_loot", k -> new LinkedHashSet<>()).add(key);
                    tags.computeIfAbsent("tradeable", k -> new LinkedHashSet<>()).add(key);
                }
                if (safe(enchantment::isCurse, false)) tags.computeIfAbsent("curse", k -> new LinkedHashSet<>()).add(key);
                n++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not generate the enchantment {}: {}", name, e.toString());
            }
        }
        for (var e : tags.entrySet()) {
            JsonObject tag = new JsonObject();
            tag.addProperty("replace", false);
            JsonArray values = new JsonArray();
            for (String v : e.getValue()) values.add(v);
            tag.add("values", values);
            put(out, "data/minecraft/tags/enchantment/" + e.getKey() + ".json", tag);
        }
        if (n > 0) LOG.info("1.12.2: generated {} mod enchantment(s)", n);
    }

    private static JsonObject json(Enchantment enchantment, String itemsTag) {
        JsonObject o = new JsonObject();
        JsonObject description = new JsonObject();
        String key = safe(enchantment::getName, null);
        description.addProperty("translate", key != null ? key : "enchantment." + enchantment.getRegistryName().getPath());
        o.add("description", description);
        o.addProperty("supported_items", itemsTag);
        var rarity = enchantment.getRarity();
        o.addProperty("weight", Math.max(1, Math.min(1024, rarity == null ? 1 : rarity.getWeight())));
        int max = Math.max(1, Math.min(255, safe(enchantment::getMaxLevel, 1)));
        o.addProperty("max_level", max);
        o.add("min_cost", cost(enchantment, true));
        o.add("max_cost", cost(enchantment, false));
        o.addProperty("anvil_cost", rarity == null ? 4 : switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        });
        JsonArray slots = new JsonArray();
        EntityEquipmentSlot[] legacySlots = enchantment.mcc$slots();
        Set<String> names = new LinkedHashSet<>();
        if (legacySlots != null) for (EntityEquipmentSlot slot : legacySlots) if (slot != null) names.add(slot.getName());
        if (names.isEmpty()) names.add("any");
        for (String s : names) slots.add(s);
        o.add("slots", slots);
        return o;
    }

    /** A legacy cost curve as the modern linear one: the cost at level 1 and the step from level 1 to 2. */
    private static JsonObject cost(Enchantment enchantment, boolean min) {
        int one = safeCost(enchantment, min, 1);
        int two = safeCost(enchantment, min, 2);
        JsonObject o = new JsonObject();
        o.addProperty("base", one);
        o.addProperty("per_level_above_first", two - one);
        return o;
    }

    private static int safeCost(Enchantment enchantment, boolean min, int level) {
        try {
            return min ? enchantment.getMinEnchantability(level) : enchantment.getMaxEnchantability(level);
        } catch (RuntimeException | LinkageError e) {
            return min ? 1 + 10 * (level - 1) : 21 + 10 * (level - 1);
        }
    }

    /** The modern ids of every item the enchantment applies to (each subtype item of a legacy item with subtypes). */
    private static Set<String> applicableItems(Enchantment enchantment) {
        Set<String> out = new java.util.TreeSet<>();
        for (Item item : Registries1122.ITEMS.getValues()) {
            if (item == null) continue;
            boolean applies;
            try {
                applies = enchantment.canApply(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item));
            } catch (RuntimeException | LinkageError e) {
                applies = false;
            }
            if (!applies) continue;
            var variants = ItemBridge1122.variants(item);
            if (variants != null && !variants.isEmpty()) {
                for (var modern : variants.values()) add(out, modern);
            } else if (item.mcc$modern instanceof net.minecraft.world.item.Item modern) {
                add(out, modern);
            }
        }
        return out;
    }

    private static void add(Set<String> out, net.minecraft.world.item.Item modern) {
        if (modern == null || modern == net.minecraft.world.item.Items.AIR) return;
        var key = BuiltInRegistries.ITEM.getKey(modern);
        if (key != null) out.add(key.toString());
    }

    private static <T> T safe(Supplier<T> s, T fallback) {
        try {
            T v = s.get();
            return v != null ? v : fallback;
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    private static void put(Map<String, Supplier<byte[]>> out, String path, JsonObject json) {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }
}
