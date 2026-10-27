package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/** A 1.12.2 chunk generator: a dimension's terrain, population and structures. */
public interface IChunkGenerator {
    Chunk generateChunk(int x, int z);

    void populate(int x, int z);

    boolean generateStructures(Chunk chunkIn, int x, int z);

    List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos);

    BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored);

    void recreateStructures(Chunk chunkIn, int x, int z);

    boolean isInsideStructure(World worldIn, String structureName, BlockPos pos);
}
