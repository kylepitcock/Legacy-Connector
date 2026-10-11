package org.kylepitcock.mccompat.core.flatten;

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
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Flattening");
    /** Data version of post-flattening block states (BlockStateFlatteningFix lives in schema 1450). */
    private static final int FLATTENED_BLOCK_VERSION = 1450;
    /** 1.12.2 data version; item stacks in legacy format are upgraded from here. */
    private static final int LEGACY_ITEM_VERSION = 1343;

    private static BlockState[] states;
    private static Map<BlockState, Integer> reverseStates;
    private static Map<Block, Integer> reverseBlocks;
    private static Map<String, Integer> blockIds;
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
        for (Object2IntMap.Entry<String> e : BlockStateData.ID_BY_OLD_NAME.object2IntEntrySet()) {
            blockIds.put(e.getKey(), e.getIntValue() >> 4);
        }
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
        if (s == null) s = states[legacyId << 4];
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

    public static int itemId(String legacyName) {
        initItemIds();
        Integer id = itemIds.get(legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName);
        return id == null ? -1 : id;
    }

    /** Modern item for a legacy vanilla item (or block-item) name and metadata/damage. */
    public static synchronized Item item(String legacyName, int meta) {
        String name = legacyName.indexOf(':') < 0 ? "minecraft:" + legacyName : legacyName;
        int key = (name.hashCode() * 31) ^ meta;
        Item cached = ITEMS.get(key);
        if (cached != null) return cached;
        Item result = Items.AIR;
        try {
            CompoundTag stack = new CompoundTag();
            stack.putString("id", name);
            stack.putByte("Count", (byte) 1);
            stack.putShort("Damage", (short) meta);
            Dynamic<Tag> out = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stack), LEGACY_ITEM_VERSION, dataVersion());
            String id = out.get("id").asString("minecraft:air");
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
