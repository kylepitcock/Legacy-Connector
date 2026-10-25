package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12.2 vanilla blocks and items as legacy objects over the modern ones. A field name maps to a modern registry name (mostly its
 * lower case; the 1.13 flattening renamed the rest, and variants selected by metadata become their first variant).
 */
public final class Vanilla1122 {
    private static final Map<String, String> BLOCK_NAMES = new HashMap<>();
    private static final Map<String, String> ITEM_NAMES = new HashMap<>();
    private static final Map<net.minecraft.world.item.Item, Item> ITEM_REVERSE = new IdentityHashMap<>();

    static {
        String[] blocks = {"GRASS", "grass_block", "LOG", "oak_log", "LOG2", "acacia_log", "PLANKS", "oak_planks", "LEAVES", "oak_leaves",
                "LEAVES2", "acacia_leaves", "SAPLING", "oak_sapling", "WOOL", "white_wool", "STAINED_GLASS", "white_stained_glass",
                "STAINED_GLASS_PANE", "white_stained_glass_pane", "STAINED_HARDENED_CLAY", "white_terracotta", "HARDENED_CLAY", "terracotta",
                "STONEBRICK", "stone_bricks", "BRICK_BLOCK", "bricks", "MOB_SPAWNER", "spawner", "WEB", "cobweb", "TALLGRASS", "short_grass",
                "DEADBUSH", "dead_bush", "YELLOW_FLOWER", "dandelion", "RED_FLOWER", "poppy", "LIT_FURNACE", "furnace", "REEDS", "sugar_cane",
                "FENCE", "oak_fence", "FENCE_GATE", "oak_fence_gate", "WOODEN_DOOR", "oak_door", "WOODEN_SLAB", "oak_slab", "DOUBLE_WOODEN_SLAB",
                "oak_slab", "STONE_SLAB", "smooth_stone_slab", "DOUBLE_STONE_SLAB", "smooth_stone_slab", "WOODEN_BUTTON", "oak_button",
                "WOODEN_PRESSURE_PLATE", "oak_pressure_plate", "FLOWING_WATER", "water", "FLOWING_LAVA", "lava", "SNOW_LAYER", "snow", "SNOW",
                "snow_block", "QUARTZ_ORE", "nether_quartz_ore", "LIT_REDSTONE_LAMP", "redstone_lamp", "LIT_REDSTONE_ORE", "redstone_ore",
                "UNLIT_REDSTONE_TORCH", "redstone_torch", "POWERED_REPEATER", "repeater", "UNPOWERED_REPEATER", "repeater", "POWERED_COMPARATOR",
                "comparator", "UNPOWERED_COMPARATOR", "comparator", "DAYLIGHT_DETECTOR_INVERTED", "daylight_detector", "STANDING_SIGN", "oak_sign",
                "WALL_SIGN", "oak_wall_sign", "FROSTED_ICE", "frosted_ice", "PISTON_EXTENSION", "moving_piston", "WATERLILY", "lily_pad",
                "MELON_BLOCK", "melon", "GOLDEN_RAIL", "powered_rail", "NOTEBLOCK", "note_block", "TRAPDOOR", "oak_trapdoor", "END_BRICKS",
                "end_stone_bricks", "GRASS_PATH", "dirt_path", "CONCRETE", "white_concrete", "CONCRETE_POWDER", "white_concrete_powder",
                "CARPET", "white_carpet", "RED_SANDSTONE_STAIRS", "red_sandstone_stairs", "OAK_STAIRS", "oak_stairs", "BREWING_STAND",
                "brewing_stand", "COBBLESTONE_WALL", "cobblestone_wall", "SKULL", "skeleton_skull", "BED", "red_bed", "FLOWER_POT", "flower_pot",
                "PUMPKIN", "carved_pumpkin", "LIT_PUMPKIN", "jack_o_lantern", "MONSTER_EGG", "infested_stone", "SILVER_SHULKER_BOX", "light_gray_shulker_box",
                "PURPLE_SHULKER_BOX", "purple_shulker_box"};
        for (int i = 0; i < blocks.length; i += 2) BLOCK_NAMES.put(blocks[i], blocks[i + 1]);
        String[] items = {"REEDS", "sugar_cane", "NETHERBRICK", "nether_brick", "SPECKLED_MELON", "glistering_melon_slice", "MELON", "melon_slice",
                "SKULL", "skeleton_skull", "FIREWORK_CHARGE", "firework_star", "FIREWORKS", "firework_rocket", "COOKED_FISH", "cooked_cod",
                "FISH", "cod", "EMPTY_MAP", "map", "EYE_OF_ENDER", "ender_eye", "POTIONITEM", "potion", "SIGN", "oak_sign", "BED", "red_bed",
                "BOAT", "oak_boat", "OAK_DOOR", "oak_door", "WOODEN_DOOR", "oak_door", "DYE", "bone_meal", "COMMAND_BLOCK_MINECART", "command_block_minecart",
                "CHORUS_FRUIT_POPPED", "popped_chorus_fruit", "BANNER", "white_banner", "BOOK", "book", "WRITABLE_BOOK", "writable_book",
                "RECORD_13", "music_disc_13", "RECORD_CAT", "music_disc_cat", "RECORD_BLOCKS", "music_disc_blocks", "RECORD_CHIRP", "music_disc_chirp",
                "RECORD_FAR", "music_disc_far", "RECORD_MALL", "music_disc_mall", "RECORD_MELLOHI", "music_disc_mellohi", "RECORD_STAL", "music_disc_stal",
                "RECORD_STRAD", "music_disc_strad", "RECORD_WARD", "music_disc_ward", "RECORD_11", "music_disc_11", "RECORD_WAIT", "music_disc_wait",
                "SPAWN_EGG", "pig_spawn_egg", "MUTTON", "mutton", "BEEF", "beef", "CHICKEN", "chicken", "PORKCHOP", "porkchop", "REDSTONE", "redstone",
                "COMPARATOR", "comparator", "REPEATER", "repeater", "TOTEM_OF_UNDYING", "totem_of_undying", "ENCHANTED_BOOK", "enchanted_book",
                "MILK_BUCKET", "milk_bucket", "SKELETON_SKULL", "skeleton_skull"};
        for (int i = 0; i < items.length; i += 2) ITEM_NAMES.put(items[i], items[i + 1]);
    }

