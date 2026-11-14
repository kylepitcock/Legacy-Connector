package legacyconnector.v1_6_4.cpw.mods.fml.relauncher;

/**
 * FML 1.5.2/1.6.4's launch entry. Mods only ask which side they run on ({@code FMLRelauncher.side()}: "CLIENT" or "SERVER"; Immibis Core
 * decides its proxy by it).
 */
public final class FMLRelauncher {
    private FMLRelauncher() {}

    public static String side() {
        return isClient() ? "CLIENT" : "SERVER";
    }

    public static boolean isClient() {
        // No game side yet in the offline coremod probe: the side the coremod phase was started for
        var dist = net.neoforged.fml.loading.FMLEnvironment.dist;
        return dist != null ? dist.isClient() : !"SERVER".equals(System.getProperty("legacyconnector.side"));
    }
}
