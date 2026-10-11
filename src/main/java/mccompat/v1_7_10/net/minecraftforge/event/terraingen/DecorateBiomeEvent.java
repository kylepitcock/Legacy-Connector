package mccompat.v1_7_10.net.minecraftforge.event.terraingen;

import java.util.Random;
import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Fired around and during biome decoration (Pre/Post on the main bus, Decorate on the terrain gen bus). */
public class DecorateBiomeEvent extends Event {
    public final World world;
    public final Random rand;
    public final int chunkX;
    public final int chunkZ;

    public DecorateBiomeEvent(World world, Random rand, int worldX, int worldZ) {
        this.world = world;
        this.rand = rand;
        this.chunkX = worldX;
        this.chunkZ = worldZ;
    }

    public static class Pre extends DecorateBiomeEvent {
        public Pre(World world, Random rand, int worldX, int worldZ) {
            super(world, rand, worldX, worldZ);
        }
    }

    public static class Post extends DecorateBiomeEvent {
        public Post(World world, Random rand, int worldX, int worldZ) {
            super(world, rand, worldX, worldZ);
        }
    }

    /** One decoration pass; DENY skips it. */
    @HasResult
    public static class Decorate extends DecorateBiomeEvent {
        public enum EventType {
            BIG_SHROOM, CACTUS, CLAY, DEAD_BUSH, LILYPAD, FLOWERS, GRASS, LAKE, PUMPKIN, REED, SAND, SAND_PASS2, SHROOM, TREE, CUSTOM
        }

        public final EventType type;

        public Decorate(World world, Random rand, int worldX, int worldZ, EventType type) {
            super(world, rand, worldX, worldZ);
            this.type = type;
        }
    }
}
