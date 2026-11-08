package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enchantments of translated 1.20.1 mods. They are objects registered in a registry of their own ({@code legacyconnector:legacy_enchantment}); a generated data
 * pack contributor writes the 1.21.1 data-driven entry for each (description, costs, supported items, exclusive set, weight) and puts it in the vanilla
 * enchanting table tag, so the game knows the enchantment under the mod's id. See {@link LegacyEnchantment}.
 */
public final class LegacyEnchantments {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Enchantments1201");
    public static final ResourceKey<Registry<LegacyEnchantment>> KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_enchantment"));
    public static final Registry<LegacyEnchantment> REGISTRY = new RegistryBuilder<>(KEY).sync(false).create();

    private LegacyEnchantments() {}

    /** Mod bus listener of the runtime. */
    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(REGISTRY);
    }

    public static ResourceLocation idOf(LegacyEnchantment e) {
        return REGISTRY.getKey(e);
    }

    // ------------------------------------------------------------------ the generated data pack entries

    /** {@code GeneratedPack.Contributor}: one enchantment file per registered legacy enchantment, plus the enchanting table tags. */
    public static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA || REGISTRY.size() == 0) return;
        List<LegacyEnchantment> all = new ArrayList<>();
        for (LegacyEnchantment e : REGISTRY) all.add(e);
        JsonArray inTable = new JsonArray();
        JsonArray tradeable = new JsonArray();
        JsonArray treasure = new JsonArray();
        JsonArray curse = new JsonArray();
        JsonArray nonTreasure = new JsonArray();
        JsonArray discoverable = new JsonArray();
        for (LegacyEnchantment e : all) {
            ResourceLocation id = REGISTRY.getKey(e);
            try {
                JsonObject json = definition(e, id, all);
                byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                out.put("data/" + id.getNamespace() + "/enchantment/" + id.getPath() + ".json", () -> bytes);
                String s = id.toString();
                if (e.isDiscoverable() && !e.isTreasureOnly()) inTable.add(s);
                if (e.isTradeable()) tradeable.add(s);
                if (e.isTreasureOnly()) treasure.add(s);
                else nonTreasure.add(s);
                if (e.isCurse()) curse.add(s);
                if (e.isDiscoverable()) discoverable.add(s);
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("enchantment {} could not be converted to a data pack entry: {}", id, ex.toString());
            }
        }
        tag(out, "in_enchanting_table", inTable);
        tag(out, "tradeable", tradeable);
        tag(out, "treasure", treasure);
        tag(out, "non_treasure", nonTreasure);
        tag(out, "curse", curse);
        tag(out, "on_random_loot", discoverable);
    }

    private static void tag(Map<String, Supplier<byte[]>> out, String name, JsonArray values) {
        if (values.isEmpty()) return;
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);
        byte[] bytes = tag.toString().getBytes(StandardCharsets.UTF_8);
        out.put("data/minecraft/tags/enchantment/" + name + ".json", () -> bytes);
    }

    private static JsonObject definition(LegacyEnchantment e, ResourceLocation id, List<LegacyEnchantment> all) {
        JsonObject json = new JsonObject();
        JsonObject description = new JsonObject();
        description.addProperty("translate", "enchantment." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
        json.add("description", description);
        JsonArray items = new JsonArray();
        for (Item item : BuiltInRegistries.ITEM) {
            boolean ok;
            try {
                ok = e.canEnchant(new ItemStack(item));
            } catch (RuntimeException | LinkageError ex) {
                ok = false;
            }
            if (ok) items.add(BuiltInRegistries.ITEM.getKey(item).toString());
        }
        json.add("supported_items", items);
        LegacyEnchantment.Rarity rarity = e.getRarity();
        json.addProperty("weight", rarity.getWeight());
        int max = Math.max(1, e.getMaxLevel());
        json.addProperty("max_level", max);
        json.add("min_cost", cost(e.getMinCost(1), max > 1 ? e.getMinCost(2) - e.getMinCost(1) : 0));
        json.add("max_cost", cost(e.getMaxCost(1), max > 1 ? e.getMaxCost(2) - e.getMaxCost(1) : 0));
        json.addProperty("anvil_cost", switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        });
        json.add("slots", slots(e.mcc$slots()));
        JsonArray exclusive = new JsonArray();
        for (LegacyEnchantment other : all) {
            if (other != e && !(e.mcc$checkCompatibility(other) && other.mcc$checkCompatibility(e))) exclusive.add(REGISTRY.getKey(other).toString());
        }
        if (!exclusive.isEmpty()) json.add("exclusive_set", exclusive);
        json.add("effects", new JsonObject());
        return json;
    }

    private static JsonObject cost(int base, int perLevel) {
        JsonObject c = new JsonObject();
        c.addProperty("base", Math.max(1, base));
        c.addProperty("per_level_above_first", Math.max(0, perLevel));
        return c;
    }

    private static JsonArray slots(EquipmentSlot[] slots) {
        JsonArray out = new JsonArray();
        List<EquipmentSlot> list = List.of(slots);
        if (list.containsAll(List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET))) {
            out.add("any");
            return out;
        }
        for (EquipmentSlot s : slots) {
            out.add(switch (s) {
                case MAINHAND -> "mainhand";
                case OFFHAND -> "offhand";
                case HEAD -> "head";
                case CHEST -> "chest";
                case LEGS -> "legs";
                case FEET -> "feet";
                default -> "any";
            });
        }
        if (out.isEmpty()) out.add("mainhand");
        return out;
    }

    // ------------------------------------------------------------------ conversions used by the migrated code

    /** A vanilla entry as the legacy type. */
    public static LegacyEnchantment legacy(net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder) {
        return holder.unwrapKey().map(k -> {
            LegacyEnchantment mine = REGISTRY.get(k.location());
            return mine != null ? mine : LegacyEnchantment.of(holder);
        }).orElseGet(() -> LegacyEnchantment.of(holder));
    }
}
