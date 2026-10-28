package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 generator of the "Monument" structure. The modern game places these structures itself, so this one starts none of its own;
 * nearest/inside queries answer from the modern structures (see {@code Structures1122}).
 */
public class StructureOceanMonument extends MapGenStructure {
    public StructureOceanMonument() {
    }

    public java.util.List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> getMonsters() {
        return new java.util.ArrayList<>();
    }

    @Override
    public String getStructureName() {
        return "Monument";
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
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, "Monument", pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(this.world, "Monument", pos) || super.isInsideStructure(pos);
    }

    @Override
    public boolean isPositionInStructure(World worldIn, BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(worldIn, "Monument", pos) || super.isPositionInStructure(worldIn, pos);
    }
}
