package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

/** Desert temples, jungle temples and witch huts. The modern overworld has them already; mod dimensions that use this generator get none. */
public class MapGenScatteredFeature extends MapGenStructure {
    public MapGenScatteredFeature() {
    }

    /** Witches spawn in swamp huts. */
    private final java.util.List<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry> scatteredFeatureSpawnList = new java.util.ArrayList<>();

    {
        MapGenNetherBridge.mcc$spawn(scatteredFeatureSpawnList, "Witch", 1, 1, 1);
    }

    public java.util.List getScatteredFeatureSpawnList() {
        return scatteredFeatureSpawnList;
    }

    /** Whether the position is inside a swamp hut of this generator (chunk providers spawn witches there). */
    public boolean func_143030_a(int x, int y, int z) {
        StructureStart start = func_143028_c(x, y, z);
        if (start == null || start.getComponents().isEmpty()) return false;
        return start.getComponents().get(0).getClass().getSimpleName().equals("SwampHut");
    }

    @Override
    public String getStructureName() {
        return "Temple";
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
