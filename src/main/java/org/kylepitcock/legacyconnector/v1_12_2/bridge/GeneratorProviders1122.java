package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorEnd;
import legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorFlat;
import legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorHell;
import legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorOverworld;
import legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;

/**
 * The {@code chunkGenerator} argument of {@code IWorldGenerator.generate}: Forge handed the dimension's own generator, and many mods
 * tell the dimensions apart by its class ({@code chunkGenerator instanceof ChunkGeneratorHell}, {@code ChunkGeneratorEnd}, ...) with
 * the overworld as the branch everything else falls into. A plain placeholder is an instance of none of them, so such a mod builds
 * its overworld content in the Nether and the End (in the End's void at y=0). The stand-ins here are instances of the class 1.12.2
 * passed in that dimension and do nothing of their own. They are made without running a constructor (a vanilla generator's
 * constructor builds noise generators and posts the mods' noise events); every method is overridden, so the fields they never got
 * are never read.
 */
final class GeneratorProviders1122 {
    private static volatile IChunkGenerator hell, end, overworld, flat;

    private GeneratorProviders1122() {}

    /** The stand-in of the dimension's vanilla generator class, or {@code fallback} for a dimension 1.12.2 had no generator class of. */
    static IChunkGenerator forWorld(World world, IChunkGenerator fallback) {
        try {
            var access = world == null ? null : world.mcc$access();
            Level full = access instanceof Level l ? l : access instanceof net.minecraft.world.level.WorldGenLevel g ? g.getLevel() : null;
            if (full == null) return fallback;
            var key = full.dimension();
            if (key == Level.NETHER) return hell != null ? hell : (hell = make(Hell.class));
            if (key == Level.END) return end != null ? end : (end = make(End.class));
            if (key == Level.OVERWORLD) {
                if (full instanceof net.minecraft.server.level.ServerLevel sl && sl.getChunkSource().getGenerator() instanceof FlatLevelSource) {
                    return flat != null ? flat : (flat = make(Flat.class));
                }
                return overworld != null ? overworld : (overworld = make(Overworld.class));
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // no stand-in: the placeholder
        }
        return fallback;
    }

    private static IChunkGenerator make(Class<? extends IChunkGenerator> type) throws ReflectiveOperationException {
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }

    // The stand-ins. The constructors are never run (allocateInstance); they only let the classes compile.

    static final class Hell extends ChunkGeneratorHell {
        private Hell() {
            super(null, false, 0L);
        }

        @Override
        public Chunk generateChunk(int x, int z) {
            return null;
        }

        @Override
        public void populate(int x, int z) {
        }

        @Override
        public boolean generateStructures(Chunk chunkIn, int x, int z) {
            return false;
        }

        @Override
        public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
            return new ArrayList<>();
        }

        @Override
        public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
            return null;
        }

        @Override
        public void recreateStructures(Chunk chunkIn, int x, int z) {
        }

        @Override
        public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
            return false;
        }
    }

    static final class End extends ChunkGeneratorEnd {
        private End() {
            super(null, false, 0L, null);
        }

        @Override
        public Chunk generateChunk(int x, int z) {
            return null;
        }

        @Override
        public void populate(int x, int z) {
        }

        @Override
        public boolean generateStructures(Chunk chunkIn, int x, int z) {
            return false;
        }

        @Override
        public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
            return new ArrayList<>();
        }

        @Override
        public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
            return null;
        }

        @Override
        public void recreateStructures(Chunk chunkIn, int x, int z) {
        }

        @Override
        public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
            return false;
        }
    }

    static final class Overworld extends ChunkGeneratorOverworld {
        private Overworld() {
            super(null, 0L, false, null);
        }

        @Override
        public Chunk generateChunk(int x, int z) {
            return null;
        }

        @Override
        public void populate(int x, int z) {
        }

        @Override
        public boolean generateStructures(Chunk chunkIn, int x, int z) {
            return false;
        }

        @Override
        public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
            return new ArrayList<>();
        }

        @Override
        public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
            return null;
        }

        @Override
        public void recreateStructures(Chunk chunkIn, int x, int z) {
        }

        @Override
        public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
            return false;
        }
    }

    static final class Flat extends ChunkGeneratorFlat {
        private Flat() {
            super(null, 0L, false, null);
        }

        @Override
        public Chunk generateChunk(int x, int z) {
            return null;
        }

        @Override
        public void populate(int x, int z) {
        }

        @Override
        public boolean generateStructures(Chunk chunkIn, int x, int z) {
            return false;
        }

        @Override
        public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
            return new ArrayList<>();
        }

        @Override
        public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
            return null;
        }

        @Override
        public void recreateStructures(Chunk chunkIn, int x, int z) {
        }

        @Override
        public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
            return false;
        }
    }
}
