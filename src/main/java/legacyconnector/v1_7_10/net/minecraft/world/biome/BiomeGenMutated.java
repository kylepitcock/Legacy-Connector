package legacyconnector.v1_7_10.net.minecraft.world.biome;

/** A mutated variant of a biome (Sunflower Plains, Ice Spikes, ...): the base biome's settings under another id. */
public class BiomeGenMutated extends BiomeGenBase {
    public BiomeGenBase baseBiome;
    /** SRG name of {@link #baseBiome}. */
    public BiomeGenBase field_150611_aD;

    public BiomeGenMutated(int id, BiomeGenBase base) {
        super(id);
        this.baseBiome = base;
        this.field_150611_aD = base;
        this.topBlock = base.topBlock;
        this.fillerBlock = base.fillerBlock;
        this.biomeName = base.biomeName + " M";
        this.color = base.color;
        this.rootHeight = base.rootHeight;
        this.heightVariation = base.heightVariation;
        this.temperature = base.temperature;
        this.rainfall = base.rainfall;
        this.waterColorMultiplier = base.waterColorMultiplier;
        this.enableSnow = base.enableSnow;
        this.enableRain = base.enableRain;
        this.spawnableCreatureList.addAll(base.spawnableCreatureList);
        this.spawnableMonsterList.addAll(base.spawnableMonsterList);
        this.spawnableWaterCreatureList.addAll(base.spawnableWaterCreatureList);
    }
}
