package legacyconnector.v1_12_2.net.minecraft.client.renderer.color;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import org.kylepitcock.legacyconnector.v1_12_2.client.Colors1122;

/**
 * Block tints. A handler registered for a mod block tints its modern block (see {@link Colors1122}); asking for a colour answers the
 * registered handler, or the game's colour of the modern state (grass, foliage, water of vanilla blocks).
 */
public class BlockColors {
    private final Map<Block, IBlockColor> handlers = new ConcurrentHashMap<>();
    /**
     * Forge's field (by delegate), read by reflection: mods (Quark, Botania) take vanilla's vine and grass handlers from it to tint
     * their own blocks. A view: a mod handler, else one answering the game's colour of the block; put registers a handler.
     */
    @SuppressWarnings("unused")
    private final Map<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<Block>, IBlockColor> blockColorMap = new java.util.AbstractMap<>() {
        @Override
        public IBlockColor get(Object key) {
            if (!(key instanceof legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<?> d) || !(d.get() instanceof Block block)) return null;
            IBlockColor handler = handlers.get(block);
            if (handler != null) return handler;
            // the game's own handler, taken now: a mod wraps it and registers the wrapper for the same block (Quark's grass)
            if (block.mcc$modern instanceof net.minecraft.world.level.block.Block modern) Colors1122.snapshot(modern);
            return (state, world, pos, tint) -> Colors1122.originalBlockColor(state, pos, tint);
        }

        @Override
        public IBlockColor put(legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<Block> key, IBlockColor value) {
            IBlockColor old = key == null ? null : handlers.get(key.get());
            if (key != null && key.get() != null) registerBlockColorHandler(value, key.get());
            return old;
        }

        @Override
        public java.util.Set<Entry<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<Block>, IBlockColor>> entrySet() {
            java.util.Set<Entry<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<Block>, IBlockColor>> out = new java.util.LinkedHashSet<>();
            for (var e : handlers.entrySet()) out.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey().delegate, e.getValue()));
            return out;
        }
    };

    public BlockColors() {
    }

    /** The game's block colours (Minecraft.getBlockColors()). */
    public static BlockColors mcc$instance() {
        return Colors1122.blocks();
    }

    public void registerBlockColorHandler(IBlockColor blockColor, Block... blocksIn) {
        if (blockColor == null || blocksIn == null) return;
        for (Block block : blocksIn) {
            if (block == null) continue;
            handlers.put(block, blockColor);
            if (this == mcc$instance()) Colors1122.registerBlock(block, blockColor);
        }
    }

    public IBlockColor mcc$handler(Block block) {
        return handlers.get(block);
    }

    public Map<Block, IBlockColor> mcc$handlers() {
        return handlers;
    }

    public int getColor(IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, BlockPos pos) {
        IBlockColor handler = handlers.get(state.getBlock());
        if (handler != null) return handler.colorMultiplier(state, null, null, 0);
        return Colors1122.modernBlockColor(state, pos, 0);
    }

    public int colorMultiplier(IBlockState state, IBlockAccess blockAccess, BlockPos pos, int renderPass) {
        if (state == null) return -1;
        IBlockColor handler = handlers.get(state.getBlock());
        if (handler != null) {
            try {
                return handler.colorMultiplier(state, blockAccess, pos, renderPass);
            } catch (RuntimeException e) {
                return -1;
            }
        }
        return Colors1122.modernBlockColor(state, pos, renderPass);
    }

    /** 1.12 built the game's block colours here; the game's own (with the mods' handlers) is returned. */
    public static BlockColors init() {
        return mcc$instance();
    }
}
