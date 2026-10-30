package legacyconnector.v1_7_10.net.minecraft.nbt;

/** NBT read size limit. The modern reader enforces its own limit, so this only carries the budget. */
public class NBTSizeTracker {
    public static final NBTSizeTracker INFINITE = new NBTSizeTracker(0L);

    private final long max;
    private long read;

    public NBTSizeTracker(long max) {
        this.max = max;
    }

    /** {@code addSpaceRead} in 1.7.10's mappings. */
    public void addSpaceRead(long bits) {
        read += bits / 8L;
        if (max != 0L && read > max) throw new RuntimeException("Tried to read NBT tag that was too big; tried to allocate: " + read + "bytes where max allowed: " + max);
    }
}
