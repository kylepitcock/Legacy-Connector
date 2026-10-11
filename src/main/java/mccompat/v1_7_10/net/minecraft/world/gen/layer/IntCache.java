package mccompat.v1_7_10.net.minecraft.world.gen.layer;

/** 1.7.10 shared int[] pool. Here every request is a fresh array, which is thread-safe for parallel world gen. */
public class IntCache {
    public static int[] getIntCache(int size) {
        return new int[size];
    }

    public static void resetIntCache() {
    }

    public static String getCacheSizes() {
        return "cache: 0, tcache: 0, allocated: 0, tallocated: 0";
    }
}
