package legacyconnector.v1_7_10.net.minecraft.world.biome;

/** The river: no land animals (mods extend it for their own waterways). */
public class BiomeGenRiver extends BiomeGenBase {
    public BiomeGenRiver(int id) {
        super(id);
        this.spawnableCreatureList.clear();
    }
}
