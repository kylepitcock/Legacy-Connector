package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Beta 1.7.3 NBT tag 6 (generated with its siblings; see {@link NbtElement}). */
public class NbtDouble extends NbtElement {
    public double value;

    public NbtDouble() {}

    public NbtDouble(double value) {
        this.value = value;
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeDouble(value);
    }

    @Override
    void read(DataInput in) throws IOException {
        value = in.readDouble();
    }

    @Override
    public byte getType() {
        return 6;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
