package legacyconnector.v1_12_2.net.minecraft.world.biome;

/** 1.12.2 vanilla biome class mods extend (its terrain and decoration are the modern game's in modern worlds). */
public class BiomeHell extends Biome {
    public BiomeHell(BiomeProperties properties) {
        super(properties);
    }

    /** The nether's biome decoration is the modern game's. */
    @Override
    public void decorate(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random rand, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
    }
}
