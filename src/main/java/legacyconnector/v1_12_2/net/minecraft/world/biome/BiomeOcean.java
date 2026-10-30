package legacyconnector.v1_12_2.net.minecraft.world.biome;

/** 1.12.2 ocean: no land animals, the ocean temperature category. */
public class BiomeOcean extends Biome {
    public BiomeOcean(BiomeProperties properties) {
        super(properties);
        spawnableCreatureList.clear();
    }

    @Override
    public TempCategory getTempCategory() {
        return TempCategory.OCEAN;
    }
}
