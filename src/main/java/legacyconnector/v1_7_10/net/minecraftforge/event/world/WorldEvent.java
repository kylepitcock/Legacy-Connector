package legacyconnector.v1_7_10.net.minecraftforge.event.world;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.world.World;

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

    /** The mobs that may spawn at a position: handlers add or remove {@code SpawnListEntry}s of {@link #list}, or cancel. */
    @legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
    public static class PotentialSpawns extends WorldEvent {
        public final legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type;
        public final int x;
        public final int y;
        public final int z;
        public final java.util.List<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry> list;

        public PotentialSpawns(World world, legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type, int x, int y, int z,
                java.util.List<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry> oldList) {
            super(world);
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.list = oldList != null ? new java.util.ArrayList<>(oldList) : new java.util.ArrayList<>();
        }
    }
}
