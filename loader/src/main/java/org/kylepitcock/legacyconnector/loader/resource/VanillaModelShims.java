package org.kylepitcock.legacyconnector.loader.resource;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vanilla block and item models of 1.12 that 1.13 and later no longer ship under that name (redstone_dot, melon_stem_growth3, oak_bark,
 * sandstone_outer_stairs, flower_pot_rose, ...), which 1.12 blockstates name as models and 1.12 model files name as parents. Each one is
 * written back as a small model file of the {@code minecraft} namespace (the way {@link LegacyResourceConverter} already generates the vine
 * models), either an alias of the model 1.21.1 renamed it to or a definition of the same geometry, so a reference by the old name resolves
 * wherever it is read (the game's model loader, {@code Assets1122}'s blockstate conversion). A jar gets only the files its own JSON names.
 */
public final class VanillaModelShims {
    private static final String M = "minecraft:";
    private static final Map<String, String> CUBE_ALL = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS = new LinkedHashMap<>();
    private static final Map<String, String> EXACT = new LinkedHashMap<>();
    private static final Pattern STAIRS = Pattern.compile("(.+)_(inner|outer)_stairs");
    private static final Pattern DOOR = Pattern.compile("(acacia|birch|dark_oak|jungle|spruce|oak|wooden)_door_(bottom|top)(_rh)?");
    private static final Pattern STEM = Pattern.compile("(melon|pumpkin)_stem_(growth([0-7])|fruit)");
    private static final Pattern POT = Pattern.compile("flower_pot_([a-z_]+)");
    private static final Pattern COCOA = Pattern.compile("cocoa_age([0-2])(_s)?");
    private static final Map<String, String> STAIR_MATERIALS = new LinkedHashMap<>();
    private static final Map<String, String> POTTED = new LinkedHashMap<>();
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple", "blue",
            "brown", "green", "red", "black"};
    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    static {
        for (String wood : WOODS) CUBE_ALL.put("block/" + wood + "_bark", wood + "_log");
        for (String c : COLORS) {
            String modern = c.equals("silver") ? "light_gray" : c;
            CUBE_ALL.put("block/glass_" + c, modern + "_stained_glass");
            CUBE_ALL.put("block/hardened_clay_stained_" + c, modern + "_terracotta");
        }
        CUBE_ALL.put("block/lit_redstone_ore", "redstone_ore");
        CUBE_ALL.put("block/lit_redstone_lamp", "redstone_lamp_on");
        CUBE_ALL.put("block/silver_concrete", "light_gray_concrete");
        CUBE_ALL.put("block/silver_concrete_powder", "light_gray_concrete_powder");
        CUBE_ALL.put("block/soulsand", "soul_sand");
        CUBE_ALL.put("block/granite_smooth", "polished_granite");
        CUBE_ALL.put("block/andesite_smooth", "polished_andesite");
        CUBE_ALL.put("block/diorite_smooth", "polished_diorite");
        CUBE_ALL.put("block/stonebrick_cracked", "cracked_stone_bricks");
        CUBE_ALL.put("block/stonebrick_mossy", "mossy_stone_bricks");
        CUBE_ALL.put("block/stonebrick_carved", "chiseled_stone_bricks");
        CUBE_ALL.put("block/end_bricks", "end_stone_bricks");
        CUBE_ALL.put("block/red_nether_brick", "red_nether_bricks");
        CUBE_ALL.put("block/prismarine_dark", "dark_prismarine");
        CUBE_ALL.put("block/snow", "snow");

        // models 1.13 renamed; a bare alias keeps the texture slots (the target models define their own defaults)
        ALIAS.put("block/redstone_dot", "redstone_dust_dot");
        ALIAS.put("block/redstone_side0", "redstone_dust_side0");
        ALIAS.put("block/redstone_side1", "redstone_dust_side1");
        ALIAS.put("block/redstone_side_alt0", "redstone_dust_side_alt0");
        ALIAS.put("block/redstone_side_alt1", "redstone_dust_side_alt1");
        ALIAS.put("block/redstone_up", "redstone_dust_up");
        ALIAS.put("block/tripwire_hook_powered", "tripwire_hook_on");
        ALIAS.put("block/tripwire_hook_attached_powered", "tripwire_hook_attached_on");
        ALIAS.put("block/waterlily", "lily_pad");
        ALIAS.put("block/tallgrass", "short_grass");
        ALIAS.put("block/web", "cobweb");
        ALIAS.put("block/grass_normal", "grass_block");
        ALIAS.put("block/grass_snowed", "grass_block_snow");
        ALIAS.put("block/flower_pot_fern", "potted_fern");

        EXACT.put("block/column_side", "{\"parent\":\"minecraft:block/block\",\"textures\":{\"particle\":\"#side\"},\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],"
                + "\"faces\":{\"north\":{\"texture\":\"#side\",\"cullface\":\"north\"},\"east\":{\"texture\":\"#side\",\"cullface\":\"east\"},"
                + "\"south\":{\"texture\":\"#side\",\"cullface\":\"south\"},\"west\":{\"texture\":\"#side\",\"cullface\":\"west\"}}}]}");
        EXACT.put("block/wooden_button_inventory", "{\"parent\":\"minecraft:block/button_inventory\",\"textures\":{\"texture\":\"minecraft:block/oak_planks\"}}");
        EXACT.put("block/stone_button_inventory", "{\"parent\":\"minecraft:block/button_inventory\",\"textures\":{\"texture\":\"minecraft:block/stone\"}}");
        EXACT.put("item/bottle_drinkable", bottle("potion"));
        EXACT.put("item/bottle_splash", bottle("splash_potion"));
        EXACT.put("item/bottle_lingering", bottle("lingering_potion"));

        STAIR_MATERIALS.put("sandstone", "sandstone_bottom|sandstone_top|sandstone");
        STAIR_MATERIALS.put("red_sandstone", "red_sandstone_bottom|red_sandstone_top|red_sandstone");
        STAIR_MATERIALS.put("brick", "bricks|bricks|bricks");
        STAIR_MATERIALS.put("stone_brick", "stone_bricks|stone_bricks|stone_bricks");
        STAIR_MATERIALS.put("nether_brick", "nether_bricks|nether_bricks|nether_bricks");
        STAIR_MATERIALS.put("quartz", "quartz_block_bottom|quartz_block_top|quartz_block_side");
        STAIR_MATERIALS.put("purpur", "purpur_block|purpur_block|purpur_block");
        STAIR_MATERIALS.put("cobblestone", "cobblestone|cobblestone|cobblestone");
        STAIR_MATERIALS.put("stone", "cobblestone|cobblestone|cobblestone");
        for (String wood : WOODS) STAIR_MATERIALS.put(wood, wood + "_planks|" + wood + "_planks|" + wood + "_planks");
        STAIR_MATERIALS.put("wood", "oak_planks|oak_planks|oak_planks");

        POTTED.put("rose", "poppy");
        POTTED.put("orchid", "blue_orchid");
        POTTED.put("allium", "allium");
        POTTED.put("houstonia", "azure_bluet");
        POTTED.put("tulip_red", "red_tulip");
        POTTED.put("tulip_orange", "orange_tulip");
        POTTED.put("tulip_white", "white_tulip");
        POTTED.put("tulip_pink", "pink_tulip");
        POTTED.put("daisy", "oxeye_daisy");
        POTTED.put("dandelion", "dandelion");
        for (String wood : WOODS) POTTED.put(wood, wood + "_sapling");
        POTTED.put("mushroom_red", "red_mushroom");
        POTTED.put("mushroom_brown", "brown_mushroom");
        POTTED.put("dead_bush", "dead_bush");
        POTTED.put("cactus", "cactus");
        POTTED.put("fern", "fern");
    }

    private VanillaModelShims() {}

    private static String bottle(String modernItem) {
        // 1.12: layer0 the tinted liquid overlay, layer1 the bottle; 1.21 swapped the two layers and the tint index
        return "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/potion_overlay\",\"layer1\":\"minecraft:item/" + modernItem + "\"}}";
    }

    private static String cubeAll(String texture) {
        return "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"minecraft:block/" + texture + "\"}}";
    }

    /**
     * The model file ({@code models/<path>.json}, so {@code block/redstone_dot}) of a 1.12 vanilla model that 1.21.1 lacks, as JSON bytes; null for
     * every other path (a name 1.21.1 still has must stay vanilla's).
     */
    public static byte[] json(String path) {
        String s = text(path);
        return s == null ? null : s.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(String path) {
        String cube = CUBE_ALL.get(path);
        if (cube != null) return cubeAll(cube);
        String alias = ALIAS.get(path);
        if (alias != null) return "{\"parent\":\"minecraft:block/" + alias + "\"}";
        String exact = EXACT.get(path);
        if (exact != null) return exact;
        if (!path.startsWith("block/")) return null;
        String name = path.substring("block/".length());
        String vine = LegacyResourceConverter.vineModel(name);
        if (vine != null) return vine;
        Matcher m = STAIRS.matcher(name);
        if (m.matches() && STAIR_MATERIALS.containsKey(m.group(1))) {
            String[] t = STAIR_MATERIALS.get(m.group(1)).split("\\|");
            return "{\"parent\":\"minecraft:block/" + m.group(2) + "_stairs\",\"textures\":{\"bottom\":\"minecraft:block/" + t[0] + "\",\"top\":\"minecraft:block/" + t[1]
                    + "\",\"side\":\"minecraft:block/" + t[2] + "\"}}";
        }
        m = DOOR.matcher(name);
        if (m.matches()) {
            String wood = m.group(1).equals("wooden") ? "oak" : m.group(1);
            String parent = "door_" + m.group(2) + (m.group(3) != null ? "_right" : "_left");
            return "{\"parent\":\"minecraft:block/" + parent + "\",\"textures\":{\"bottom\":\"minecraft:block/" + wood + "_door_bottom\",\"top\":\"minecraft:block/" + wood
                    + "_door_top\"}}";
        }
        m = STEM.matcher(name);
        if (m.matches()) {
            String crop = m.group(1);
            if (m.group(3) != null) {
                return "{\"parent\":\"minecraft:block/stem_growth" + m.group(3) + "\",\"textures\":{\"stem\":\"minecraft:block/" + crop + "_stem\"}}";
            }
            return "{\"parent\":\"minecraft:block/stem_fruit\",\"textures\":{\"stem\":\"minecraft:block/attached_" + crop + "_stem\",\"upperstem\":\"minecraft:block/" + crop
                    + "_stem\"}}";
        }
        m = COCOA.matcher(name);
        if (m.matches()) return "{\"parent\":\"minecraft:block/cocoa_stage" + m.group(1) + "\"}";
        m = POT.matcher(name);
        if (m.matches() && POTTED.containsKey(m.group(1)) && !name.equals("flower_pot_cross")) {
            return "{\"parent\":\"minecraft:block/potted_" + POTTED.get(m.group(1)) + "\"}";
        }
        return null;
    }
}
