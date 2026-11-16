package legacyconnector.vb1_7_3.net.minecraft.client.color.world;

/** Beta 1.7.3 {@code ColorizerWater}: only holds the water colormap the game loaded. */
public class WaterColors {
    public static int[] colorMap = new int[65536];

    public static void setColorMap(int[] map) { colorMap = map; }
}
