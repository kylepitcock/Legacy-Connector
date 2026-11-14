package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The mining tags of 1.12.2 blocks, from the harvest tool and level the block declares ({@code setHarvestLevel}, or an override of
 * {@code getHarvestTool}/{@code getHarvestLevel}), so modern tools mine them at the right speed and only drop them at the level Forge 1.12 demanded.
 * A block without a declared tool whose material needs one (rock, iron) is a pickaxe block of level 0, as in 1.12.
 */
public final class ToolTags1122 {
    private static final String[] TIERS = {null, "minecraft:needs_stone_tool", "minecraft:needs_iron_tool", "minecraft:needs_diamond_tool"};

    private ToolTags1122() {}

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (Block b : BlockBridge1122.modBlocks()) {
            var modern = BlockBridge1122.modern(b);
            if (modern == null) continue;
            try {
                tagBlock(b, BuiltInRegistries.BLOCK.getKey(modern).toString(), tags);
            } catch (RuntimeException | LinkageError e) {
                // a harvest rule that needs a world: the block has no tool tags
            }
        }
        contributeItems(out);
        for (var e : tags.entrySet()) {
            String[] rl = e.getKey().split(":");
            JsonObject json = new JsonObject();
            json.addProperty("replace", false);
            JsonArray values = new JsonArray();
            e.getValue().forEach(values::add);
            json.add("values", values);
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/" + rl[0] + "/tags/block/" + rl[1] + ".json", () -> bytes);
        }
    }

    private static void tagBlock(Block b, String id, Map<String, Set<String>> tags) {
        String tool = null;
        int level = Integer.MAX_VALUE;
        // the harvest rule can depend on the state (a mod's getHarvestTool override): the tool of the first state that has one, the lowest level of that tool
        for (int meta = 0; meta < 16; meta++) {
            IBlockState state;
            try {
                state = b.getStateFromMeta(meta);
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (state == null) continue;
            String t = b.getHarvestTool(state);
            if (t == null) continue;
            if (tool == null) tool = t;
            if (tool.equals(t)) level = Math.min(level, Math.max(0, b.getHarvestLevel(state)));
        }
        if (tool == null) {
            // the modern hoe is also the leaf tool (vanilla's leaves are in mineable/hoe), so leaf blocks of mods are too
            if (b.getMaterialLegacy() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.LEAVES) {
                tags.computeIfAbsent("minecraft:mineable/hoe", k -> new LinkedHashSet<>()).add(id);
                return;
            }
            if (b.getMaterialLegacy() == null || b.getMaterialLegacy().isToolNotRequired()) return;
            tool = "pickaxe";
            level = 0;
        }
        String mineable = switch (tool) {
            case "pickaxe" -> "minecraft:mineable/pickaxe";
            case "axe" -> "minecraft:mineable/axe";
            case "shovel" -> "minecraft:mineable/shovel";
            case "hoe" -> "minecraft:mineable/hoe";
            default -> null;
        };
        if (mineable != null) tags.computeIfAbsent(mineable, k -> new LinkedHashSet<>()).add(id);
        if (mineable != null && level >= 1) tags.computeIfAbsent(TIERS[Math.min(level, 3)], k -> new LinkedHashSet<>()).add(id);
    }

    /**
     * The item tags the modern enchantments are bound to (vanilla's enchantments name {@code #minecraft:enchantable/sword} and the like, not item classes):
     * legacy swords, tools and armor join them, so Sharpness, Efficiency, Unbreaking, Protection... apply to them as they did in 1.12.
     */
    private static void contributeItems(Map<String, Supplier<byte[]>> out) {
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (var item : ItemBridge1122.modItems()) {
            var variants = ItemBridge1122.variants(item);
            if (variants == null || variants.isEmpty()) continue;
            String[] groups;
            if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemSword) {
                groups = new String[] {"minecraft:swords", "minecraft:enchantable/sword", "minecraft:enchantable/weapon", "minecraft:enchantable/sharp_weapon",
                        "minecraft:enchantable/fire_aspect", "minecraft:enchantable/durability"};
            } else if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemAxe) {
                groups = new String[] {"minecraft:axes", "minecraft:enchantable/weapon", "minecraft:enchantable/sharp_weapon", "minecraft:enchantable/mining",
                        "minecraft:enchantable/mining_loot", "minecraft:enchantable/durability"};
            } else if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemPickaxe) {
                groups = new String[] {"minecraft:pickaxes", "minecraft:enchantable/mining", "minecraft:enchantable/mining_loot", "minecraft:enchantable/durability"};
            } else if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemSpade) {
                groups = new String[] {"minecraft:shovels", "minecraft:enchantable/mining", "minecraft:enchantable/mining_loot", "minecraft:enchantable/durability"};
            } else if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemHoe) {
                groups = new String[] {"minecraft:hoes", "minecraft:enchantable/mining", "minecraft:enchantable/mining_loot", "minecraft:enchantable/durability"};
            } else if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemArmor armor && armor.getEquipmentSlot() != null) {
                String slot = switch (armor.getEquipmentSlot()) {
                    case HEAD -> "head";
                    case CHEST -> "chest";
                    case LEGS -> "leg";
                    case FEET -> "foot";
                    default -> null;
                };
                if (slot == null) continue;
                groups = new String[] {"minecraft:" + slot + "_armor", "minecraft:enchantable/" + slot + "_armor", "minecraft:enchantable/armor",
                        "minecraft:enchantable/equippable", "minecraft:enchantable/durability"};
            } else {
                continue;
            }
            for (var modern : variants.values()) {
                String id = BuiltInRegistries.ITEM.getKey(modern).toString();
                for (String g : groups) tags.computeIfAbsent(g, k -> new LinkedHashSet<>()).add(id);
            }
        }
        for (var e : tags.entrySet()) {
            String[] rl = e.getKey().split(":");
            JsonObject json = new JsonObject();
            json.addProperty("replace", false);
            JsonArray values = new JsonArray();
            e.getValue().forEach(values::add);
            json.add("values", values);
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/" + rl[0] + "/tags/item/" + rl[1] + ".json", () -> bytes);
        }
    }
}
