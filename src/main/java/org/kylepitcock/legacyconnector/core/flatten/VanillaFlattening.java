package org.kylepitcock.legacyconnector.core.flatten;

import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates pre-1.13 vanilla identities (numeric id + metadata) to modern blocks and items and back.
 *
 * <p>Rather than shipping our own tables, this reuses the game's own world-upgrade machinery: the 1.13
 * "flattening" table ({@link BlockStateData}) plus every later {@code DataFixer} rename, exactly what Minecraft
 * runs when an old world is opened. So {@code wool:3} becomes {@code light_blue_wool}, {@code grass} becomes
 * {@code grass_block}, {@code dye:4} becomes {@code lapis_lazuli}, and so on, for every vanilla id.</p>
 */
public final class VanillaFlattening {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Flattening");
    /** Data version of post-flattening block states (BlockStateFlatteningFix lives in schema 1450). */
    private static final int FLATTENED_BLOCK_VERSION = 1450;
    /** 1.12.2 data version; item stacks in legacy format are upgraded from here. */
    private static final int LEGACY_ITEM_VERSION = 1343;

    private static BlockState[] states;
    private static Map<BlockState, Integer> reverseStates;
    private static Map<Block, Integer> reverseBlocks;
    private static Map<String, Integer> blockIds;
    private static String[] blockNames;
    private static Map<BlockState, Integer> nearest;
    private static final Map<Integer, Item> ITEMS = new HashMap<>();
    private static Map<String, Integer> itemIds;

    private VanillaFlattening() {}

