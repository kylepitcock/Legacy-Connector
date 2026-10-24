package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.Map;

/** The village generator. Modern villages come from the overworld's own structure set, so this places none. */
public class MapGenVillage extends MapGenStructure {
    public static java.util.List<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase> villageSpawnBiomes = new java.util.ArrayList<>();
    private int terrainType;

    public MapGenVillage() {
    }

    public MapGenVillage(Map options) {
    }

    @Override
    public String getStructureName() {
        return "Village";
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
