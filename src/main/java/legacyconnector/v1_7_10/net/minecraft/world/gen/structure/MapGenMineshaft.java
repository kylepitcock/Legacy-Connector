package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

/**
 * The abandoned mineshaft generator. The modern overworld already has mineshafts; for dimensions that create one this places none,
 * and subclasses can override the start check.
 */
public class MapGenMineshaft extends MapGenStructure {
    private double field_82673_e = 0.004D;

    public MapGenMineshaft() {
    }

    @Override
    public String getStructureName() {
        return "Mineshaft";
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
