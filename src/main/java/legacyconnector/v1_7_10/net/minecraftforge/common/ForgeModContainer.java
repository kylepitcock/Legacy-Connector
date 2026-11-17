package legacyconnector.v1_7_10.net.minecraftforge.common;

/** Forge's own settings that mods read. */
public class ForgeModContainer {
    public static int[] blendRanges = {1, 2, 3, 4, 5, 6, 7, 8};
    public static float zombieBabyChance = 0.05F;
    /** Chance a zombie summons help (Forge default 0.1; ZombieEvent.SummonAidEvent). */
    public static double zombieSummonBaseChance = 0.1D;
    /** Number of orbs the experience-orb clumping merges at most (Forge default 40). */
    public static int clumpingThreshold = 40;

    private static legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration mcc$config;

    /** Forge's own configuration (forge.cfg); held in memory, its settings are the fields above. */
    public static synchronized legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration getConfig() {
        if (mcc$config == null) mcc$config = new legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration();
        return mcc$config;
    }
    public static boolean removeErroringEntities = false;
    public static boolean removeErroringTileEntities = false;
    public static boolean fullBoundingBoxLadders = false;
    /** Forge config "sortRecipies": sort shaped recipes before shapeless ones in the recipe list. */
    public static boolean shouldSortRecipies = true;
}
