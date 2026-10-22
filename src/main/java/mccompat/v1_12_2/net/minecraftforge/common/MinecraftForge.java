package mccompat.v1_12_2.net.minecraftforge.common;

import mccompat.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus;

public class MinecraftForge {
    /** Gameplay events (entities, blocks, worlds, players). */
    public static final EventBus EVENT_BUS = new EventBus("1.12.2-forge");
    public static final EventBus TERRAIN_GEN_BUS = new EventBus("1.12.2-terrain");
    public static final EventBus ORE_GEN_BUS = new EventBus("1.12.2-oregen");
    public static final String MC_VERSION = "1.12.2";

    public static void initialize() {
    }

    public static String getBrandingVersion() {
        return "Minecraft Forge 10.13.4.1614 (MC Compatibility)";
    }
}
