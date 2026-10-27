package legacyconnector.v1_12_2.net.minecraft.nbt;

import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;

/** An array of longs (tag type 12, new in 1.12). */
public class NBTTagLongArray extends NBTBase {
    private final long[] data;

    public NBTTagLongArray(long[] data) {
        this.data = data;
    }

    public NBTTagLongArray(java.util.List<Long> values) {
        this.data = new long[values.size()];
        for (int i = 0; i < data.length; i++) {
            Long l = values.get(i);
            data[i] = l == null ? 0L : l;
        }
    }

    @Override
    public byte getId() {
        return 12;
    }

    @Override
    public NBTTagLongArray copy() {
        return new NBTTagLongArray(data.clone());
    }

    @Override
    public Tag mcc$modern() {
        return new LongArrayTag(data.clone());
    }

    /** Not in 1.12.2 (its array was private); kept for the layer's own readers. */
    public long[] mcc$data() {
        return data;
    }
}
