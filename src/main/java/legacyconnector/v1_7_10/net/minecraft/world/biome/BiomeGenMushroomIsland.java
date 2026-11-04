package legacyconnector.v1_7_10.net.minecraft.world.biome;

/** The mushroom island: only mooshrooms, no monsters. */
public class BiomeGenMushroomIsland extends BiomeGenBase {
    public BiomeGenMushroomIsland(int id) {
        super(id);
        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.theBiomeDecorator.treesPerChunk = -100;
        this.theBiomeDecorator.flowersPerChunk = -100;
        this.theBiomeDecorator.grassPerChunk = -100;
    }
}
