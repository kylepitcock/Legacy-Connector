package legacyconnector.v1_12_2.net.minecraft.world.biome;

/** What a biome adds to a chunk when it is decorated: the counts mods tune per biome. */
public class BiomeDecorator {
    public boolean decorating;
    public int sandPerChunk = 1;
    public int sandPerChunk2 = 3;
    public int clayPerChunk = 1;
    public int treesPerChunk;
    public int flowersPerChunk = 2;
    public int grassPerChunk = 1;
    public int deadBushPerChunk;
    public int mushroomsPerChunk;
    public int reedsPerChunk;
    public int cactiPerChunk;
    public int gravelPatchesPerChunk = 1;
    public int sandPatchesPerChunk = 3;
    public int clayPatchesPerChunk = 1;
    public int bigMushroomsPerChunk;
    public boolean generateFalls = true;
    public int waterlilyPerChunk;

    public java.util.Random decorationRandom;
    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos chunkPos;

    public BiomeDecorator() {
    }

    /**
     * Decorates a chunk. Vanilla's ores, trees and plants are the modern game's features, so the base pass only records the chunk
     * (mod decorators extending this class add their own features in {@code genDecorations}).
     */
    public void decorate(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random, Biome biome,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (decorating) return;
        decorating = true;
        try {
            chunkPos = pos;
            decorationRandom = random;
            genDecorations(biome, worldIn, random);
        } finally {
            decorating = false;
        }
    }

    protected void genDecorations(Biome biomeIn, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.Random random) {
    }
}
