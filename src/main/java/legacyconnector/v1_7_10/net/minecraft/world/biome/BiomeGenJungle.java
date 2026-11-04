package legacyconnector.v1_7_10.net.minecraft.world.biome;

/** The jungle (and its edge): warm and wet, mods extend it for their own rainforests. */
public class BiomeGenJungle extends BiomeGenBase {
    private final boolean field_150614_aC;

    public BiomeGenJungle(int id, boolean edge) {
        super(id);
        this.field_150614_aC = edge;
        this.theBiomeDecorator.grassPerChunk = 25;
        this.theBiomeDecorator.flowersPerChunk = 4;
        if (!edge) this.theBiomeDecorator.treesPerChunk = 50;
        else this.theBiomeDecorator.treesPerChunk = 2;
    }
}
