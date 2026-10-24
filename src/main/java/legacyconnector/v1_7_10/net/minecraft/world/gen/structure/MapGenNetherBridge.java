package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

/** The nether fortress generator. The modern Nether already has fortresses, so this places none. */
public class MapGenNetherBridge extends MapGenStructure {
    public MapGenNetherBridge() {
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
