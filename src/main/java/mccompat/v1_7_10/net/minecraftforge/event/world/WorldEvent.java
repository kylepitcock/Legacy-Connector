package mccompat.v1_7_10.net.minecraftforge.event.world;

import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import mccompat.v1_7_10.net.minecraft.world.World;

public class WorldEvent extends Event {
    public final World world;

    public WorldEvent(World world) {
        this.world = world;
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
