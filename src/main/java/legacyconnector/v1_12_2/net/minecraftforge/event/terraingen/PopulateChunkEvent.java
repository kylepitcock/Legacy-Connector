package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/**
 * Forge 1.12.2 chunk population events: Pre and Post around a chunk's population (main bus; posted for every overworld, Nether and
 * End chunk, see {@code TerrainEvents1122}), Populate before each kind of feature (terrain bus; DENY skips it).
 */
public class PopulateChunkEvent extends ChunkGeneratorEvent {
    private final World world;
    private final Random rand;
    private final int chunkX;
    private final int chunkZ;
    private final boolean hasVillageGenerated;

    /** The chunk generator populating (Forge's name for {@link #getGenerator}). */
    public legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator getGen() {
        return getGenerator();
    }

    public PopulateChunkEvent(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, World world, Random rand, int chunkX, int chunkZ,
            boolean hasVillageGenerated) {
        super(gen);
        this.world = world;
        this.rand = rand;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.hasVillageGenerated = hasVillageGenerated;
    }

    public World getWorld() {
        return world;
    }

    public Random getRand() {
        return rand;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public boolean isHasVillageGenerated() {
        return hasVillageGenerated;
    }

    public static class Pre extends PopulateChunkEvent {
        public Pre(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, World world, Random rand, int chunkX, int chunkZ, boolean hasVillageGenerated) {
            super(gen, world, rand, chunkX, chunkZ, hasVillageGenerated);
        }
    }

    public static class Post extends PopulateChunkEvent {
        public Post(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, World world, Random rand, int chunkX, int chunkZ, boolean hasVillageGenerated) {
            super(gen, world, rand, chunkX, chunkZ, hasVillageGenerated);
        }
    }

    @HasResult
    public static class Populate extends PopulateChunkEvent {
        private final EventType type;

        public Populate(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, World world, Random rand, int chunkX, int chunkZ,
                boolean hasVillageGenerated, EventType type) {
            super(gen, world, rand, chunkX, chunkZ, hasVillageGenerated);
            this.type = type;
        }

        public EventType getType() {
            return type;
        }

        public enum EventType {
            DUNGEON, FIRE, GLOWSTONE, ICE, LAKE, LAVA, NETHER_LAVA, NETHER_LAVA2, NETHER_MAGMA, ANIMALS, CUSTOM
        }
    }
}
