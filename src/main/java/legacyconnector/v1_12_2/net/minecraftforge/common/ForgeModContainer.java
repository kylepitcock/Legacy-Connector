package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.io.File;
import legacyconnector.v1_12_2.net.minecraftforge.common.config.Configuration;

/** Forge's own mod container: its configuration and the public settings mods read. */
public class ForgeModContainer {
    public static boolean fullBoundingBoxLadders = false;
    public static double zombieSummonBaseChance = 0.1D;
    public static boolean replaceVanillaBucketModel = true;
    public static boolean forgeLightPipelineEnabled = true;
    public static boolean selectiveResourceReloadEnabled = false;
    public static boolean disableStairSlabCulling = false;
    public static boolean alwaysSetupTerrainOffThread = false;
    public static int clumpingThreshold = 64;
    public static boolean removeErroringEntities = false;
    public static boolean removeErroringTileEntities = false;
    public static boolean fixVanillaCascading = false;
    public static boolean shouldSortRecipies = true;
    public static boolean disableVersionCheck = true;
    public static boolean logCascadingWorldGeneration = true;
    public static boolean zoomInMissingModelTextInGui = false;

    private static Configuration config;

    public static synchronized Configuration getConfig() {
        if (config == null) config = new Configuration(new File(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().toFile(), "forge.cfg"));
        return config;
    }
}
