package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

/** The nether fortress generator. The modern Nether already has fortresses, so this places none. */
public class MapGenNetherBridge extends MapGenStructure {
    public MapGenNetherBridge() {
    }

    /** The mobs that spawn inside fortresses: blazes, zombie pigmen, skeletons and magma cubes. */
    private final java.util.List<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry> spawnList = new java.util.ArrayList<>();

    {
        mcc$spawn(spawnList, "Blaze", 10, 2, 3);
        mcc$spawn(spawnList, "PigZombie", 5, 4, 4);
        mcc$spawn(spawnList, "Skeleton", 10, 4, 4);
        mcc$spawn(spawnList, "LavaSlime", 3, 4, 4);
    }

    /** Adds a spawn entry for a vanilla mob by its 1.7.10 name (left out when its legacy class is not available). */
    static void mcc$spawn(java.util.List<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry> list, String name, int weight, int min, int max) {
        Class<?> cls = legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.get(name);
        if (cls != null) list.add(new legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry(cls, weight, min, max));
    }

    public java.util.List getSpawnList() {
        return spawnList;
    }

    @Override
    public String getStructureName() {
        return "Fortress";
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int x, int z) {
        return false;
    }

    @Override
    protected StructureStart getStructureStart(int x, int z) {
        return null;
    }
}
