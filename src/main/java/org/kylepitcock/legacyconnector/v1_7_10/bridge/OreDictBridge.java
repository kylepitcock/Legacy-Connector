package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Ore dictionary names <-> modern item tags. {@code ingotCopper} is {@code c:ingots/copper}, the NeoForge common
 * tag convention, so legacy recipes accept copper from vanilla and from modern mods, and legacy items become
 * usable in modern recipes that use the tag.
 */
public final class OreDictBridge {
    private static final Map<String, String> PREFIXES = new LinkedHashMap<>();
    private static final Map<String, String> SPECIAL = new LinkedHashMap<>();

    static {
        PREFIXES.put("ingot", "c:ingots/");
        PREFIXES.put("nugget", "c:nuggets/");
        PREFIXES.put("ore", "c:ores/");
        PREFIXES.put("dust", "c:dusts/");
        PREFIXES.put("gem", "c:gems/");
        PREFIXES.put("block", "c:storage_blocks/");
        PREFIXES.put("plate", "c:plates/");
        PREFIXES.put("gear", "c:gears/");
        PREFIXES.put("rod", "c:rods/");
        PREFIXES.put("dye", "c:dyes/");
        PREFIXES.put("crop", "c:crops/");
        PREFIXES.put("seed", "c:seeds/");
        PREFIXES.put("raw", "c:raw_materials/");
        SPECIAL.put("logWood", "minecraft:logs");
        SPECIAL.put("plankWood", "minecraft:planks");
        SPECIAL.put("slabWood", "minecraft:wooden_slabs");
        SPECIAL.put("stairWood", "minecraft:wooden_stairs");
        SPECIAL.put("stickWood", "c:rods/wooden");
        SPECIAL.put("treeSapling", "minecraft:saplings");
        SPECIAL.put("treeLeaves", "minecraft:leaves");
        SPECIAL.put("blockWool", "minecraft:wool");
        SPECIAL.put("stone", "c:stones");
        SPECIAL.put("cobblestone", "c:cobblestones");
        SPECIAL.put("sand", "c:sands");
        SPECIAL.put("sandstone", "c:sandstone/blocks");
        SPECIAL.put("blockGlass", "c:glass_blocks");
        SPECIAL.put("paneGlass", "c:glass_panes");
        SPECIAL.put("chestWood", "c:chests/wooden");
        SPECIAL.put("slimeball", "c:slime_balls");
        SPECIAL.put("enderpearl", "c:ender_pearls");
        SPECIAL.put("string", "c:strings");
        SPECIAL.put("gunpowder", "c:gunpowders");
        SPECIAL.put("leather", "c:leathers");
        SPECIAL.put("feather", "c:feathers");
        SPECIAL.put("bone", "c:bones");
        SPECIAL.put("egg", "c:eggs");
        SPECIAL.put("obsidian", "c:obsidians");
        SPECIAL.put("glowstone", "c:storage_blocks/glowstone");
        SPECIAL.put("record", "minecraft:music_discs");
        SPECIAL.put("craftingTableWood", "c:player_workstations/crafting_tables");
    }

    private OreDictBridge() {}