    private static int dataVersion() {
        return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    private static synchronized void initBlocks() {
        if (states != null) return;
        states = new BlockState[4096];
        reverseStates = new HashMap<>();
        reverseBlocks = new HashMap<>();
        for (int key = 0; key < 4096; key++) {
            BlockState s = upgrade(BlockStateData.getTag(key));
            states[key] = s;
            if (s == null) continue;
            reverseStates.putIfAbsent(s, key);
            reverseBlocks.putIfAbsent(s.getBlock(), key);
        }
        blockIds = new HashMap<>();
        blockNames = new String[256];
        for (Object2IntMap.Entry<String> e : BlockStateData.ID_BY_OLD_NAME.object2IntEntrySet()) {
            blockIds.put(e.getKey(), e.getIntValue() >> 4);
            int id = e.getIntValue() >> 4;
            if (id >= 0 && id < 256 && (blockNames[id] == null || e.getKey().compareTo(blockNames[id]) < 0)) blockNames[id] = e.getKey();
        }
        nearest = new java.util.concurrent.ConcurrentHashMap<>();
        LOG.debug("Built legacy block flattening table: {} states", reverseStates.size());
    }

    private static BlockState upgrade(Dynamic<?> flattened) {
        try {
            @SuppressWarnings("unchecked")
            Dynamic<Tag> input = (Dynamic<Tag>) (Dynamic<?>) new Dynamic<>(NbtOps.INSTANCE, flattened.convert(NbtOps.INSTANCE).getValue());
            Dynamic<Tag> out = DataFixers.getDataFixer().update(References.BLOCK_STATE, input, FLATTENED_BLOCK_VERSION, dataVersion());
            if (!(out.getValue() instanceof CompoundTag tag)) return null;
            return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Modern state for a 1.7.10/1.12.2 vanilla block id and metadata. */
    public static BlockState state(int legacyId, int meta) {
        initBlocks();
        if (legacyId < 0 || legacyId > 255) return Blocks.AIR.defaultBlockState();
        BlockState s = states[(legacyId << 4) | (meta & 15)];
        // a metadata the block never used reads as its first valid one (the skull's meta 0, a facing it never had)
        for (int m = 0; legacyId != 0 && (s == null || s.isAir()) && m < 16; m++) s = states[legacyId << 4 | m];
        return s == null ? Blocks.AIR.defaultBlockState() : s;
    }

    /** {@code id << 4 | meta} for a modern state, or -1 if the state has no legacy equivalent. */
    public static int legacyKey(BlockState state) {
        initBlocks();
        Integer exact = reverseStates.get(state);
        if (exact != null) return exact;
        Integer byBlock = reverseBlocks.get(state.getBlock());
        return byBlock == null ? -1 : byBlock;
    }

    /**
     * {@code id << 4 | meta} for a modern state: the exact legacy state, else the legacy state of the same block that shares the most
     * property values (a waterlogged or powered state 1.12 never had), else -1.
     */
    public static int nearestLegacyKey(BlockState state) {
        initBlocks();
        Integer exact = reverseStates.get(state);
        if (exact != null) return exact;
        Integer known = nearest.get(state);
        if (known != null) return known;
        int best = -1;
        int bestScore = -1;
        for (int key = 0; key < states.length; key++) {
            BlockState s = states[key];
            if (s == null || s.getBlock() != state.getBlock() || reverseStates.get(s) != key) continue;
            int score = 0;
            for (var p : state.getProperties()) if (s.getValue(p).equals(state.getValue(p))) score++;
            if (score > bestScore) {
                bestScore = score;
                best = key;
            }
        }
        nearest.put(state, best);
        return best;
    }

    /** The 1.12 registry name ("minecraft:stone") of a legacy numeric block id, or null. */
    public static String legacyName(int legacyId) {
        initBlocks();
        return legacyId < 0 || legacyId >= blockNames.length ? null : blockNames[legacyId];
    }

    /** Numeric id of a legacy block name ("stone" or "minecraft:stone"), or -1. */
    public static int blockId(String legacyName) {
        initBlocks();
        Integer id = blockIds.get(legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName);
        return id == null ? -1 : id;
    }

    // ------------------------------------------------------------------ items

    private static synchronized void initItemIds() {
        if (itemIds != null) return;
        itemIds = new HashMap<>();
        for (int id = 0; id < 32000; id++) {
            String name = ItemIdFix.getItem(id);
            if (name != null && !name.equals("minecraft:air")) itemIds.putIfAbsent(name, id);
        }
        initBlocks();
        for (var e : blockIds.entrySet()) itemIds.putIfAbsent(e.getKey(), e.getValue());
    }

    /** Qualifies a 1.7.10 item name and applies the one rename the data fixer's 1.8 era names do not cover. */
    private static String modernised(String legacyName) {
        String name = legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName;
        return name.equals("minecraft:cooked_fished") ? "minecraft:cooked_fish" : name;
    }

    public static int itemId(String legacyName) {
        initItemIds();
        Integer id = itemIds.get(modernised(legacyName));
        // ItemIdFix's own table keeps the 1.7.10 spelling ("cooked_fished" is 350): without an id the cooked fish item had no
        // reverse mapping, so modern cooked cod became a stranger to legacy code (no Thaumcraft aspects).
        if (id == null) id = itemIds.get(legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName);
        return id == null ? -1 : id;
    }

    /**
     * The data fixer's answer per legacy item name and damage ("minecraft:wool|3" -> "minecraft:blue_wool"), kept in
     * {@code .legacyconnector/vanilla-items-<data version>.txt} across launches: running a stack through the fixer chain costs about a
     * millisecond each and the legacy mods ask for thousands (recipes, ore dictionary, creative tabs). The answers depend on the game's
     * data version only.
     */
    private static final Map<String, String> FIXED_IDS = new HashMap<>();
    private static boolean fixedLoaded;
    private static boolean fixedDirty;

    private static java.nio.file.Path fixedFile() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve(".legacyconnector").resolve("vanilla-items-" + dataVersion() + ".txt");
    }

    private static void loadFixed() {
        if (fixedLoaded) return;
        fixedLoaded = true;
        try {
            java.nio.file.Path file = fixedFile();
            if (!java.nio.file.Files.exists(file)) return;
            for (String line : java.nio.file.Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8)) {
                int tab = line.indexOf('	');
                if (tab > 0) FIXED_IDS.put(line.substring(0, tab), line.substring(tab + 1));
            }
        } catch (java.io.IOException | RuntimeException e) {
            LOG.debug("Could not read the cache of upgraded legacy item names", e);
        }
    }

    /** Writes the cache of upgraded item names when it grew (called once the legacy mods have loaded). */
    public static synchronized void saveFixedItems() {
        if (!fixedDirty) return;
        fixedDirty = false;
        try {
            java.nio.file.Path file = fixedFile();
            java.nio.file.Files.createDirectories(file.getParent());
            StringBuilder sb = new StringBuilder();
            new java.util.TreeMap<>(FIXED_IDS).forEach((k, v) -> sb.append(k).append('	').append(v).append('
'));
            java.nio.file.Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            java.nio.file.Files.writeString(tmp, sb, java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException | RuntimeException e) {
            LOG.debug("Could not write the cache of upgraded legacy item names", e);
        }
    }

    /** Modern item for a legacy vanilla item (or block-item) name and metadata/damage. */
    public static synchronized Item item(String legacyName, int meta) {
        String name = modernised(legacyName);
        int key = (name.hashCode() * 31) ^ meta;
        Item cached = ITEMS.get(key);
        if (cached != null) return cached;
        Item result = Items.AIR;
        loadFixed();
        String fixedKey = name + "|" + meta;
        String id = FIXED_IDS.get(fixedKey);
        try {
            if (id == null) {
                CompoundTag stack = new CompoundTag();
                stack.putString("id", name);
                stack.putByte("Count", (byte) 1);
                stack.putShort("Damage", (short) meta);
                Dynamic<Tag> out = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stack), LEGACY_ITEM_VERSION, dataVersion());
                id = out.get("id").asString("minecraft:air");
                FIXED_IDS.put(fixedKey, id);
                fixedDirty = true;
            }
            result = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        } catch (RuntimeException e) {
            LOG.debug("Could not upgrade legacy item {}:{}", name, meta, e);
        }
        ITEMS.put(key, result);
        return result;
    }

    /** Builds a reverse index modern item -> (legacy name, meta) for the given legacy names and metas 0..15. */
    public static Map<Item, Object[]> reverseItems(Iterable<String> legacyNames) {
        Map<Item, Object[]> out = new HashMap<>();
        for (String name : legacyNames) {
            for (int meta = 0; meta < 16; meta++) {
                Item item = item(name, meta);
                if (item != Items.AIR) out.putIfAbsent(item, new Object[] {name, meta});
            }
        }
        return out;
    }
}
