package org.kylepitcock.mccompat.v1_7_10.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraft.world.chunk.Chunk;
import mccompat.v1_7_10.net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
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
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chunk generator of a mod dimension: runs the legacy {@code IChunkProvider} of the mod's world provider. Terrain
 * comes from {@code provideChunk} (block and metadata arrays copied into the chunk); structures, trees and ores
 * from {@code populate}, run against the generation region.
 */
public class LegacyChunkGenerator extends ChunkGenerator {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Dimensions");
    public static final MapCodec<LegacyChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("dimension").forGetter(g -> g.dimension),
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, LegacyChunkGenerator::new));

    private final int dimension;
    private volatile IChunkProvider legacy;
    private boolean failed;

    public LegacyChunkGenerator(int dimension, BiomeSource biomeSource) {
        super(biomeSource);
        this.dimension = dimension;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** The mod's chunk provider (created once, from the provider of the dimension's level). */
    private synchronized IChunkProvider legacy() {
        if (legacy == null && !failed) {
            var provider = LegacyDimensions.provider(dimension);
            if (provider != null) {
                try {
                    legacy = provider.createChunkGenerator();
                    if (legacy == null) {
                        LOG.warn("The world provider of dimension {} has no chunk generator", dimension);
                        failed = true;
                    }
                } catch (RuntimeException | LinkageError e) {
                    LOG.error("The chunk generator of dimension {} could not be created", dimension, e);
                    failed = true;
                }
            }
        }
        return legacy;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            IChunkProvider provider = legacy();
            if (provider == null) return chunk;
            int cx = chunk.getPos().x, cz = chunk.getPos().z;
            Chunk generated;
            try {
                synchronized (provider) {
                    synchronized (biomeLock(provider)) {
                        Chunk.mcc$beginDetached();
                        try {
                            generated = provider.provideChunk(cx, cz);
                        } finally {
                            Chunk.mcc$endDetached();
                        }
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.error("Dimension {} failed to generate chunk {},{}", dimension, cx, cz, e);
                return chunk;
            }
            if (generated == null || !generated.mcc$hasArrays()) return chunk;
            Block[] blocks = generated.mcc$blocks();
            byte[] meta = generated.mcc$meta();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int height = generated.mcc$arrayHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int column = (x * 16 + z) * height;
                    for (int y = 0; y < height && y < 256; y++) {
                        Block block = blocks[column | y];
                        if (block == null) continue;
                        BlockState state = BlockBridge.toModern(block, meta[column | y] & 15);
                        if (state.isAir()) continue;
                        chunk.setBlockState(pos.set((cx << 4) + x, y, (cz << 4) + z), state, false);
                    }
                }
            }
            return chunk;
        }, Util.backgroundExecutor());
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        IChunkProvider provider = legacy();
        if (provider == null) return;
        LevelAccessor previous = World.mcc$enterRegion(level);
        try {
            synchronized (provider) {
                synchronized (biomeLock(provider)) {
                    provider.populate(provider, chunk.getPos().x, chunk.getPos().z);
                }
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("Dimension {} failed to populate chunk {}", dimension, chunk.getPos(), e);
        } finally {
            World.mcc$leaveRegion(previous);
        }
    }

    /** Legacy biome layers keep per-call state: every use of them from this dimension shares one monitor. */
    private Object biomeLock(IChunkProvider provider) {
        var wp = LegacyDimensions.provider(dimension);
        return wp != null && wp.worldChunkMgr != null ? wp.worldChunkMgr : provider;
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
        info.add("Legacy dimension " + dimension);
    }
}