    private Vanilla1122() {}

    private static ResourceLocation find(net.minecraft.core.Registry<?> registry, String path) {
        ResourceLocation id = ResourceLocation.withDefaultNamespace(path);
        if (registry.containsKey(id)) return id;
        for (String prefix : new String[] {"oak_", "white_"}) {
            id = ResourceLocation.withDefaultNamespace(prefix + path);
            if (registry.containsKey(id)) return id;
        }
        return null;
    }

    public static Block block(String field) {
        String legacy = field.toLowerCase(java.util.Locale.ROOT);
        VanillaBlock b = new VanillaBlock(field.equals("AIR") ? Material.AIR : Material.ROCK);
        b.setRegistryName("minecraft", legacy);
        ResourceLocation id = find(BuiltInRegistries.BLOCK, BLOCK_NAMES.getOrDefault(field, legacy));
        if (id != null) b.mcc$modern = BuiltInRegistries.BLOCK.get(id);
        Registries1122.BLOCKS.registerVanilla(b);
        return b;
    }

    public static synchronized Item item(String field) {
        String legacy = field.toLowerCase(java.util.Locale.ROOT);
        VanillaItem i = new VanillaItem();
        i.setRegistryName("minecraft", legacy);
        ResourceLocation id = find(BuiltInRegistries.ITEM, ITEM_NAMES.getOrDefault(field, legacy));
        if (id != null) {
            var modern = BuiltInRegistries.ITEM.get(id);
            i.mcc$modern = modern;
            ITEM_REVERSE.putIfAbsent(modern, i);
            i.mcc$vanillaModern = modern;
        }
        Registries1122.ITEMS.registerVanilla(i);
        return i;
    }

    /** The legacy wrapper of a modern vanilla item, or null. */
    public static synchronized Item legacyOf(net.minecraft.world.item.Item modern) {
        return ITEM_REVERSE.get(modern);
    }

    /** A vanilla block as a legacy object. */
    public static final class VanillaBlock extends Block {
        VanillaBlock(Material material) {
            super(material);
        }
    }

    /** A vanilla item as a legacy object. */
    public static final class VanillaItem extends Item {
    }
}
