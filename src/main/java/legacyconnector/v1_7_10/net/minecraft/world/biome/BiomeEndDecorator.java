package legacyconnector.v1_7_10.net.minecraft.world.biome;

import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenSpikes;

/** The End's decorator: obsidian spikes (the dragon is the dimension's own). Mods' End biomes subclass it. */
public class BiomeEndDecorator extends BiomeDecorator {
    protected WorldGenerator spikeGen = new WorldGenSpikes(Blocks.end_stone);

    public BiomeEndDecorator() {
        this.treesPerChunk = 0;
        this.flowersPerChunk = 0;
        this.grassPerChunk = 0;
    }

    @Override
    protected void genDecorations(BiomeGenBase biome) {
        generateOres();
        if (randomGenerator.nextInt(5) == 0) {
            int x = chunk_X + randomGenerator.nextInt(16) + 8;
            int z = chunk_Z + randomGenerator.nextInt(16) + 8;
            int y = currentWorld.getTopSolidOrLiquidBlock(x, z);
            spikeGen.generate(currentWorld, randomGenerator, x, y, z);
        }
    }
}
