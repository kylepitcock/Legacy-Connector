package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/** Events a terrain generator posts while it builds a chunk, so other mods can change the noise or the surface blocks. */
public class ChunkProviderEvent extends Event {
    public final IChunkProvider chunkProvider;

    public ChunkProviderEvent(IChunkProvider chunkProvider) {
        this.chunkProvider = chunkProvider;
    }

    /** The noise field the terrain is shaped from; a handler may replace {@link #noisefield}. */
    public static class InitNoiseField extends ChunkProviderEvent {
        public double[] noisefield;
        public final int posX;
        public final int posY;
        public final int posZ;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;

        public InitNoiseField(IChunkProvider provider, double[] noisefield, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ) {
            super(provider);
            this.noisefield = noisefield;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }
    }

    /** Fired before the biome's surface blocks (grass, dirt, sand) replace the stone near the top. */
    public static class ReplaceBiomeBlocks extends ChunkProviderEvent {
        public final int chunkX;
        public final int chunkZ;
        public final Block[] blockArray;
        public final byte[] metaArray;
        public final BiomeGenBase[] biomeArray;
        public final World world;

        /** The 1.7.2 form, without metadata or world (the nether generator posts this one). */
        public ReplaceBiomeBlocks(IChunkProvider provider, int chunkX, int chunkZ, Block[] blockArray, BiomeGenBase[] biomeArray) {
            this(provider, chunkX, chunkZ, blockArray, new byte[blockArray == null ? 0 : blockArray.length], biomeArray, null);
        }

        public ReplaceBiomeBlocks(IChunkProvider provider, int chunkX, int chunkZ, Block[] blockArray, byte[] metaArray, BiomeGenBase[] biomeArray) {
            this(provider, chunkX, chunkZ, blockArray, metaArray, biomeArray, null);
        }

        public ReplaceBiomeBlocks(IChunkProvider provider, int chunkX, int chunkZ, Block[] blockArray, byte[] metaArray, BiomeGenBase[] biomeArray, World world) {
            super(provider);
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.blockArray = blockArray;
            this.metaArray = metaArray;
            this.biomeArray = biomeArray;
            this.world = world;
        }
    }
}
