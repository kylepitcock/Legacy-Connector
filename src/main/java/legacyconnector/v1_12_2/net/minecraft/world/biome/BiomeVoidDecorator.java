package legacyconnector.v1_12_2.net.minecraft.world.biome;

/** 1.12.2 decorator of the void biome: decorates nothing (no trees, grass, ores or lakes). */
public class BiomeVoidDecorator extends BiomeDecorator {
    public BiomeVoidDecorator() {
        super();
    }

    @Override
    public void decorate(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random, Biome biome,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        // the void has nothing to place
    }
}
