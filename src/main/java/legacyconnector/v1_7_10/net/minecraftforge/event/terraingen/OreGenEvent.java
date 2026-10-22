package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator;

/** Ore generation events (ore gen bus). */
public class OreGenEvent extends Event {
    public final World world;
    public final Random rand;
    public final int worldX;
    public final int worldZ;

    public OreGenEvent(World world, Random rand, int worldX, int worldZ) {
        this.world = world;
        this.rand = rand;
        this.worldX = worldX;
        this.worldZ = worldZ;
    }

    public static class Pre extends OreGenEvent {
        public Pre(World world, Random rand, int worldX, int worldZ) {
            super(world, rand, worldX, worldZ);
        }
    }

    public static class Post extends OreGenEvent {
        public Post(World world, Random rand, int worldX, int worldZ) {
            super(world, rand, worldX, worldZ);
        }
    }

    /** One ore pass; DENY skips it. */
    @HasResult
    public static class GenerateMinable extends OreGenEvent {
        public enum EventType {
            COAL, DIAMOND, DIRT, GOLD, GRAVEL, IRON, LAPIS, REDSTONE, QUARTZ, CUSTOM
        }

        public final EventType type;
        public final WorldGenerator generator;

        public GenerateMinable(World world, Random rand, WorldGenerator generator, int worldX, int worldZ, EventType type) {
            super(world, rand, worldX, worldZ);
            this.generator = generator;
            this.type = type;
        }
    }
}
