package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Beta 1.7.3 NBT tag 1 (generated with its siblings; see {@link NbtElement}). */
public class NbtByte extends NbtElement {
    public byte value;

    public NbtByte() {}

    public NbtByte(byte value) {
        this.value = value;
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeByte(value);
    }

    @Override
    void read(DataInput in) throws IOException {
        value = in.readByte();
    }

    @Override
    public byte getType() {
        return 1;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
