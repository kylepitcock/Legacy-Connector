package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.12.2 shared int[] pool. Here every request is a fresh array, which is thread-safe for parallel world gen; the pool's lists and size
 * counter exist for mods that reset or inspect them (they stay empty, so clearing them is harmless).
 */
public class IntCache {
    /** Size of a small pooled array (1.12 default). */
    public static int intCacheSize = 256;
    public static final List<int[]> freeSmallArrays = Collections.synchronizedList(new ArrayList<>());
    public static final List<int[]> inUseSmallArrays = Collections.synchronizedList(new ArrayList<>());
    public static final List<int[]> freeLargeArrays = Collections.synchronizedList(new ArrayList<>());
    public static final List<int[]> inUseLargeArrays = Collections.synchronizedList(new ArrayList<>());

    public static int[] getIntCache(int size) {
        return new int[size];
    }

    public static void resetIntCache() {
        freeSmallArrays.clear();
        inUseSmallArrays.clear();
        freeLargeArrays.clear();
        inUseLargeArrays.clear();
    }

    public static String getCacheSizes() {
        return "cache: 0, tcache: 0, allocated: 0, tallocated: 0";
    }
}
