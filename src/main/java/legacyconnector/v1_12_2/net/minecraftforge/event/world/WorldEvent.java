package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.WorldSettings;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Forge's world events; Load, Unload and Save are posted when the modern level is loaded, unloaded and saved. */
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

    @Cancelable
    public static class PotentialSpawns extends WorldEvent {
        private final EnumCreatureType type;
        private final BlockPos pos;
        private final List<Biome.SpawnListEntry> list;

        public PotentialSpawns(World world, EnumCreatureType type, BlockPos pos, List<Biome.SpawnListEntry> oldList) {
            super(world);
            this.pos = pos;
            this.type = type;
            this.list = oldList != null ? new ArrayList<>(oldList) : new ArrayList<>();
        }

        public EnumCreatureType getType() {
            return type;
        }

        public BlockPos getPos() {
            return pos;
        }

        public List<Biome.SpawnListEntry> getList() {
            return list;
        }
    }

    @Cancelable
    public static class CreateSpawnPosition extends WorldEvent {
        private final WorldSettings settings;

        public CreateSpawnPosition(World world, WorldSettings settings) {
            super(world);
            this.settings = settings;
        }

        public WorldSettings getSettings() {
            return settings;
        }
    }
}
