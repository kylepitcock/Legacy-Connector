package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The 1.7.10 unlocalized names of vanilla blocks and items ({@code tile.oreIron}, {@code item.ingotIron}): they differ from the
 * registry names. Mods build ids and lang keys from them (Mystcraft's {@code ModMat_tile.stone} symbols).
 */
public final class VanillaNames {
    private static final Map<String, String> BLOCKS = new HashMap<>();
    private static final Map<String, String> ITEMS = new HashMap<>();
    /** Reverse tables: unlocalized name -> the 1.7.10 registry names that use it ("log" -> log, log2). */
    private static final Map<String, java.util.List<String>> BLOCKS_BY_NAME = new HashMap<>();
    private static final Map<String, java.util.List<String>> ITEMS_BY_NAME = new HashMap<>();
    /** Resolved modern keys of legacy vanilla lang keys ("" = none). */
    private static final Map<String, String> MODERN_KEYS = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        try (InputStream in = VanillaNames.class.getResourceAsStream("/legacyconnector/legacy/1.7.10/vanilla-unlocalized-names.txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.startsWith("#")) continue;
                        String[] p = line.trim().split(" ");
                        if (p.length != 3) continue;
                        boolean block = p[0].equals("b");
                        (block ? BLOCKS : ITEMS).put(p[1], p[2]);
                        (block ? BLOCKS_BY_NAME : ITEMS_BY_NAME).computeIfAbsent(p[2], k -> new java.util.ArrayList<>()).add(p[1]);
                    }
                }
            }
        } catch (java.io.IOException e) {
            // no table: registry names are used
        }
    }

    private VanillaNames() {}

    /** The name without its prefix for a vanilla {@code legacyName} ("minecraft:iron_ore" -> "oreIron"). */
    public static String unlocalized(boolean block, String legacyName) {
        if (legacyName.indexOf(':') >= 0 && !legacyName.startsWith("minecraft:")) return legacyName;
        String path = legacyName.startsWith("minecraft:") ? legacyName.substring("minecraft:".length()) : legacyName;
        return (block ? BLOCKS : ITEMS).getOrDefault(path, path);
    }

    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "big_oak"};
    private static final String[] COLORS = {"white", "orange", "magenta", "lightBlue", "yellow", "lime", "pink", "gray", "silver", "cyan",
            "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] DYES = {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime",
            "yellow", "lightBlue", "magenta", "orange", "white"};

    /**
     * 1.7.10's subtype name suffixes by metadata, keyed by registry name ({@code tile.wood.spruce.name} is planks:1,
     * {@code tile.cloth.red.name} wool:14, {@code item.dyePowder.blue.name} dye:4).
     */
    private static final Map<String, String[]> VARIANTS = Map.ofEntries(
            Map.entry("planks", WOODS), Map.entry("sapling", WOODS), Map.entry("wooden_slab", WOODS),
            Map.entry("log", new String[] {"oak", "spruce", "birch", "jungle"}), Map.entry("leaves", new String[] {"oak", "spruce", "birch", "jungle"}),
            Map.entry("log2", new String[] {"acacia", "big_oak"}), Map.entry("leaves2", new String[] {"acacia", "big_oak"}),
            Map.entry("wool", COLORS), Map.entry("stained_glass", COLORS), Map.entry("stained_glass_pane", COLORS),
            Map.entry("stained_hardened_clay", COLORS), Map.entry("carpet", COLORS), Map.entry("dye", DYES),
            Map.entry("dirt", new String[] {"default", "default", "podzol"}), Map.entry("sand", new String[] {"default", "red"}),
            Map.entry("sandstone", new String[] {"default", "chiseled", "smooth"}),
            Map.entry("stonebrick", new String[] {"default", "mossy", "cracked", "chiseled"}),
            Map.entry("quartz_block", new String[] {"default", "chiseled", "lines"}),
            Map.entry("red_flower", new String[] {"poppy", "blueOrchid", "allium", "houstonia", "tulipRed", "tulipOrange", "tulipWhite",
                    "tulipPink", "oxeyeDaisy"}),
            Map.entry("yellow_flower", new String[] {"dandelion"}),
            Map.entry("double_plant", new String[] {"sunflower", "syringa", "grass", "fern", "rose", "paeonia"}),
            Map.entry("tallgrass", new String[] {"shrub", "grass", "fern"}),
            Map.entry("stone_slab", new String[] {"stone", "sand", "wood", "cobble", "brick", "smoothStoneBrick", "netherBrick", "quartz"}),
            Map.entry("cobblestone_wall", new String[] {"normal", "mossy"}),
            Map.entry("monster_egg", new String[] {"stone", "cobble", "brick", "mossybrick", "crackedbrick", "chiseledbrick"}),
            Map.entry("anvil", new String[] {"intact", "slightlyDamaged", "veryDamaged"}),
            Map.entry("skull", new String[] {"skeleton", "wither", "zombie", "char", "creeper"}));

    /**
     * The modern translation key of a 1.7.10 vanilla lang key: {@code tile.stone.name} -> {@code block.minecraft.stone},
     * {@code item.ingotIron.name} -> {@code item.minecraft.iron_ingot}, {@code tile.cloth.red.name} -> {@code block.minecraft.red_wool}.
     * Mods show vanilla names through these keys (ChromatiCraft's hidden ores call {@code Blocks.stone.getLocalizedName()}, others
     * translate {@code tile.stone.name} directly), and 1.7.10 resolved them from its own lang. Null for other keys. The key goes
     * through the legacy wrapper of that vanilla block/item, so it names the modern object the bridge maps the legacy one to.
     * Cheap for other keys (one map miss); resolved keys are cached.
     */
    public static String modernKey(String key) {
        if (key == null || key.length() < 11 || !key.endsWith(".name")) return null;
        boolean tile = key.startsWith("tile.");
        if (!tile && !key.startsWith("item.")) return null;
        String name = key.substring(5, key.length() - 5);
        String variant = null;
        java.util.List<String> regs = (tile ? BLOCKS_BY_NAME : ITEMS_BY_NAME).get(name);
        if (regs == null) {
            int dot = name.indexOf('.');
            if (dot <= 0) return null;
            variant = name.substring(dot + 1);
            regs = (tile ? BLOCKS_BY_NAME : ITEMS_BY_NAME).get(name.substring(0, dot));
            if (regs == null) return null;
        }
        String cached = MODERN_KEYS.get(key);
        if (cached != null) return cached.isEmpty() ? null : cached;
        String found = null;
        boolean ready = true;
        for (String reg : regs) {
            int meta = 0;
            if (variant != null) {
                String[] names = VARIANTS.get(reg);
                meta = names == null ? -1 : java.util.Arrays.asList(names).indexOf(variant);
                if (meta < 0) continue;
            }
            try {
                legacyconnector.v1_7_10.net.minecraft.item.Item item = ItemBridge.byName("minecraft:" + reg);
                net.minecraft.world.item.ItemStack stack = item == null ? net.minecraft.world.item.ItemStack.EMPTY : ItemBridge.toModern(item, 1, meta);
                if (!stack.isEmpty() && stack.getItem() != net.minecraft.world.item.Items.BARRIER) {
                    found = stack.getItem().getDescriptionId();
                    break;
                }
                if (tile) {
                    legacyconnector.v1_7_10.net.minecraft.block.Block block = BlockBridge.byName("minecraft:" + reg);
                    if (block != null) {
                        net.minecraft.world.level.block.Block modern = BlockBridge.toModern(block, meta).getBlock();
                        if (modern != net.minecraft.world.level.block.Blocks.AIR) {
                            found = modern.getDescriptionId();
                            break;
                        }
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                ready = false; // the bridges are not up yet: try again later
            }
        }
        if (found != null || ready) MODERN_KEYS.put(key, found == null ? "" : found);
        return found;
    }
}
