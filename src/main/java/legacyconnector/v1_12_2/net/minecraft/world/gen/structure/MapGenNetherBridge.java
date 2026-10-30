package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 generator of nether fortresses. The modern game places these structures itself, so this one starts none of its own; the queries
 * legacy code makes ({@code getNearestStructurePos}, {@code isInsideStructure}) answer from the modern structures (see
 * {@code Structures1122}).
 */
public class MapGenNetherBridge extends MapGenStructure {
    private final java.util.List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> spawnList = new java.util.ArrayList<>(java.util.List.of(
            new legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityBlaze.class, 10, 2, 3),
            new legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityPigZombie.class, 5, 4, 4),
            new legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityWitherSkeleton.class, 8, 5, 5),
            new legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.monster.EntitySkeleton.class, 2, 5, 5),
            new legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityMagmaCube.class, 3, 4, 4)));

    public MapGenNetherBridge() {
    }

    /** The mobs that spawn in fortresses (1.12's list; the modern fortress spawns its own). */
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> getSpawnList() {
        return spawnList;
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
