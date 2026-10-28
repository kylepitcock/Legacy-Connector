package legacyconnector.v1_12_2.net.minecraft.world.biome;

/** 1.12.2 vanilla biome class mods extend (its terrain and decoration are the modern game's in modern worlds). */
public class BiomeSwamp extends Biome {
    public BiomeSwamp(BiomeProperties properties) {
        super(properties);
    }

    protected static final legacyconnector.v1_12_2.net.minecraft.world.gen.NoiseGeneratorPerlin GRASS_COLOR_NOISE = Biome.GRASS_COLOR_NOISE;
}
