package legacyconnector.v1_12_2.net.minecraft.world.biome;

/** 1.12.2 snowy biome (ice plains, ice spikes when {@code superIcy}). */
public class BiomeSnow extends Biome {
    private final boolean superIcy;

    public BiomeSnow(boolean superIcyIn, BiomeProperties properties) {
        super(properties);
        this.superIcy = superIcyIn;
    }

    public boolean mcc$isSuperIcy() {
        return superIcy;
    }
}
