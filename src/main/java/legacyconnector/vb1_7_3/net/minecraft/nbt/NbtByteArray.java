package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Beta 1.7.3 NBT tag 7 (generated with its siblings; see {@link NbtElement}). */
public class NbtByteArray extends NbtElement {
    public byte[] value;

    public NbtByteArray() {}

    public NbtByteArray(byte[] value) {
        this.value = value;
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeInt(value.length);
        out.write(value);
    }

    @Override
    void read(DataInput in) throws IOException {
        value = new byte[in.readInt()];
        in.readFully(value);
    }

    @Override
    public byte getType() {
        return 7;
    }

    @Override
    public String toString() {
        return "[" + value.length + " bytes]";
    }
}
