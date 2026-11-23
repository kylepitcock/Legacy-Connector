package legacyconnector.v1_7_10.net.minecraft.world;

/** Grass tint by climate. Without a colour map this returns the temperate tint; mods that replace the map get the vanilla lookup. */
public class ColorizerGrass {
    private static int[] grassBuffer;

    public static void setGrassBiomeColorizer(int[] buffer) {
        grassBuffer = buffer != null && buffer.length >= 65536 ? buffer : null;
    }

    public static int getGrassColor(double temperature, double humidity) {
        int[] map = grassBuffer;
        if (map == null) return 0x91BD59;
        temperature = Math.max(0.0, Math.min(1.0, temperature));
        humidity = Math.max(0.0, Math.min(1.0, humidity)) * temperature;
        int x = (int) ((1.0 - temperature) * 255.0);
        int y = (int) ((1.0 - humidity) * 255.0);
        return map[y << 8 | x];
    }
}
