package legacyconnector.v1_7_10.net.minecraft.world;

/** The overworld's provider; mods extend it for dimensions that look like the overworld (sky, day cycle, weather). */
public class WorldProviderSurface extends WorldProvider {
    public WorldProviderSurface() {
    }

    @Override
    public String getDimensionName() {
        return "Overworld";
    }
}
