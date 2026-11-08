package legacyconnector.v1_7_10.net.minecraftforge.common;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus;

public class MinecraftForge {
    /** Gameplay events (entities, blocks, worlds, players). */
    public static final EventBus EVENT_BUS = new EventBus("1.7.10-forge");
    public static final EventBus TERRAIN_GEN_BUS = new EventBus("1.7.10-terrain");
    public static final EventBus ORE_GEN_BUS = new EventBus("1.7.10-oregen");
    public static final String MC_VERSION = "1.7.10";

    /** Weighted seed drops from tall grass: rolled alongside the modern grass loot through the block drops event. */
    public static void addGrassSeed(legacyconnector.v1_7_10.net.minecraft.item.ItemStack seed, int weight) {
        ForgeHooks.seedList.add(new ForgeHooks.SeedEntry(seed, weight));
        org.kylepitcock.legacyconnector.v1_7_10.bridge.GrassSeeds1710.add(seed, weight);
    }

    /** A plant that grows on grass when it is fertilized, picked by weight among the entries ({@code ForgeHooks.plantGrass}). */
    public static void addGrassPlant(legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata, int weight) {
        ForgeHooks.grassList.add(new ForgeHooks.GrassEntry(block, metadata, weight));
    }

    /** 1.4.7 and 1.5: the tool class no longer mines the block (every metadata the class was set for). */
    public static void removeBlockEffectiveness(legacyconnector.v1_7_10.net.minecraft.block.Block block, String toolClass) {
        if (block == null || toolClass == null) return;
        for (int m = 0; m < 16; m++) {
            if (toolClass.equals(block.getHarvestTool(m))) block.setHarvestLevel(null, -1, m);
        }
    }

    public static void initialize() {
    }

    public static String getBrandingVersion() {
        return "Minecraft Forge 10.13.4.1614 (MC Compatibility)";
    }
}
