package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** Forge 1.12.2 world type events (terrain bus): the biome zoom size and the biome layer stack of a world type. */
public class WorldTypeEvent extends Event {
    private final legacyconnector.v1_12_2.net.minecraft.world.WorldType worldType;

    public WorldTypeEvent(legacyconnector.v1_12_2.net.minecraft.world.WorldType worldType) {
        this.worldType = worldType;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.WorldType getWorldType() {
        return worldType;
    }

    public static class BiomeSize extends WorldTypeEvent {
        private final int originalSize;
        private int newSize;

        public BiomeSize(legacyconnector.v1_12_2.net.minecraft.world.WorldType worldType, int original) {
            super(worldType);
            this.originalSize = original;
            this.newSize = original;
        }

        public int getOriginalSize() {
            return originalSize;
        }

        public int getNewSize() {
            return newSize;
        }

        public void setNewSize(int newSize) {
            this.newSize = newSize;
        }
    }

    public static class InitBiomeGens extends WorldTypeEvent {
        private final long seed;
        private final legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer[] originalBiomeGens;
        private legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer[] newBiomeGens;

        public InitBiomeGens(legacyconnector.v1_12_2.net.minecraft.world.WorldType worldType, long seed,
                legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer[] original) {
            super(worldType);
            this.seed = seed;
            this.originalBiomeGens = original;
            this.newBiomeGens = original == null ? null : original.clone();
        }

        public long getSeed() {
            return seed;
        }

        public legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer[] getOriginalBiomeGens() {
            return originalBiomeGens;
        }

        public legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer[] getNewBiomeGens() {
            return newBiomeGens;
        }

        public void setNewBiomeGens(legacyconnector.v1_12_2.net.minecraft.world.gen.layer.GenLayer[] newBiomeGens) {
            this.newBiomeGens = newBiomeGens;
        }
    }
}
