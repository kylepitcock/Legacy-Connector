package legacyconnector.v1_7_10.net.minecraft.nbt;

/** NBT read size limit. The modern reader enforces its own limit, so this only carries the budget. */
public class NBTSizeTracker {
    public static final NBTSizeTracker INFINITE = new NBTSizeTracker(0L);

    private final long max;
    private long read;

    public NBTSizeTracker(long max) {
        this.max = max;
    }

    /** The SRG name of {@link #addSpaceRead}. */
    public void func_152450_a(long bits) {
        addSpaceRead(bits);
    }

    /** Counts a modified-UTF-8 string against the budget: 16 bits of length, then 8 per encoded byte. */
    public static void readUTF(NBTSizeTracker tracker, String data) {
        tracker.addSpaceRead(16L);
        if (data == null) return;
        int length = data.length();
        int encoded = 0;
        for (int i = 0; i < length; i++) {
            int c = data.charAt(i);
            if (c >= 1 && c <= 127) encoded++;
            else if (c > 2047) encoded += 3;
            else encoded += 2;
        }
        tracker.addSpaceRead(8L * encoded);
    }

    /** {@code addSpaceRead} in 1.7.10's mappings. */
    public void addSpaceRead(long bits) {
        read += bits / 8L;
        if (max != 0L && read > max) throw new RuntimeException("Tried to read NBT tag that was too big; tried to allocate: " + read + "bytes where max allowed: " + max);
    }
}
