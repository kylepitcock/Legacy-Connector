package legacyconnector.v1_12_2.net.minecraft.world;

/** Foliage tint by climate, from the modern foliage colour map (loaded on the client; the basic tint elsewhere). */
public class ColorizerFoliage {
    public static void setFoliageBiomeColorizer(int[] buffer) {
        net.minecraft.world.level.FoliageColor.init(buffer);
    }

    public static int getFoliageColor(double temperature, double humidity) {
        int c = net.minecraft.world.level.FoliageColor.get(temperature, humidity);
        return c == -65281 ? getFoliageColorBasic() : c;
    }

    public static int getFoliageColorPine() {
        return 6396257;
    }

    public static int getFoliageColorBirch() {
        return 8431445;
    }

    public static int getFoliageColorBasic() {
        return 4764952;
    }
}
