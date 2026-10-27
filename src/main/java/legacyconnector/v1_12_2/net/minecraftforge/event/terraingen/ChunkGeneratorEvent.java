package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** Forge 1.12.2 chunk generator events, posted by legacy chunk generators on the terrain bus. */
public class ChunkGeneratorEvent extends Event {
    private final legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen;

    public ChunkGeneratorEvent(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen) {
        this.gen = gen;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator getGenerator() {
        return gen;
    }

    @HasResult
    public static class ReplaceBiomeBlocks extends ChunkGeneratorEvent {
        private final int x;
        private final int z;
        private final legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer primer;
        private final World world;

        public ReplaceBiomeBlocks(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, int x, int z,
                legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer primer, World world) {
            super(gen);
            this.x = x;
            this.z = z;
            this.primer = primer;
            this.world = world;
        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }

        public legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer getPrimer() {
            return primer;
        }

        public World getWorld() {
            return world;
        }
    }

    @HasResult
    public static class InitNoiseField extends ChunkGeneratorEvent {
        private double[] noisefield;
        private final int posX, posY, posZ, sizeX, sizeY, sizeZ;

        public InitNoiseField(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, double[] noisefield, int posX, int posY, int posZ,
                int sizeX, int sizeY, int sizeZ) {
            super(gen);
            this.noisefield = noisefield;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }

        public double[] getNoisefield() {
            return noisefield;
        }

        public void setNoisefield(double[] noisefield) {
            this.noisefield = noisefield;
        }

        public int getPosX() {
            return posX;
        }

        public int getPosY() {
            return posY;
        }

        public int getPosZ() {
            return posZ;
        }

        public int getSizeX() {
            return sizeX;
        }

        public int getSizeY() {
            return sizeY;
        }

        public int getSizeZ() {
            return sizeZ;
        }
    }
}
