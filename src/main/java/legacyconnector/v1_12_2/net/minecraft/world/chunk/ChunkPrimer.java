package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/**
 * The 16x256x16 block buffer 1.12.2 chunk generators fill. As in 1.12, {@link #data} holds each position's state id
 * ({@code blockId << 4 | meta}, 0 = air) and fast generators write it directly. The state objects set through
 * {@link #setBlockState} are kept beside it, so states the id cannot express (blocks above id 4095, state not stored in the
 * metadata) survive; a position whose id a mod changed reads back the state of the new id.
 */
public class ChunkPrimer {
    /** 1.12's state ids of the buffer (index {@code x << 12 | z << 8 | y}). */
    public final char[] data = new char[65536];
    private final IBlockState[] mcc$states = new IBlockState[65536];
    private final char[] mcc$codes = new char[65536];
    /** States by legacy id, filled as ids are read. */
    private static final IBlockState[] BY_ID = new IBlockState[65536];
    /** The id of a state whose block id does not fit (never decoded). */
    private static final char WIDE = (char) 0xFFFF;

    public ChunkPrimer() {
    }

    private static int getBlockIndex(int x, int y, int z) {
        return x << 12 | z << 8 | y;
    }

    private static char code(IBlockState state) {
        if (state == null) return 0;
        Block block = state.getBlock();
        int id = Block.getIdFromBlock(block);
        if (id < 0 || id >= 4096) return WIDE;
        return (char) (id << 4 | block.getMetaFromState(state) & 15);
    }

    private static IBlockState decode(char c) {
        if (c == 0 || c == WIDE) return air();
        IBlockState s = BY_ID[c];
        if (s == null) {
            Block block = Block.getBlockById(c >> 4);
            s = block == null ? air() : block.getStateFromMeta(c & 15);
            if (s == null) s = air();
            BY_ID[c] = s;
        }
        return s;
    }

    private IBlockState at(int index) {
        char c = data[index];
        IBlockState s = mcc$states[index];
        if (s != null && mcc$codes[index] == c) return s;
        return decode(c);
    }

    public IBlockState getBlockState(int x, int y, int z) {
        if (x < 0 || x > 15 || y < 0 || y > 255 || z < 0 || z > 15) return air();
        return at(getBlockIndex(x, y, z));
    }

    public void setBlockState(int x, int y, int z, IBlockState state) {
        if (x < 0 || x > 15 || y < 0 || y > 255 || z < 0 || z > 15) return;
        int i = getBlockIndex(x, y, z);
        char c = code(state);
        mcc$states[i] = state;
        mcc$codes[i] = c;
        data[i] = c;
    }

    /** The y above the highest non-air block of a column (0 when it is empty). */
    public int findGroundBlockIdx(int x, int z) {
        int base = (x << 4 | z) << 8;
        for (int y = 255; y >= 0; y--) {
            IBlockState state = at(base + y);
            if (state != null && state != air()) return y;
        }
        return 0;
    }

    /** Whether anything was set at a position (generators leave most of the buffer untouched). */
    public boolean mcc$isSet(int x, int y, int z) {
        int i = getBlockIndex(x, y, z);
        if (data[i] == 0 && mcc$states[i] == null) return false;
        IBlockState state = at(i);
        return state != null && state != air();
    }

    private static IBlockState AIR;

    private static IBlockState air() {
        IBlockState a = AIR;
        if (a == null) AIR = a = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        return a;
    }
}
