package legacyconnector.v1_6_4.facade.net.minecraft.world.biome;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.block.Block;

/** Facade of the 1.7.10 shim's {@code BiomeGenBase} for 1.6.4 mods: the surface blocks were block id bytes. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class BiomeGenBase {
    private BiomeGenBase() {}

    public static int getBiomeGrassColor(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self) {
        return self.getBiomeGrassColor(0, 64, 0);
    }

    /** {@code getFloatTemperature()}: the biome's temperature at sea level (1.7.10 takes the position, for the cooling above y 64). */
    public static float getFloatTemperature(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self) {
        return self.temperature;
    }

    public static int getBiomeFoliageColor(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self) {
        return self.getBiomeFoliageColor(0, 64, 0);
    }

    /** {@code setMinMaxHeight(min, max)}: 1.6.4's {@code minHeight}/{@code maxHeight} are 1.7.10's {@code rootHeight}/{@code heightVariation}, stored as given. */
    public static legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase setMinMaxHeight(
            legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self, float min, float max) {
        self.rootHeight = min;
        self.heightVariation = max;
        return self;
    }

    private static Block blockOf(byte id, Block fallback) {
        Block b = Ids.block(id & 0xFF);
        return b != null ? b : fallback;
    }

    public static byte mcc$get$topBlock(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self) {
        return (byte) Ids.blockId(self.topBlock);
    }

    public static void mcc$set$topBlock(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self, byte id) {
        self.topBlock = blockOf(id, legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass);
    }

    public static byte mcc$get$fillerBlock(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self) {
        return (byte) Ids.blockId(self.fillerBlock);
    }

    public static void mcc$set$fillerBlock(legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase self, byte id) {
        self.fillerBlock = blockOf(id, legacyconnector.v1_7_10.net.minecraft.init.Blocks.dirt);
    }
}
