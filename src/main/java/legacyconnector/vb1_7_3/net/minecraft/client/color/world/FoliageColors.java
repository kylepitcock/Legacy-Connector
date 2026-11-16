package legacyconnector.vb1_7_3.net.minecraft.client.color.world;

/** Beta 1.7.3 {@code ColorizerFoliage}: the fixed spruce, birch and default leaf colours and the climate lookup (the native foliage colormap). */
public class FoliageColors {
    public static int[] colorMap = new int[65536];

    public static void setColorMap(int[] map) { colorMap = map; }

    public static int getColor(double temperature, double downfall) {
        try { return net.minecraft.world.level.FoliageColor.get(temperature, downfall); }
        catch (RuntimeException e) { return getDefaultColor(); } // the colormap is loaded on the client only
    }

    public static int getSpruceColor() { return 0x619961; }

    public static int getBirchColor() { return 0x80A755; }

    public static int getDefaultColor() { return 0x48B518; }
}
