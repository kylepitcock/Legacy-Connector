package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

/** Desert temples, jungle temples and witch huts. The modern overworld has them already; mod dimensions that use this generator get none. */
public class MapGenScatteredFeature extends MapGenStructure {
    public MapGenScatteredFeature() {
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
