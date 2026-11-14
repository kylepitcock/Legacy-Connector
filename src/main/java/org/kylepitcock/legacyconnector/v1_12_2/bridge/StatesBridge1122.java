package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.core.flatten.VanillaFlattening;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;

/**
 * Converts between 1.12.2 block states (properties) and modern ones: the META property of a legacy block, or for vanilla blocks the
 * 1.12 numeric id and metadata through the game's flattening table (stone:1 is granite, a lit furnace is LIT=true), so vanilla states
 * keep their properties both ways when the legacy block is an instance of its own class (Blocks.STONE is a BlockStone).
 */
public final class StatesBridge1122 {
    private StatesBridge1122() {}

    /** Modern state -> legacy vanilla state (legacy states are interned, as modern ones are). */
    private static final Map<BlockState, Object> TO_LEGACY = new ConcurrentHashMap<>();
    /** Legacy vanilla state -> modern state. */
    private static final Map<IBlockState, BlockState> TO_MODERN = new ConcurrentHashMap<>();
    /** A sentinel for "no legacy state" in {@link #TO_LEGACY}. */
    private static final Object NONE = new Object();

    /** The legacy block of a modern block (a legacy mod block or a vanilla one), or null. */
    public static Block legacyBlock(BlockState state) {
        if (state.getBlock() instanceof Legacy1122Block b) return b.legacy();
        IBlockState legacy = vanillaToLegacy(state);
        if (legacy != null) return legacy.getBlock();
        Block vanilla = Vanilla1122.legacyBlockOf(state.getBlock());
        return vanilla != null || state.isAir() ? vanilla : Vanilla1122.standInBlock(state.getBlock());
    }

    public static IBlockState toLegacy(BlockState state) {
        if (state.getBlock() instanceof Legacy1122Block b) {
            try {
                return b.legacy().getStateFromMeta(state.getValue(Legacy1122Block.META));
            } catch (RuntimeException | LinkageError e) {
                return b.legacy().getDefaultState();
            }
        }
        IBlockState legacy = vanillaToLegacy(state);
        if (legacy != null) return legacy;
        Block vanilla = Vanilla1122.legacyBlockOf(state.getBlock());
        // a block 1.12.2 never had (a barrel, a modern mod's machine): its own stand-in, so legacy code does not see air where it stands
        if (vanilla == null && !state.isAir()) vanilla = Vanilla1122.standInBlock(state.getBlock());
        return vanilla == null ? null : vanilla.getDefaultState();
    }

    /** The legacy state of a modern vanilla state by its 1.12 id and metadata, or null when 1.12 had no such block. */
    private static IBlockState vanillaToLegacy(BlockState state) {
        Object known = TO_LEGACY.get(state);
        if (known != null) return known == NONE ? null : (IBlockState) known;
        IBlockState out = null;
        try {
            int key = VanillaFlattening.nearestLegacyKey(state);
            String name = key < 0 ? null : VanillaFlattening.legacyName(key >> 4);
            if (name != null) {
                Block block = Registries1122.BLOCKS.getValue(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name));
                // only a block of its own class has the properties of the metadata (a plain wrapper keeps the old answer)
                if (block != null && (!block.getBlockState().getProperties().isEmpty() || block.mcc$modern == state.getBlock())) {
                    out = block.getStateFromMeta(key & 15);
                }
            }
        } catch (RuntimeException | LinkageError e) {
            out = null;
        }
        TO_LEGACY.put(state, out == null ? NONE : out);
        return out;
    }

    /**
     * Whether the block's metadata stands for this exact state, so that {@link #toModern} keeps all of it. A state with properties
     * the metadata does not store (a render part only the mod's renderer asks for, a connection from getActualState) is not.
     */
    public static boolean metaHolds(IBlockState state) {
        if (state == null) return true;
        state = canonical(state);
        try {
            Block block = state.getBlock();
            return state.equals(block.getStateFromMeta(block.getMetaFromState(state)));
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    /**
     * The block's own state instance with the properties of a state some mod wrapped or copied (EnderIO's BlockStateWrapperBase delegates the
     * properties to the state it wraps but is not equal to it): lookups by state identity and equals need the container's instance.
     */
    public static IBlockState canonical(IBlockState state) {
        if (state == null || state instanceof legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer.StateImplementation) return state;
        try {
            IBlockState out = state.getBlock().getDefaultState();
            for (var e : state.getProperties().entrySet()) out = withValue(out, e.getKey(), e.getValue());
            return out;
        } catch (RuntimeException | LinkageError e) {
            return state;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState withValue(IBlockState state, legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty property, Comparable value) {
        return state.withProperty(property, value);
    }

    public static BlockState toModern(IBlockState state) {
        if (state == null) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        Block legacy = state.getBlock();
        if (legacy.mcc$modern instanceof Legacy1122Block b) {
            int meta;
            try {
                meta = legacy.getMetaFromState(state) & 15;
            } catch (RuntimeException | LinkageError e) {
                meta = 0;
            }
            return b.defaultBlockState().setValue(Legacy1122Block.META, meta);
        }
        if (legacy.mcc$modern instanceof net.minecraft.world.level.block.Block vanilla) {
            if (state.getPropertyKeys().isEmpty()) return vanilla.defaultBlockState();
            BlockState known = TO_MODERN.get(state);
            if (known != null) return known;
            BlockState out = vanilla.defaultBlockState();
            try {
                var name = legacy.getRegistryName();
                int id = name == null || !"minecraft".equals(name.getNamespace()) ? -1 : VanillaFlattening.blockId(name.getPath());
                if (id >= 0) {
                    BlockState flat = VanillaFlattening.state(id, legacy.getMetaFromState(state) & 15);
                    if (flat != null && !flat.isAir()) out = flat;
                }
            } catch (RuntimeException | LinkageError e) {
                // the block's default state
            }
            TO_MODERN.put(state, out);
            return out;
        }
        // a legacy block without a registered modern block (a duplicate name, a failed registration) can not be stored in a chunk or sent to a client
        if (legacy.mcc$modern == null && UNREGISTERED_REPORTED.add(legacy.getClass().getName())) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").warn("{} ({}) has no registered modern block; placing it places air instead",
                    legacy.getClass().getName(), legacy.getRegistryName());
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    private static final java.util.Set<String> UNREGISTERED_REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();
}
