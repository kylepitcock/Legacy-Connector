package legacyconnector.v1_12_2.net.minecraftforge.common;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus;

public class MinecraftForge {
    /** Gameplay events (entities, blocks, worlds, players). */
    public static final EventBus EVENT_BUS = new EventBus("1.12.2-forge");
    public static final EventBus TERRAIN_GEN_BUS = new EventBus("1.12.2-terrain");
    public static final EventBus ORE_GEN_BUS = new EventBus("1.12.2-oregen");
    public static final String MC_VERSION = "1.12.2";

    public static void initialize() {
    }

    /**
     * Adds a seed broken grass drops (ForgeHooks' grass seed list, where wheat seeds weigh 10): the modern game's short and tall
     * grass loot keeps its wheat seeds, and these are rolled alongside with their share of the weight.
     */
    public static void addGrassSeed(legacyconnector.v1_12_2.net.minecraft.item.ItemStack seed, int weight) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GrassSeeds1122.add(seed, weight);
    }

    public static String getBrandingVersion() {
        return "Minecraft Forge 14.23.5.2860";
    }
}
