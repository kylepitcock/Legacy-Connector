package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * 1.12.2 block states of a 16x16x16 section ({@code ExtendedBlockStorage.getData()}), indexed {@code y << 8 | z << 4 | x}. The
 * section's container reads and writes the section itself (and so the chunk it views); one a mod creates keeps its own states.
 * The NBT form is 1.12's: block id low bytes, a nibble of metadata and a nibble of high id bits per position.
 */
public class BlockStateContainer {
    private final ExtendedBlockStorage mcc$section;
    private final IBlockState[] mcc$states;

    public BlockStateContainer() {
        this.mcc$section = null;
        this.mcc$states = new IBlockState[4096];
    }

    public BlockStateContainer(ExtendedBlockStorage section) {
        this.mcc$section = section;
        this.mcc$states = null;
    }

    private static IBlockState air() {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
    }

    private static int getIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    public void set(int x, int y, int z, IBlockState state) {
        set(getIndex(x & 15, y & 15, z & 15), state);
    }

    protected void set(int index, IBlockState state) {
        if (mcc$section != null) {
            mcc$section.set(index & 15, index >> 8 & 15, index >> 4 & 15, state == null ? air() : state);
        } else {
            mcc$states[index & 4095] = state;
        }
    }

    public IBlockState get(int x, int y, int z) {
        return get(getIndex(x & 15, y & 15, z & 15));
    }

    protected IBlockState get(int index) {
        if (mcc$section != null) return mcc$section.get(index & 15, index >> 8 & 15, index >> 4 & 15);
        IBlockState s = mcc$states[index & 4095];
        return s == null ? air() : s;
    }

    /** Writes the states as 1.12's NBT arrays: block id low bytes into {@code blockIds}, metadata into {@code data}; returns the high id bits, or null when none are set. */
    public NibbleArray getDataForNBT(byte[] blockIds, NibbleArray data) {
        NibbleArray add = null;
        for (int i = 0; i < 4096; ++i) {
            IBlockState state = get(i);
            Block block = state.getBlock();
            int id = Block.getIdFromBlock(block);
            int meta = block.getMetaFromState(state) & 15;
            int x = i & 15, y = i >> 8 & 15, z = i >> 4 & 15;
            if ((id >> 8 & 15) != 0) {
                if (add == null) add = new NibbleArray();
                add.set(x, y, z, id >> 8 & 15);
            }
            blockIds[i] = (byte) (id & 255);
            data.set(x, y, z, meta);
        }
        return add;
    }

    /** Reads 1.12's NBT arrays (see {@link #getDataForNBT}); unknown ids become air. */
    public void setDataFromNBT(byte[] blockIds, NibbleArray data, NibbleArray blockIdExtension) {
        for (int i = 0; i < 4096; ++i) {
            int x = i & 15, y = i >> 8 & 15, z = i >> 4 & 15;
            int high = blockIdExtension == null ? 0 : blockIdExtension.get(x, y, z);
            int id = high << 8 | blockIds[i] & 255;
            int meta = data == null ? 0 : data.get(x, y, z);
            Block block = Block.getBlockById(id);
            IBlockState state = block == null ? null : block.getStateFromMeta(meta);
            set(i, state == null ? air() : state);
        }
    }
}
