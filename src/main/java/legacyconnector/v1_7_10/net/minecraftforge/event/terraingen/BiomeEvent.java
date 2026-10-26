package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;

/** Events about what a biome is made of. */
public class BiomeEvent extends Event {
    public final BiomeGenBase biome;

    public BiomeEvent(BiomeGenBase biome) {
        this.biome = biome;
    }

    /** Which block a village piece uses in this biome: handlers set {@link #replacement}. */
    public static class GetVillageBlockID extends BiomeEvent {
        public final Block original;
        public final int type;
        public Block replacement;

        public GetVillageBlockID(BiomeGenBase biome, Block original, int type) {
            super(biome);
            this.original = original;
            this.type = type;
        }
    }

    public static class GetVillageBlockMeta extends BiomeEvent {
        public final Block original;
        public final int type;
        public int replacement;

        public GetVillageBlockMeta(BiomeGenBase biome, Block original, int type) {
            super(biome);
            this.original = original;
            this.type = type;
        }
    }

    /** A biome colour handlers may change: they set {@link #newColor}. */
    public static class BiomeColor extends BiomeEvent {
        public final int originalColor;
        public int newColor;

        public BiomeColor(BiomeGenBase biome, int original) {
            super(biome);
            this.originalColor = original;
            this.newColor = original;
        }
    }

    public static class GetGrassColor extends BiomeColor {
        public GetGrassColor(BiomeGenBase biome, int original) {
            super(biome, original);
        }
    }

    public static class GetFoliageColor extends BiomeColor {
        public GetFoliageColor(BiomeGenBase biome, int original) {
            super(biome, original);
        }
    }

    public static class GetWaterColor extends BiomeColor {
        public GetWaterColor(BiomeGenBase biome, int original) {
            super(biome, original);
        }
    }
}
