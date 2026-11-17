package legacyconnector.v1_12_2.net.minecraft.world.biome;

/** 1.12.2 void biome: no mob spawns of any kind, nothing decorated, no top or filler blocks. */
public class BiomeVoid extends Biome {
    public BiomeVoid(BiomeProperties properties) {
        super(properties);
        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
    }

    @Override
    public BiomeDecorator createBiomeDecorator() {
        return new BiomeVoidDecorator();
    }

    @Override
    public boolean ignorePlayerSpawnSuitability() {
        return true;
    }
}
