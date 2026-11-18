package legacyconnector.v1_7_10.net.minecraft.world.biome;

/**
 * The forests (1.7.10 {@code BiomeGenForest}): type 0 the forest, 1 the flower forest, 2 the birch forest, 3 the roofed forest (no ordinary trees);
 * mods extend it for their own woods.
 */
public class BiomeGenForest extends BiomeGenBase {
    private final int field_150632_aF;

    public BiomeGenForest(int id, int type) {
        super(id);
        this.field_150632_aF = type;
        this.theBiomeDecorator.treesPerChunk = 10;
        this.theBiomeDecorator.grassPerChunk = 2;
        if (type == 1) {
            this.theBiomeDecorator.treesPerChunk = 6;
            this.theBiomeDecorator.flowersPerChunk = 100;
            this.theBiomeDecorator.grassPerChunk = 1;
        }
        setTemperatureRainfall(0.7F, 0.8F);
        if (type == 2) setTemperatureRainfall(0.6F, 0.6F);
        if (type == 0) this.spawnableCreatureList.add(new SpawnListEntry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityWolf.class, 5, 4, 4));
        if (type == 3) this.theBiomeDecorator.treesPerChunk = -999;
    }
}
