package mccompat.v1_7_10.net.minecraft.world;

public class ChunkCoordIntPair {
    public final int chunkXPos;
    public final int chunkZPos;

    public ChunkCoordIntPair(int x, int z) {
        this.chunkXPos = x;
        this.chunkZPos = z;
    }

    public static long chunkXZ2Int(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    public int getCenterXPos() {
        return (chunkXPos << 4) + 8;
    }

    public int getCenterZPosition() {
        return (chunkZPos << 4) + 8;
    }

    @Override
    public int hashCode() {
        int i = 1664525 * chunkXPos + 1013904223;
        int j = 1664525 * (chunkZPos ^ -559038737) + 1013904223;
        return i ^ j;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChunkCoordIntPair p && p.chunkXPos == chunkXPos && p.chunkZPos == chunkZPos;
    }

    @Override
    public String toString() {
        return "[" + chunkXPos + ", " + chunkZPos + "]";
    }
}
