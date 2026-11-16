package legacyconnector.vb1_7_3.net.minecraft.client.color.world;

/** Beta 1.7.3 {@code ColorizerGrass}: grass colour by climate, from the native grass colormap. */
public class GrassColors {
    public static int[] colorMap = new int[65536];

    public static void setColorMap(int[] map) { colorMap = map; }

    public static int getColor(double temperature, double downfall) {
        try { return net.minecraft.world.level.GrassColor.get(temperature, downfall); }
        catch (RuntimeException e) { return 0x48B518; } // the colormap is loaded on the client only
    }
}
