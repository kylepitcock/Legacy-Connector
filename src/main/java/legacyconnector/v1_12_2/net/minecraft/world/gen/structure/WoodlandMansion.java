package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 generator of the "Mansion" structure. The modern game places these structures itself, so this one starts none of its own;
 * nearest/inside queries answer from the modern structures (see {@code Structures1122}).
 */
public class WoodlandMansion extends MapGenStructure {
    public WoodlandMansion() {
    }

    public static final java.util.List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome> ALLOWED_BIOMES = java.util.Arrays.asList(
            legacyconnector.v1_12_2.net.minecraft.init.Biomes.ROOFED_FOREST, legacyconnector.v1_12_2.net.minecraft.init.Biomes.MUTATED_ROOFED_FOREST);

    /** The mansion generator of an overworld generator (1.12 used it for the terrain height at a candidate). */
    public WoodlandMansion(legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorOverworld chunkGenerator) {
    }

    @Override
    public String getStructureName() {
        return "Mansion";
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        return false;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new StructureStart(chunkX, chunkZ);
    }

    @Override
    public BlockPos getNearestStructurePos(World worldIn, BlockPos pos, boolean findUnexplored) {
        this.world = worldIn;
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, "Mansion", pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(this.world, "Mansion", pos) || super.isInsideStructure(pos);
    }

    @Override
    public boolean isPositionInStructure(World worldIn, BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(worldIn, "Mansion", pos) || super.isPositionInStructure(worldIn, pos);
    }
}
