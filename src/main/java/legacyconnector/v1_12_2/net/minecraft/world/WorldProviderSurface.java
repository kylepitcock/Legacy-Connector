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

    private legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator mcc$generator;

    /**
     * The vanilla generator of this dimension (the modern game generates the terrain; the object stands in for mods that read the
     * provider's generator, e.g. to name or classify it). One per provider, since building one seeds all its noise.
     */
    @Override
    public legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator createChunkGenerator() {
        if (mcc$generator == null && world != null && org.kylepitcock.legacyconnector.v1_12_2.bridge.LegacyWorldPresets1122.hasOwnGenerator(terrainType)) {
            // a world created with a mod's world type that has its own generator: the type's (1.12: WorldProviderSurface asked terrainType)
            mcc$generator = terrainType.getChunkGenerator(world, generatorSettings);
        }
        if (mcc$generator == null && world != null) {
            var info = world.getWorldInfo();
            mcc$generator = new legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkGeneratorOverworld(world, world.getSeed(),
                    info.isMapFeaturesEnabled(), info.getGeneratorOptions());
        }
        return mcc$generator;
    }
}