    /** Tag for an ore dictionary name; unknown conventions get a private {@code legacyconnector:oredict/...} tag. */
    public static ResourceLocation tagFor(String oreName) {
        String special = SPECIAL.get(oreName);
        if (special != null) return ResourceLocation.parse(special);
        for (var e : PREFIXES.entrySet()) {
            String p = e.getKey();
            if (oreName.length() > p.length() && oreName.startsWith(p) && Character.isUpperCase(oreName.charAt(p.length()))) {
                return ResourceLocation.parse(e.getValue() + snake(oreName.substring(p.length())));
            }
        }
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "oredict/" + snake(oreName));
    }

    static String snake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && camel.charAt(i - 1) != '_') sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString().replaceAll("[^a-z0-9_./-]", "_");
    }

    /** Reverse of {@link #tagFor} for tags that follow a known convention. */
    static String oreNameFor(ResourceLocation tag) {
        for (var e : SPECIAL.entrySet()) if (e.getValue().equals(tag.toString())) return e.getKey();
        String s = tag.toString();
        for (var e : PREFIXES.entrySet()) {
            if (s.startsWith(e.getValue()) && s.length() > e.getValue().length()) {
                String rest = s.substring(e.getValue().length());
                if (rest.contains("/")) return null;
                StringBuilder sb = new StringBuilder(e.getKey());
                for (String part : rest.split("_")) if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                return sb.toString();
            }
        }
        return null;
    }

    public static void onRegister(String name, ItemStack stack) {
    }

    /** Modern items carrying the tag for {@code oreName} (only once tags are bound, i.e. in a running world). */
    public static List<ItemStack> modernItemsFor(String oreName) {
        List<ItemStack> out = new ArrayList<>();
        TagKey<Item> key = TagKey.create(Registries.ITEM, tagFor(oreName));
        BuiltInRegistries.ITEM.getTag(key).ifPresent(set -> {
            for (var holder : set) {
                ItemStack s = ItemStack.mcc$wrap(new net.minecraft.world.item.ItemStack(holder));
                if (s != null) out.add(s);
            }
        });
        return out;
    }

    public static Set<String> modernNamesFor(ItemStack stack) {
        Set<String> out = new LinkedHashSet<>();
        stack.mcc$modern().getTags().forEach(t -> {
            String name = oreNameFor(t.location());
            if (name != null) out.add(name);
        });
        return out;
    }

    /** The default vanilla entries Forge 1.7.10 registered itself, so legacy lookups during init see vanilla items. */
    public static void registerVanillaDefaults() {
        int w = OreDictionary.WILDCARD_VALUE;
        reg("logWood", new ItemStack(Blocks.log, 1, w), new ItemStack(Blocks.log2, 1, w));
        reg("plankWood", new ItemStack(Blocks.planks, 1, w));
        reg("slabWood", new ItemStack(Blocks.wooden_slab, 1, w));
        reg("stairWood", new ItemStack(Blocks.oak_stairs), new ItemStack(Blocks.spruce_stairs), new ItemStack(Blocks.birch_stairs),
                new ItemStack(Blocks.jungle_stairs), new ItemStack(Blocks.acacia_stairs), new ItemStack(Blocks.dark_oak_stairs));
        reg("stickWood", new ItemStack(Items.stick));
        reg("treeSapling", new ItemStack(Blocks.sapling, 1, w));
        reg("treeLeaves", new ItemStack(Blocks.leaves, 1, w), new ItemStack(Blocks.leaves2, 1, w));
        reg("oreGold", new ItemStack(Blocks.gold_ore));
        reg("oreIron", new ItemStack(Blocks.iron_ore));
        reg("oreLapis", new ItemStack(Blocks.lapis_ore));
        reg("oreDiamond", new ItemStack(Blocks.diamond_ore));
        reg("oreRedstone", new ItemStack(Blocks.redstone_ore));
        reg("oreEmerald", new ItemStack(Blocks.emerald_ore));
        reg("oreQuartz", new ItemStack(Blocks.quartz_ore));
        reg("oreCoal", new ItemStack(Blocks.coal_ore));
        reg("ingotIron", new ItemStack(Items.iron_ingot));
        reg("ingotGold", new ItemStack(Items.gold_ingot));
        reg("gemDiamond", new ItemStack(Items.diamond));
        reg("gemEmerald", new ItemStack(Items.emerald));
        reg("gemQuartz", new ItemStack(Items.quartz));
        reg("gemLapis", new ItemStack(Items.dye, 1, 4));
        reg("dustRedstone", new ItemStack(Items.redstone));
        reg("dustGlowstone", new ItemStack(Items.glowstone_dust));
        reg("glowstone", new ItemStack(Blocks.glowstone));
        reg("cropWheat", new ItemStack(Items.wheat));
        reg("cropPotato", new ItemStack(Items.potato));
        reg("cropCarrot", new ItemStack(Items.carrot));
        reg("stone", new ItemStack(Blocks.stone));
        reg("cobblestone", new ItemStack(Blocks.cobblestone));
        reg("sandstone", new ItemStack(Blocks.sandstone, 1, w));
        reg("sand", new ItemStack(Blocks.sand, 1, w));
        reg("slimeball", new ItemStack(Items.slime_ball));
        reg("blockIron", new ItemStack(Blocks.iron_block));
        reg("blockGold", new ItemStack(Blocks.gold_block));
        reg("blockDiamond", new ItemStack(Blocks.diamond_block));
        reg("blockEmerald", new ItemStack(Blocks.emerald_block));
        reg("blockRedstone", new ItemStack(Blocks.redstone_block));
        reg("blockLapis", new ItemStack(Blocks.lapis_block));
        reg("blockGlass", new ItemStack(Blocks.glass));
        reg("paneGlass", new ItemStack(Blocks.glass_pane));
        reg("chestWood", new ItemStack(Blocks.chest));
        String[] dyes = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink", "Lime", "Yellow", "LightBlue",
                "Magenta", "Orange", "White"};
        for (int i = 0; i < 16; i++) reg("dye" + dyes[i], new ItemStack(Items.dye, 1, i));
    }

    private static void reg(String name, ItemStack... stacks) {
        for (ItemStack s : stacks) OreDictionary.registerOre(name, s);
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
