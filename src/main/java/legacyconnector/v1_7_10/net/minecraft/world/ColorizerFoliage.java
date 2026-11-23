package legacyconnector.v1_7_10.net.minecraft.world;

/** Foliage tint by climate. The modern colour maps are per biome, so this returns the temperate tints. */
public class ColorizerFoliage {
    private static int[] foliageBuffer;

    public static void setFoliageBiomeColorizer(int[] buffer) {
        foliageBuffer = buffer != null && buffer.length >= 65536 ? buffer : null;
    }

    public static int getFoliageColor(double temperature, double humidity) {
        int[] map = foliageBuffer;
        if (map == null) return 0x48B518;
        temperature = Math.max(0.0, Math.min(1.0, temperature));
        humidity = Math.max(0.0, Math.min(1.0, humidity)) * temperature;
        int x = (int) ((1.0 - temperature) * 255.0);
        int y = (int) ((1.0 - humidity) * 255.0);
        return map[y << 8 | x];
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
