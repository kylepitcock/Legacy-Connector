package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;
import legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderEnd;
import legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderFlat;
import legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderGenerate;
import legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderHell;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.FlatLevelSource;

/**
 * The {@code chunkGenerator} argument of {@code IWorldGenerator.generate}. Forge handed the dimension's own generator, and many mods
 * tell the dimensions apart by its class ({@code chunkGenerator instanceof ChunkProviderHell}, {@code ChunkProviderEnd},
 * {@code ChunkProviderGenerate}/{@code ChunkProviderFlat}), usually with the overworld as the branch every other class falls into.
 * A plain placeholder is an instance of none of them: such a mod then builds its overworld content in the Nether and the End (in the
 * End's void at y=0). The modern game generates the terrain itself, so the argument is a stand-in that is an instance of the class
 * 1.7.10 passed in that dimension and does nothing of its own: no terrain, no chunks, no structures.
 *
 * <p>The stand-ins are made without running a constructor (a vanilla generator's constructor builds noise generators and posts
 * the mods' noise events), and every method that would read the fields it never got is overridden.
 */
final class GeneratorProviders1710 {
    private static volatile IChunkProvider hell, end, overworld, flat;

    private GeneratorProviders1710() {}

    /** The stand-in of the dimension's vanilla generator class, or {@code fallback} for a dimension 1.7.10 had no generator class of. */
    static IChunkProvider forLevel(WorldGenLevel level, IChunkProvider fallback) {
        try {
            Level full = level.getLevel();
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

    private static IChunkProvider make(Class<? extends IChunkProvider> type) throws ReflectiveOperationException {
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }

    // The stand-ins. The constructors are never run (allocateInstance); they only let the classes compile.

    static final class Hell extends ChunkProviderHell {
        private Hell() {
            super(null, 0L);
        }

        @Override
        public Chunk provideChunk(int cx, int cz) {
            return null;
        }

        @Override
        public Chunk loadChunk(int cx, int cz) {
            return null;
        }

        @Override
        public void populate(IChunkProvider provider, int cx, int cz) {
        }

        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
            return null;
        }

        @Override
        public legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition findClosestStructure(World world, String name, int x, int y, int z) {
            return null;
        }

        @Override
        public void recreateStructures(int cx, int cz) {
        }

        @Override
        public boolean saveChunks(boolean all, Object progress) {
            return true;
        }
    }

    static final class End extends ChunkProviderEnd {
        private End() {
            super(null, 0L);
        }

        @Override
        public Chunk provideChunk(int cx, int cz) {
            return null;
        }

        @Override
        public Chunk loadChunk(int cx, int cz) {
            return null;
        }

        @Override
        public void populate(IChunkProvider provider, int cx, int cz) {
        }

        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
            return null;
        }

        @Override
        public void recreateStructures(int cx, int cz) {
        }

        @Override
        public boolean saveChunks(boolean all, Object progress) {
            return true;
        }
    }

    static final class Overworld extends ChunkProviderGenerate {
        private Overworld() {
            super(null, 0L, false);
        }

        @Override
        public Chunk provideChunk(int cx, int cz) {
            return null;
        }

        @Override
        public void populate(IChunkProvider provider, int cx, int cz) {
        }

        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }
    }

    static final class Flat extends ChunkProviderFlat {
        private Flat() {
            super(null, 0L, false, null);
        }

        @Override
        public Chunk provideChunk(int cx, int cz) {
            return null;
        }

        @Override
        public Chunk loadChunk(int cx, int cz) {
            return null;
        }

        @Override
        public void populate(IChunkProvider provider, int cx, int cz) {
        }

        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public void recreateStructures(int cx, int cz) {
        }
    }
}
