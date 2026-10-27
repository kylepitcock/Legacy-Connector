package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 generator of nether fortresses. The modern game places these structures itself, so this one starts none of its own; the queries
 * legacy code makes ({@code getNearestStructurePos}, {@code isInsideStructure}) answer from the modern structures (see
 * {@code Structures1122}).
 */
public class MapGenNetherBridge extends MapGenStructure {
    public MapGenNetherBridge() {
    }

    public MapGenNetherBridge(java.util.Map<String, String> options) {
    }

    @Override
    public String getStructureName() {
        return "Fortress";
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
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, "Fortress", pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(this.world, "Fortress", pos) || super.isInsideStructure(pos);
    }
}
