package legacyconnector.v1_6_4.facade.net.minecraftforge.common;

/**
 * Facade of the 1.7.10 shim's {@code MinecraftForge} for 1.6.4 mods: harvest levels were set from the outside
 * ({@code setBlockHarvestLevel(block, toolClass, level)}); 1.7.10 puts them on the block. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class MinecraftForge {
    private MinecraftForge() {}

    public static void setBlockHarvestLevel(legacyconnector.v1_7_10.net.minecraft.block.Block block, String toolClass, int level) {
        if (block != null) block.setHarvestLevel(toolClass, level);
    }

    public static void setBlockHarvestLevel(legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata, String toolClass, int level) {
        if (block != null) block.setHarvestLevel(toolClass, level, metadata);
    }
}
