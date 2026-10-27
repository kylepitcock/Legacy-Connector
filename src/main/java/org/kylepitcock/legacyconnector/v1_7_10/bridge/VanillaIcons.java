package org.kylepitcock.legacyconnector.v1_7_10.bridge;

/** 1.7.10 icon names of vanilla blocks per side and metadata, for mods that ask vanilla blocks for their icons. */
public final class VanillaIcons {
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] LOGS = {"oak", "spruce", "birch", "jungle"};
    private static final String[] LOGS2 = {"acacia", "big_oak"};
    private static final String[] PLANKS = {"oak", "spruce", "birch", "jungle", "acacia", "big_oak"};
    private static final String[] STONEBRICK = {"stonebrick", "stonebrick_mossy", "stonebrick_cracked", "stonebrick_carved"};

    private VanillaIcons() {}

    /** Name without namespace ("log_oak_top"); the block's own legacy name where nothing special is known. */
    public static String name(String legacyName, int side, int meta) {
        String name = rule(legacyName, side, meta);
        return name != null || legacyName == null ? name : bare(legacyName);
    }

    /**
     * The icon of a vanilla block: the rules below, else (in the client, once the models are loaded) the sprite of the modern
     * model its metadata maps to (saplings, flowers, leaves, slabs, carpets...), else the block's own legacy name.
     */
    public static String name(legacyconnector.v1_7_10.net.minecraft.block.Block block, String legacyName, int side, int meta) {
        String name = rule(legacyName, side, meta);
        if (name != null) return name;
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            name = org.kylepitcock.legacyconnector.v1_7_10.client.VanillaModelIcons.name(BlockBridge.toModern(block, meta), side);
            if (name != null) return name;
        }
        return legacyName == null ? null : bare(legacyName);
    }

    private static String bare(String legacyName) {
        return legacyName.startsWith("minecraft:") ? legacyName.substring("minecraft:".length()) : legacyName;
    }

    /** The hand-written rule for the block, null where there is none. */
    private static String rule(String legacyName, int side, int meta) {
        if (legacyName == null) return null;
        legacyName = bare(legacyName);
        switch (legacyName) {
            case "log", "log2" -> {
                String wood = legacyName.equals("log") ? LOGS[meta & 3] : LOGS2[Math.min(meta & 1, 1)];
                boolean barkOnly = (meta & 12) == 12;
                boolean end = !barkOnly && ((meta & 12) == 0 && side <= 1 || (meta & 12) == 4 && side >= 4 || (meta & 12) == 8 && (side == 2 || side == 3));
                return "log_" + wood + (end ? "_top" : "");
            }
            case "planks" -> {
                return "planks_" + PLANKS[Math.min(meta, PLANKS.length - 1)];
            }
            case "grass" -> {
                return side == 1 ? "grass_top" : side == 0 ? "dirt" : "grass_side";
            }
            // BlockLiquid: still on top and bottom, flowing on the sides (Thaumcraft's crucible draws its water with it)
            case "water", "flowing_water" -> {
                return side <= 1 ? "water_still" : "water_flow";
            }
            case "lava", "flowing_lava" -> {
                return side <= 1 ? "lava_still" : "lava_flow";
            }
            case "brick_block" -> {
                return "brick";
            }
            case "bookshelf" -> {
                return side <= 1 ? "planks_oak" : "bookshelf";
            }
            case "crafting_table" -> {
                return side == 1 ? "crafting_table_top" : side == 0 ? "planks_oak" : side == 2 || side == 3 ? "crafting_table_front" : "crafting_table_side";
            }
            case "wool" -> {
                return "wool_colored_" + COLORS[meta & 15];
            }
            case "stained_hardened_clay" -> {
                return "hardened_clay_stained_" + COLORS[meta & 15];
            }
            case "stonebrick" -> {
                return STONEBRICK[meta & 3];
            }
            case "sandstone" -> {
                return side == 1 ? "sandstone_top" : side == 0 ? "sandstone_bottom" : meta == 1 ? "sandstone_carved" : meta == 2 ? "sandstone_smooth" : "sandstone_normal";
            }
            case "furnace", "lit_furnace" -> {
                return side <= 1 ? "furnace_top" : side == 3 ? "furnace_front_off" : "furnace_side";
            }
            case "tnt" -> {
                return side == 1 ? "tnt_top" : side == 0 ? "tnt_bottom" : "tnt_side";
            }
            case "pumpkin", "lit_pumpkin" -> {
                return side <= 1 ? "pumpkin_top" : "pumpkin_side";
            }
            case "melon_block" -> {
                return side <= 1 ? "melon_top" : "melon_side";
            }
            case "quartz_block" -> {
                return side == 1 ? "quartz_block_top" : side == 0 ? "quartz_block_bottom" : "quartz_block_side";
            }
            case "glass", "stone", "cobblestone", "dirt", "sand", "gravel", "iron_block", "gold_block", "diamond_block", "emerald_block",
                    "lapis_block", "obsidian", "netherrack", "soul_sand", "clay", "coal_block", "redstone_block", "hardened_clay",
                    "glowstone", "ice", "snow", "bedrock" -> {
                return legacyName.equals("snow") ? "snow" : legacyName;
            }
            default -> {
                return null;
            }
        }
    }
}
