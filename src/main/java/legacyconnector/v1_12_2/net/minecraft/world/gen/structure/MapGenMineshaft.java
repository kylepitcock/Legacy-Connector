package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 generator of mineshafts. The modern game places these structures itself, so this one starts none of its own; the queries
 * legacy code makes ({@code getNearestStructurePos}, {@code isInsideStructure}) answer from the modern structures (see
 * {@code Structures1122}).
 */
public class MapGenMineshaft extends MapGenStructure {
    public MapGenMineshaft() {
    }

    public MapGenMineshaft(java.util.Map<String, String> options) {
    }

    @Override
    public String getStructureName() {
        return "Mineshaft";
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
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, "Mineshaft", pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(this.world, "Mineshaft", pos) || super.isInsideStructure(pos);
    }
}
