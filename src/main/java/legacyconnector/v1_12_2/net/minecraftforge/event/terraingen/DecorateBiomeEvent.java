package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/**
 * Forge 1.12.2 biome decoration events: Pre and Post around a chunk's decoration (main bus), Decorate before each kind of decoration
 * (terrain bus; a DENY result skips it).
 */
public class DecorateBiomeEvent extends Event {
    private final World world;
    private final Random rand;
    private final ChunkPos chunkPos;

    public DecorateBiomeEvent(World world, Random rand, ChunkPos chunkPos) {
        this.world = world;
        this.rand = rand;
        this.chunkPos = chunkPos;
    }

    @Deprecated
    public DecorateBiomeEvent(World world, Random rand, BlockPos pos) {
        this(world, rand, new ChunkPos(pos));
    }

    public World getWorld() {
        return world;
    }

    public Random getRand() {
        return rand;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    /** The decorated chunk's corner block (1.12's {@code pos}). */
    public BlockPos getPos() {
        return chunkPos.getBlock(0, 0, 0);
    }

    public static class Pre extends DecorateBiomeEvent {
        public Pre(World world, Random rand, ChunkPos chunkPos) {
            super(world, rand, chunkPos);
        }

        @Deprecated
        public Pre(World world, Random rand, BlockPos pos) {
            super(world, rand, pos);
        }
    }

    public static class Post extends DecorateBiomeEvent {
        public Post(World world, Random rand, ChunkPos chunkPos) {
            super(world, rand, chunkPos);
        }

        @Deprecated
        public Post(World world, Random rand, BlockPos pos) {
            super(world, rand, pos);
        }
    }

    @HasResult
    public static class Decorate extends DecorateBiomeEvent {
        private final EventType type;
        private final BlockPos placementPos;

        public Decorate(World world, Random rand, ChunkPos chunkPos, BlockPos placementPos, EventType type) {
            super(world, rand, chunkPos);
            this.type = type;
            this.placementPos = placementPos;
        }

        @Deprecated
        public Decorate(World world, Random rand, BlockPos pos, EventType type) {
            this(world, rand, new ChunkPos(pos), null, type);
        }

        public EventType getType() {
            return type;
        }

        /** Where this decoration goes, or null when the decorator has not picked a position. */
        public BlockPos getPlacementPos() {
            return placementPos;
        }

        public enum EventType {
            BIG_SHROOM, CACTUS, CLAY, DEAD_BUSH, DESERT_WELL, LILYPAD, FLOWERS, FOSSIL, GRASS, ICE, LAKE_WATER, LAKE_LAVA, PUMPKIN, REED, ROCK, SAND,
            SAND_PASS2, SHROOM, TREE, CUSTOM
        }
    }
}
