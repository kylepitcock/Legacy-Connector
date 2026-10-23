package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/** Fired by chunk providers around populating a chunk (Pre/Post) and for single features (Populate). */
public class PopulateChunkEvent extends Event {
    public final IChunkProvider chunkProvider;
    public final World world;
    public final Random rand;
    public final int chunkX;
    public final int chunkZ;
    public final boolean hasVillageGenerated;

    public PopulateChunkEvent(IChunkProvider chunkProvider, World world, Random rand, int chunkX, int chunkZ, boolean hasVillageGenerated) {
        this.chunkProvider = chunkProvider;
        this.world = world;
        this.rand = rand;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.hasVillageGenerated = hasVillageGenerated;
    }

    public static class Pre extends PopulateChunkEvent {
        public Pre(IChunkProvider chunkProvider, World world, Random rand, int chunkX, int chunkZ, boolean hasVillageGenerated) {
            super(chunkProvider, world, rand, chunkX, chunkZ, hasVillageGenerated);
        }
    }

    public static class Post extends PopulateChunkEvent {
        public Post(IChunkProvider chunkProvider, World world, Random rand, int chunkX, int chunkZ, boolean hasVillageGenerated) {
            super(chunkProvider, world, rand, chunkX, chunkZ, hasVillageGenerated);
        }
    }

    @HasResult
    public static class Populate extends PopulateChunkEvent {
        public enum EventType {
            DUNGEON, FIRE, GLOWSTONE, ICE, LAKE, LAVA, NETHER_LAVA, NETHER_LAVA2, ANIMALS, CUSTOM
        }

        public final EventType type;

        public Populate(IChunkProvider chunkProvider, World world, Random rand, int chunkX, int chunkZ, boolean hasVillageGenerated, EventType type) {
            super(chunkProvider, world, rand, chunkX, chunkZ, hasVillageGenerated);
            this.type = type;
        }
    }
}
