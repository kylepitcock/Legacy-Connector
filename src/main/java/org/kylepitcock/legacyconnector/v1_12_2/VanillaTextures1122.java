package org.kylepitcock.legacyconnector.v1_12_2;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vanilla textures named the 1.12 way ("blocks/dirt", "minecraft:blocks/slime") as 1.21.1 names them. The loader's resource converter does
 * this for the model files of a mod's jar (LegacyResourceConverter.TEXTURE_RENAMES, which this mirrors); blockstate texture overrides turned into
 * generated models at runtime (Tinkers' Construct's slime dirt: "particle": "blocks/dirt") need the same rename.
 */
final class VanillaTextures1122 {
    private VanillaTextures1122() {}

    /** Vanilla block and item textures: the folders became block/ and item/, and many files were renamed in 1.13. */
    private static final Map<String, String> RENAMES = new LinkedHashMap<>();

    static {
        String[] woods = {"oak", "spruce", "birch", "jungle", "acacia", "big_oak:dark_oak"};
        for (String w : woods) {
            String from = w.contains(":") ? w.substring(0, w.indexOf(':')) : w;
            String to = w.contains(":") ? w.substring(w.indexOf(':') + 1) : w;
            RENAMES.put("block/planks_" + from, "block/" + to + "_planks");
            RENAMES.put("block/log_" + from, "block/" + to + "_log");
            RENAMES.put("block/log_" + from + "_top", "block/" + to + "_log_top");
            RENAMES.put("block/leaves_" + from, "block/" + to + "_leaves");
            RENAMES.put("block/sapling_" + from, "block/" + to + "_sapling");
            RENAMES.put("block/door_" + from + "_lower", "block/" + to + "_door_bottom");
            RENAMES.put("block/door_" + from + "_upper", "block/" + to + "_door_top");
        }
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver:light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        for (String c : colors) {
            String from = c.contains(":") ? c.substring(0, c.indexOf(':')) : c;
            String to = c.contains(":") ? c.substring(c.indexOf(':') + 1) : c;
            RENAMES.put("block/wool_colored_" + from, "block/" + to + "_wool");
            RENAMES.put("block/glass_" + from, "block/" + to + "_stained_glass");
            RENAMES.put("block/hardened_clay_stained_" + from, "block/" + to + "_terracotta");
            RENAMES.put("block/concrete_" + from, "block/" + to + "_concrete");
        }
        String[][] pairs = {
                {"grass_top", "grass_block_top"}, {"grass_side", "grass_block_side"}, {"grass_side_overlay", "grass_block_side_overlay"},
                {"stonebrick", "stone_bricks"}, {"brick", "bricks"}, {"cobblestone_mossy", "mossy_cobblestone"}, {"hardened_clay", "terracotta"},
                {"mob_spawner", "spawner"}, {"reeds", "sugar_cane"}, {"waterlily", "lily_pad"}, {"web", "cobweb"},
                {"flower_dandelion", "dandelion"}, {"flower_rose", "poppy"}, {"tallgrass", "grass"}, {"deadbush", "dead_bush"},
                {"stone_andesite", "andesite"}, {"stone_diorite", "diorite"}, {"stone_granite", "granite"},
                {"stone_andesite_smooth", "polished_andesite"}, {"stone_diorite_smooth", "polished_diorite"}, {"stone_granite_smooth", "polished_granite"},
                {"stonebrick_mossy", "mossy_stone_bricks"}, {"stonebrick_cracked", "cracked_stone_bricks"}, {"stonebrick_carved", "chiseled_stone_bricks"},
                {"sandstone_normal", "sandstone"}, {"sandstone_top", "sandstone_top"}, {"redstone_lamp_off", "redstone_lamp"}, {"redstone_lamp_on", "redstone_lamp_on"},
                {"quartz_block_side", "quartz_block_side"}, {"netherrack", "netherrack"}, {"nether_brick", "nether_bricks"}, {"red_nether_brick", "red_nether_bricks"},
                {"end_bricks", "end_stone_bricks"}, {"glass_pane_top", "glass_pane_top"}, {"piston_top_normal", "piston_top"}, {"piston_top_sticky", "piston_top_sticky"},
                {"furnace_front_off", "furnace_front"}, {"furnace_front_on", "furnace_front_on"}, {"melon_side", "melon_side"}, {"pumpkin_face_off", "carved_pumpkin"},
                {"pumpkin_face_on", "jack_o_lantern"}, {"noteblock", "note_block"}, {"portal", "nether_portal"}, {"wheat_stage_7", "wheat_stage7"},
                {"command_block_front", "command_block_front"}, {"gold_ore", "gold_ore"}, {"lapis_ore", "lapis_ore"}, {"cauldron_inner", "cauldron_inner"},
                {"snow", "snow"}, {"ice_packed", "packed_ice"}, {"slime", "slime_block"}, {"stone_slab_side", "smooth_stone_slab_side"},
                {"stone_slab_top", "smooth_stone"}, {"farmland_wet", "farmland_moist"}, {"farmland_dry", "farmland"}, {"grass_path_top", "dirt_path_top"},
                {"grass_path_side", "dirt_path_side"}, {"quartz_block_lines", "quartz_pillar"}, {"quartz_block_chiseled", "chiseled_quartz_block"},
                {"red_sandstone_normal", "red_sandstone"}, {"dirt_podzol_top", "podzol_top"}, {"dirt_podzol_side", "podzol_side"},
                {"coarse_dirt", "coarse_dirt"}, {"red_sand", "red_sand"}, {"sponge_wet", "wet_sponge"}, {"rail_golden", "powered_rail"},
                {"rail_golden_powered", "powered_rail_on"}, {"rail_detector", "detector_rail"}, {"rail_activator", "activator_rail"},
                {"rail_normal", "rail"}, {"rail_normal_turned", "rail_corner"}, {"torch_on", "torch"}, {"redstone_torch_on", "redstone_torch"},
                {"redstone_torch_off", "redstone_torch_off"}, {"ladder", "ladder"}, {"cactus_side", "cactus_side"}, {"clay", "clay"}};
        for (String[] p : pairs) if (!p[0].equals(p[1])) RENAMES.put("block/" + p[0], "block/" + p[1]);
    }


    /** The 1.21.1 path of a vanilla texture path ({@code blocks/dirt} gives {@code block/dirt}). */
    static String modern(String path) {
        if (path.startsWith("blocks/")) path = "block/" + path.substring("blocks/".length());
        else if (path.startsWith("items/")) path = "item/" + path.substring("items/".length());
        String renamed = RENAMES.get(path);
        return renamed != null ? renamed : path;
    }
}
