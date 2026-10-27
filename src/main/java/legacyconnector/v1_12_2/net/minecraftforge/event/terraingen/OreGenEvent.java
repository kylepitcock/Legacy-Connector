package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** Forge 1.12.2 ore generation events (ore bus): Pre and Post around a chunk's ores, GenerateMinable before each ore (DENY skips it). */
public class OreGenEvent extends Event {
    private final World world;
    private final Random rand;
    private final BlockPos pos;

    public OreGenEvent(World world, Random rand, BlockPos pos) {
        this.world = world;
        this.rand = rand;
        this.pos = pos;
    }

    public World getWorld() {
        return world;
    }

    public Random getRand() {
        return rand;
    }

    public BlockPos getPos() {
        return pos;
    }

    public static class Pre extends OreGenEvent {
        public Pre(World world, Random rand, BlockPos pos) {
            super(world, rand, pos);
        }
    }

    public static class Post extends OreGenEvent {
        public Post(World world, Random rand, BlockPos pos) {
            super(world, rand, pos);
        }
    }

    @HasResult
    public static class GenerateMinable extends OreGenEvent {
        private final EventType type;
        private final legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator generator;

        public GenerateMinable(World world, Random rand, legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator generator, BlockPos pos,
                EventType type) {
            super(world, rand, pos);
            this.generator = generator;
            this.type = type;
        }

        public EventType getType() {
            return type;
        }

        public legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator getGenerator() {
            return generator;
        }

        public enum EventType {
            COAL, DIAMOND, DIRT, GOLD, GRAVEL, IRON, LAPIS, REDSTONE, QUARTZ, DIORITE, GRANITE, ANDESITE, EMERALD, SILVERFISH, CUSTOM
        }
    }
}
