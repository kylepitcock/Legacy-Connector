package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

/** The stronghold generator. The modern overworld has strongholds already; for dimensions that create one this places none. */
public class MapGenStronghold extends MapGenStructure {
    public MapGenStronghold() {
    }

    @Override
    public String getStructureName() {
        return "Stronghold";
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
