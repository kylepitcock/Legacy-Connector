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

        /**
         * The block ids of a 1.4.7 to 1.6.4 generator (one byte per block, {@code blockArray} of the old event): kept as given, so the handlers
         * that edit it (through the 1.6.4 facade) change the caller's own array; {@link #blockArray} holds the same blocks for 1.7.10 handlers.
         */
        public ReplaceBiomeBlocks(IChunkProvider provider, int chunkX, int chunkZ, byte[] ids, BiomeGenBase[] biomeArray) {
            this(provider, chunkX, chunkZ, decode(ids), new byte[ids == null ? 0 : ids.length], biomeArray, null);
            this.mcc$ids = ids;
        }

        /** The old id array of the event (null until a 1.6.4 handler asks for it of an event built with blocks). */
        public byte[] mcc$ids;

        private static Block[] decode(byte[] ids) {
            if (ids == null) return null;
            Block[] blocks = new Block[ids.length];
            for (int i = 0; i < ids.length; i++) blocks[i] = legacyconnector.v1_6_4.net.minecraft.util.Ids.block(ids[i] & 255);
            return blocks;
        }

        /** The ids a 1.6.4 handler sees and edits; made from the blocks the first time, written back by {@link #mcc$syncIds()}. */
        public byte[] mcc$idArray() {
            if (mcc$ids == null && blockArray != null) {
                mcc$ids = new byte[blockArray.length];
                for (int i = 0; i < blockArray.length; i++) mcc$ids[i] = (byte) legacyconnector.v1_6_4.net.minecraft.util.Ids.blockId(blockArray[i]);
                mcc$original = mcc$ids.clone();
            }
            return mcc$ids;
        }

        private byte[] mcc$original;

        /** After posting: block ids a 1.6.4 handler changed in the array it was given go back into the block array. */
        public void mcc$syncIds() {
            if (mcc$ids == null || mcc$original == null) return;
            for (int i = 0; i < mcc$ids.length; i++) {
                if (mcc$ids[i] != mcc$original[i]) blockArray[i] = legacyconnector.v1_6_4.net.minecraft.util.Ids.block(mcc$ids[i] & 255);
            }
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
