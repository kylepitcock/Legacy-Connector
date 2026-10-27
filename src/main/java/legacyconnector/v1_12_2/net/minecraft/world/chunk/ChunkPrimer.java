package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/**
 * The 16x256x16 block buffer 1.12.2 chunk generators fill. States are kept as objects (the legacy one stored block state ids);
 * an unset position is air.
 */
public class ChunkPrimer {
    private final IBlockState[] data = new IBlockState[65536];

    public ChunkPrimer() {
    }

    private static int getBlockIndex(int x, int y, int z) {
        return x << 12 | z << 8 | y;
    }

    public IBlockState getBlockState(int x, int y, int z) {
        if (x < 0 || x > 15 || y < 0 || y > 255 || z < 0 || z > 15) return air();
        IBlockState state = data[getBlockIndex(x, y, z)];
        return state == null ? air() : state;
    }

    public void setBlockState(int x, int y, int z, IBlockState state) {
        if (x < 0 || x > 15 || y < 0 || y > 255 || z < 0 || z > 15) return;
        data[getBlockIndex(x, y, z)] = state;
    }

    /** The y above the highest non-air block of a column (0 when it is empty). */
    public int findGroundBlockIdx(int x, int z) {
        int base = (x << 4 | z) << 8;
        for (int y = 255; y >= 0; y--) {
            IBlockState state = data[base + y];
            if (state != null && state != air()) return y;
        }
        return 0;
    }

    /** Whether anything was set at a position (generators leave most of the buffer untouched). */
    public boolean mcc$isSet(int x, int y, int z) {
        IBlockState state = data[getBlockIndex(x, y, z)];
        return state != null && state != air();
    }

    private static IBlockState AIR;

    private static IBlockState air() {
        IBlockState a = AIR;
        if (a == null) AIR = a = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        return a;
    }
}
