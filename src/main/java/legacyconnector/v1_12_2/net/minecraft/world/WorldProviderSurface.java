package legacyconnector.v1_12_2.net.minecraft.world;

/** The overworld (and any modern dimension without a legacy provider of its own). */
public class WorldProviderSurface extends WorldProvider {
    public WorldProviderSurface() {
    }

    @Override
    public DimensionType getDimensionType() {
        return DimensionType.OVERWORLD;
    }

    @Override
    public boolean canDropChunk(int x, int z) {
        return !world.isSpawnChunk(x, z) || !getDimensionType().shouldLoadSpawn();
    }
}
