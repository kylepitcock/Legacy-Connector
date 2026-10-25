package legacyconnector.v1_7_10.net.minecraft.world;

/** Grass tint by climate. The modern colour maps are per biome, so this returns the temperate tint. */
public class ColorizerGrass {
    public static int getGrassColor(double temperature, double humidity) {
        return 0x91BD59;
    }
}
