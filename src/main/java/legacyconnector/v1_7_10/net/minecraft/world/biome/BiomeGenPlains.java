package legacyconnector.v1_7_10.net.minecraft.world.biome;

/** The plains (1.7.10 {@code BiomeGenPlains}): low flat land with a lot of grass and flowers, no trees, horses among the animals (mods extend it for their own plains). */
public class BiomeGenPlains extends BiomeGenBase {
    public BiomeGenPlains(int id) {
        super(id);
        setTemperatureRainfall(0.8F, 0.4F);
        setHeight(height_LowPlains);
        this.spawnableCreatureList.add(new SpawnListEntry(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityHorse.class, 5, 2, 6));
        this.theBiomeDecorator.treesPerChunk = -999;
        this.theBiomeDecorator.flowersPerChunk = 4;
        this.theBiomeDecorator.grassPerChunk = 10;
    }
}
