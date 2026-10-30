package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 generator of villages. The modern game places these structures itself, so this one starts none of its own; the queries
 * legacy code makes ({@code getNearestStructurePos}, {@code isInsideStructure}) answer from the modern structures (see
 * {@code Structures1122}).
 */
public class MapGenVillage extends MapGenStructure {
    /** The biomes villages may start in (plains, desert, savanna, taiga, and those mods add through {@code BiomeManager.addVillageBiome}). */
    public static java.util.List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome> VILLAGE_SPAWN_BIOMES =
            legacyconnector.v1_12_2.net.minecraftforge.common.BiomeManager.mcc$villageBiomes();

    public MapGenVillage() {
    }

    public MapGenVillage(java.util.Map<String, String> options) {
    }

    @Override
    public String getStructureName() {
        return "Village";
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
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, "Village", pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(this.world, "Village", pos) || super.isInsideStructure(pos);
    }
}
