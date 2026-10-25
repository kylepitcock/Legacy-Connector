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
}
