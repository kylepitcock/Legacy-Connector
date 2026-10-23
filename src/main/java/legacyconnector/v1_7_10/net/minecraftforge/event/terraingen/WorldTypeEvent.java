package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.GenLayer;

/** Fired on the terrain gen bus while a world type builds its biome layers. */
public class WorldTypeEvent extends Event {
    public final WorldType worldType;

    public WorldTypeEvent(WorldType worldType) {
        this.worldType = worldType;
    }

    /** Number of zoom passes the biome map gets (4, LARGE_BIOMES 6); handlers set {@code newSize}. */
    public static class BiomeSize extends WorldTypeEvent {
        public final byte originalSize;
        public byte newSize;

        public BiomeSize(WorldType worldType, byte original) {
            super(worldType);
            this.originalSize = original;
            this.newSize = original;
        }
    }

    public static class InitBiomeGens extends WorldTypeEvent {
        public final long seed;
        public final GenLayer[] originalBiomeGens;
        public GenLayer[] newBiomeGens;

        public InitBiomeGens(WorldType worldType, long seed, GenLayer[] original) {
            super(worldType);
            this.seed = seed;
            this.originalBiomeGens = original;
            this.newBiomeGens = original.clone();
        }
    }
}
