package legacyconnector.v1_12_2.net.minecraft.util;

/** 1.12.2's packed array of fixed-width ints (a chunk section stores its palette ids in one); entries may straddle two longs. */
public class BitArray {
    private final long[] longArray;
    private final int bitsPerEntry;
    private final long maxEntryValue;
    private final int arraySize;

    public BitArray(int bitsPerEntryIn, int arraySizeIn) {
        if (bitsPerEntryIn < 1 || bitsPerEntryIn > 32) throw new IllegalArgumentException("The value " + bitsPerEntryIn + " is not in the specified inclusive range of 1 to 32");
        this.arraySize = arraySizeIn;
        this.bitsPerEntry = bitsPerEntryIn;
        this.maxEntryValue = (1L << bitsPerEntryIn) - 1L;
        this.longArray = new long[(int) (((long) arraySizeIn * bitsPerEntryIn + 63) / 64)];
    }

    public void setAt(int index, int value) {
        long start = (long) index * bitsPerEntry;
        int i = (int) (start / 64);
        int j = (int) (((long) (index + 1) * bitsPerEntry - 1L) / 64);
        int k = (int) (start % 64);
        longArray[i] = longArray[i] & ~(maxEntryValue << k) | ((long) value & maxEntryValue) << k;
        if (i != j) {
            int l = 64 - k;
            int m = bitsPerEntry - l;
            longArray[j] = longArray[j] >>> m << m | ((long) value & maxEntryValue) >> l;
        }
    }

    public int getAt(int index) {
        long start = (long) index * bitsPerEntry;
        int i = (int) (start / 64);
        int j = (int) (((long) (index + 1) * bitsPerEntry - 1L) / 64);
        int k = (int) (start % 64);
        if (i == j) return (int) (longArray[i] >>> k & maxEntryValue);
        int l = 64 - k;
        return (int) ((longArray[i] >>> k | longArray[j] << l) & maxEntryValue);
    }

    public long[] getBackingLongArray() {
        return longArray;
    }

    public int size() {
        return arraySize;
    }
}
