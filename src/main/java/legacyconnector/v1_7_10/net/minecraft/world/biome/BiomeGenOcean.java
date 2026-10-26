package legacyconnector.v1_7_10.net.minecraft.world.biome;

/** The ocean: no land animals, and the ocean temperature category (mods extend it for their own seas). */
public class BiomeGenOcean extends BiomeGenBase {
    public BiomeGenOcean(int id) {
        super(id);
        this.spawnableCreatureList.clear();
    }

    @Override
    public TempCategory getTempCategory() {
        return TempCategory.OCEAN;
    }
}
