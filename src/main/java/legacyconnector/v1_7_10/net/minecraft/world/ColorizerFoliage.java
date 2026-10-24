package legacyconnector.v1_7_10.net.minecraft.world;

/** Foliage tint by climate. The modern colour maps are per biome, so this returns the temperate tints. */
public class ColorizerFoliage {
    public static int getFoliageColor(double temperature, double humidity) {
        return 0x48B518;
    }

    public static int getFoliageColorPine() {
        return 0x619961;
    }

    public static int getFoliageColorBirch() {
        return 0x80A755;
    }

    public static int getFoliageColorBasic() {
        return 0x48B518;
    }
}
