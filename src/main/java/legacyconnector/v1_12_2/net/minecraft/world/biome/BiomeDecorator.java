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

    public BiomeDecorator() {
    }
}
