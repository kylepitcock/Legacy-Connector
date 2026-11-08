package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Beta 1.7.3 NBT tag 4 (generated with its siblings; see {@link NbtElement}). */
public class NbtLong extends NbtElement {
    public long value;

    public NbtLong() {}

    public NbtLong(long value) {
        this.value = value;
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeLong(value);
    }

    @Override
    void read(DataInput in) throws IOException {
        value = in.readLong();
    }

    @Override
    public byte getType() {
        return 4;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
