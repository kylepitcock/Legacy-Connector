package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.HashMap;
import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Modern block states as b1.7.3 block ids and metadata, and back, for the legacy world view. A mod block is its legacy id with its
 * {@code meta} property; vanilla blocks are their b1.7.3 id (wool colours, logs, leaves, saplings and slabs with their metadata, wheat with
 * its age). A modern block b1.7.3 did not have reads as stone when it is solid, else as air.
 */
public final class BlocksB173 {
    private static volatile Map<net.minecraft.world.level.block.Block, int[]> reverse;

    private BlocksB173() {}

    public static int legacyId(BlockState state) {
        if (state.getBlock() instanceof LegacyB173Block l) return l.legacy().id;
        if (state.isAir()) return 0;
        if(state.is(Blocks.TORCH)||state.is(Blocks.WALL_TORCH))return 50;
        if(state.is(Blocks.REDSTONE_TORCH)||state.is(Blocks.REDSTONE_WALL_TORCH))
            return state.getValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT)?76:75;
        var fluid = state.getFluidState();
        if (!fluid.isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            if (fluid.getType() == Fluids.WATER) return 9;
            if (fluid.getType() == Fluids.FLOWING_WATER) return 8;
            if (fluid.getType() == Fluids.LAVA) return 11;
            if (fluid.getType() == Fluids.FLOWING_LAVA) return 10;
        }
        int[] hit = reverse().get(state.getBlock());
        if (hit != null) return hit[0]==44&&state.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)
                &&state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE?43:hit[0];
        return state.canOcclude() ? 1 : 0;
    }

    static boolean hasVanillaId(BlockState state){return reverse().containsKey(state.getBlock());}

    public static int legacyMeta(BlockState state) {
        if (state.getBlock() instanceof LegacyB173Block) return state.getValue(LegacyB173Block.META);
        Integer placed = VanillaBlockStatesB173.legacyMeta(state);
        if (placed != null) return placed;
        if (state.getBlock() instanceof CropBlock crop) return crop.getAge(state);
        var fluid = state.getFluidState();
        if (!fluid.isEmpty() && !fluid.isSource()) return Math.max(0, 8 - fluid.getAmount());
        int[] hit = reverse().get(state.getBlock());
        return hit != null ? hit[1] : 0;
    }

    /** The modern state of a legacy block id and metadata (air for 0 or unknown ids). */
    public static BlockState modernState(int id, int meta) {
        if (id <= 0 || id >= Block.BLOCKS.length) return Blocks.AIR.defaultBlockState();
        Block legacy = Block.BLOCKS[id];
        if (legacy != null && legacy.mcc$modern instanceof LegacyB173Block modern) return modern.defaultBlockState().setValue(LegacyB173Block.META, meta & 15);
        if (legacy != null && legacy.mcc$owner != null && legacy.mcc$modern != null) return legacy.mcc$modern.defaultBlockState();
        // Beta stored block metadata in a nibble: a mod's -1 ("unchanged") is 15 there
        net.minecraft.world.item.Item variant = VanillaB173.modernItem(id, meta & 15);
        if (variant instanceof net.minecraft.world.item.BlockItem bi && (id == 35 || id == 17 || id == 18 || id == 6 || id == 44 || id == 43)) {
            var state=bi.getBlock().defaultBlockState();
            return id==43&&state.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)
                    ?state.setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE):state;
        }
        var block = VanillaB173.modernBlock(id);
        if (block == null) return Blocks.AIR.defaultBlockState();
        BlockState state = block.defaultBlockState();
        BlockState placed = VanillaBlockStatesB173.modernState(id, meta, state);
        if (placed != null) return placed;
        if (block instanceof CropBlock crop) state = crop.getStateForAge(Math.min(meta, crop.getMaxAge()));
        return state;
    }

    public static net.minecraft.world.level.block.Block modernBlock(int id) {
        return modernState(id, 0).getBlock();
    }

    private static Map<net.minecraft.world.level.block.Block, int[]> reverse() {
        Map<net.minecraft.world.level.block.Block, int[]> r = reverse;
        if (r != null) return r;
        synchronized (BlocksB173.class) {
            if (reverse != null) return reverse;
            r = new HashMap<>();
            for (int id = 1; id < 256; id++) {
                var b = VanillaB173.modernBlock(id);
                if (b != null) r.putIfAbsent(b, new int[] {id, 0});
            }
            String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
                    "brown", "green", "red", "black"};
            for (int m = 0; m < 16; m++) put(r, colors[m] + "_wool", 35, m);
            String[] woods = {"oak", "spruce", "birch"};
            for (int m = 0; m < 3; m++) {
                put(r, woods[m] + "_log", 17, m);
                put(r, woods[m] + "_leaves", 18, m);
                put(r, woods[m] + "_sapling", 6, m);
            }
            String[] slabs = {"smooth_stone_slab", "sandstone_slab", "oak_slab", "cobblestone_slab"};
            for (int m = 0; m < 4; m++) put(r, slabs[m], 44, m);
            r.put(Blocks.SHORT_GRASS, new int[] {31, 1});
            r.put(Blocks.FERN, new int[] {31, 2});
            r.put(Blocks.WALL_TORCH,new int[]{50,0});
            r.put(Blocks.REDSTONE_WALL_TORCH,new int[]{75,0});
            return reverse = r;
        }
    }

    private static void put(Map<net.minecraft.world.level.block.Block, int[]> r, String path, int id, int meta) {
        ResourceLocation rl = ResourceLocation.withDefaultNamespace(path);
        if (BuiltInRegistries.BLOCK.containsKey(rl)) r.put(BuiltInRegistries.BLOCK.get(rl), new int[] {id, meta});
    }
}
