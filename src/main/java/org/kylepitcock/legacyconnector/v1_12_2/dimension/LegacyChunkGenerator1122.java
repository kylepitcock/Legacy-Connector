package org.kylepitcock.legacyconnector.v1_12_2.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;
import legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chunk generator of a 1.12.2 mod dimension: runs the {@code IChunkGenerator} of the mod's {@code WorldProvider}. Terrain comes from
 * {@code generateChunk} (the primer's states converted into the proto chunk through the block bridge, on a worker thread), trees,
 * structures and ores from {@code populate} plus the mods' {@code IWorldGenerator}s, run on the generation region (the mod's world
 * object writes into the region on that thread, see {@code World.mcc$enterRegion}).
 */
public class LegacyChunkGenerator1122 extends ChunkGenerator {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Dimensions");
    public static final MapCodec<LegacyChunkGenerator1122> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("dimension").forGetter(g -> g.dimension),
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, LegacyChunkGenerator1122::new));

    private final int dimension;
    private volatile IChunkGenerator legacy;
    private boolean failed;

    public LegacyChunkGenerator1122(int dimension, BiomeSource biomeSource) {
        super(biomeSource);
        this.dimension = dimension;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** The mod's chunk generator (created once, from the provider of the dimension's level). */
    private synchronized IChunkGenerator legacy() {
        if (legacy == null && !failed) {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            var level = server == null ? null : server.getLevel(Dimensions1122.levelKey(dimension));
            if (level != null) {
                try {
                    World world = WorldBridge1122.wrap(level);
                    legacy = World.mcc$get$provider(world).createChunkGenerator();
                    if (legacy == null) {
                        LOG.warn("The world provider of 1.12.2 dimension {} has no chunk generator", dimension);
                        failed = true;
                    }
                } catch (RuntimeException | LinkageError e) {
                    LOG.error("The chunk generator of 1.12.2 dimension {} could not be created", dimension, e);
                    failed = true;
                }
            }
        }
        return legacy;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            IChunkGenerator generator = legacy();
            if (generator == null) return chunk;
            int cx = chunk.getPos().x, cz = chunk.getPos().z;
            Chunk generated;
            try {
                synchronized (generator) {
                    generated = generator.generateChunk(cx, cz);
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.error("1.12.2 dimension {} failed to generate chunk {},{}", dimension, cx, cz, e);
                return chunk;
            }
            ChunkPrimer primer = generated == null ? null : generated.mcc$primer();
            if (primer == null) return chunk;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        if (!primer.mcc$isSet(x, y, z)) continue;
                        BlockState state;
                        try {
                            state = StatesBridge1122.toModern(primer.getBlockState(x, y, z));
                        } catch (RuntimeException | LinkageError e) {
                            continue;
                        }
                        if (state == null || state.isAir()) continue;
                        chunk.setBlockState(pos.set((cx << 4) + x, y, (cz << 4) + z), state, false);
                    }
                }
            }
            return chunk;
        }, Util.backgroundExecutor());
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        IChunkGenerator generator = legacy();
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        WorldGenLevel previous = World.mcc$enterRegion(level);
        try {
            if (generator != null) {
                try {
                    synchronized (generator) {
                        generator.populate(cx, cz);
                    }
                } catch (RuntimeException | LinkageError e) {
                    LOG.error("1.12.2 dimension {} failed to populate chunk {}", dimension, chunk.getPos(), e);
                }
            }
            // GameRegistry.generateWorld ran the mods' IWorldGenerators right after the provider's populate, in every dimension
            try {
                org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldGenBridge1122.decorateModDimension(level, cx, cz);
            } catch (RuntimeException | LinkageError e) {
                LOG.error("1.12.2 dimension {}: world generators failed in chunk {}", dimension, chunk.getPos(), e);
            }
        } finally {
            World.mcc$leaveRegion(previous);
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures,
            ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 64;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return new NoiseColumn(level.getMinBuildHeight(), new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Legacy 1.12.2 dimension " + dimension);
    }
}
