package legacyconnector.v1_12_2.net.minecraft.nbt;

/** Limits how many bytes a read may allocate. */
public class NBTSizeTracker {
    public static final NBTSizeTracker INFINITE = new NBTSizeTracker(0L) {
        @Override
        public void read(long bits) {
        }

        @Override
        net.minecraft.nbt.NbtAccounter mcc$accounter() {
            return net.minecraft.nbt.NbtAccounter.unlimitedHeap();
        }
    };

    private final long max;
    private long read;

    public NBTSizeTracker(long max) {
        this.max = max;
    }

    /** Counts {@code bits} more bits (the legacy unit), failing past the limit. */
    public void read(long bits) {
        this.read += bits / 8L;
        if (this.read > this.max) {
            throw new RuntimeException("Tried to read NBT tag that was too big; tried to allocate: " + this.read + "bytes where max allowed: " + this.max);
        }
    }

    net.minecraft.nbt.NbtAccounter mcc$accounter() {
        return net.minecraft.nbt.NbtAccounter.create(max);
    }
}
