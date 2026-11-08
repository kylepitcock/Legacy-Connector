package legacyconnector.v1_20_1.net.minecraftforge.versions.forge;

import net.neoforged.fml.ModList;

/** Forge 1.20.1's version holder, answered with the running NeoForge's version (what mods compare against is the "forge" they were built for: the major follows NeoForge's). */
public final class ForgeVersion {
    public static final String MOD_ID = "forge";

    private ForgeVersion() {
    }

    public static String getVersion() {
        return ModList.get().getModContainerById("neoforge").map(c -> c.getModInfo().getVersion().toString()).orElse("21.1.0");
    }

    public static int getMajor() {
        return Integer.parseInt(getVersion().split("\\.")[0]);
    }

    public static String getSpec() {
        return getVersion();
    }
}
