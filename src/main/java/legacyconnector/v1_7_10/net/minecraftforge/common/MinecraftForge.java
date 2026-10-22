package legacyconnector.v1_7_10.net.minecraftforge.common;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus;

public class MinecraftForge {
    /** Gameplay events (entities, blocks, worlds, players). */
    public static final EventBus EVENT_BUS = new EventBus("1.7.10-forge");
    public static final EventBus TERRAIN_GEN_BUS = new EventBus("1.7.10-terrain");
    public static final EventBus ORE_GEN_BUS = new EventBus("1.7.10-oregen");
    public static final String MC_VERSION = "1.7.10";

    public static void initialize() {
    }

    public static String getBrandingVersion() {
        return "Minecraft Forge 10.13.4.1614 (MC Compatibility)";
    }
}
