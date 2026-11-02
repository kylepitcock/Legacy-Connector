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

    public void set(int index, IBlockState state) {
        mcc$snapshot = null;
        if (mcc$section != null) {
            mcc$section.set(index & 15, index >> 8 & 15, index >> 4 & 15, state == null ? air() : state);
        } else {
            mcc$states[index & 4095] = state;
        }
    }

    public IBlockState get(int x, int y, int z) {
        return get(getIndex(x & 15, y & 15, z & 15));
    }

    public IBlockState get(int index) {
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

    // ------------------------------------------------------------------ legacy fields: storage, palette, bits

    /** 1.12's palette for sections of more than 256 distinct states. */
    public static final IBlockStatePalette REGISTRY_BASED_PALETTE = new BlockStatePaletteRegistry();

    /** Phantom: redirected to {@link #mcc$get$storage} (the section's palette ids packed as 1.12 stored them). */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.util.BitArray storage;
    /** Phantom: redirected to {@link #mcc$get$palette}. */
    @Deprecated
    public IBlockStatePalette palette;
    /** Phantom: redirected to {@link #mcc$get$bits}. */
    @Deprecated
    public int bits;

    /** A packed copy of the section in 1.12's layout: palette kind by distinct states (up to 16 linear, 256 hash map, else global ids). */
    private static final class Snapshot {
        legacyconnector.v1_12_2.net.minecraft.util.BitArray storage;
        IBlockStatePalette palette;
        int bits;
    }

    private Snapshot mcc$snapshot;

    /** Drops the packed copy so the next read of {@code storage}/{@code palette}/{@code bits} sees the section as it is now. */
    public void mcc$invalidate() {
        mcc$snapshot = null;
    }

    private Snapshot mcc$snapshot() {
        Snapshot snap = mcc$snapshot;
        if (snap != null) return snap;
        IBlockState[] states = new IBlockState[4096];
        java.util.IdentityHashMap<IBlockState, Boolean> distinct = new java.util.IdentityHashMap<>();
        for (int i = 0; i < 4096; i++) {
            states[i] = get(i);
            distinct.put(states[i], Boolean.TRUE);
        }
        int bits = 4;
        while ((1 << bits) < distinct.size() && bits < 9) bits++;
        snap = new Snapshot();
        IBlockStatePaletteResizer noResize = (b, s) -> 0;
        if (bits <= 4) {
            snap.palette = new BlockStatePaletteLinear(4, noResize);
        } else if (bits <= 8) {
            snap.palette = new BlockStatePaletteHashMap(bits, noResize);
        } else {
            snap.palette = REGISTRY_BASED_PALETTE;
            bits = Math.max(1, 32 - Integer.numberOfLeadingZeros(Block.BLOCK_STATE_IDS.size() - 1));
        }
        snap.bits = bits;
        snap.storage = new legacyconnector.v1_12_2.net.minecraft.util.BitArray(bits, 4096);
        for (int i = 0; i < 4096; i++) snap.storage.setAt(i, snap.palette.idFor(states[i]));
        mcc$snapshot = snap;
        return snap;
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.BitArray mcc$get$storage(BlockStateContainer c) {
        return c.mcc$snapshot().storage;
    }

    public static IBlockStatePalette mcc$get$palette(BlockStateContainer c) {
        return c.mcc$snapshot().palette;
    }

    public static int mcc$get$bits(BlockStateContainer c) {
        return c.mcc$snapshot().bits;
    }
}
