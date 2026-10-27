package legacyconnector.v1_12_2.net.minecraft.world;

/** Grass tint by climate, from the modern grass colour map (loaded on the client; the temperate tint elsewhere). */
public class ColorizerGrass {
    public static void setGrassBiomeColorizer(int[] buffer) {
        net.minecraft.world.level.GrassColor.init(buffer);
    }

    public static int getGrassColor(double temperature, double humidity) {
        int c = net.minecraft.world.level.GrassColor.get(temperature, humidity);
        return c == -65281 ? 0x91BD59 : c;
    }
}
