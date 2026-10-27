package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Forge's world events: a world loads, unloads or saves. */
public class WorldEvent extends Event {
    private final World world;

    public WorldEvent(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

    public static class Load extends WorldEvent {
        public Load(World world) {
            super(world);
        }
    }

    public static class Unload extends WorldEvent {
        public Unload(World world) {
            super(world);
        }
    }

    public static class Save extends WorldEvent {
        public Save(World world) {
            super(world);
        }
    }
}
