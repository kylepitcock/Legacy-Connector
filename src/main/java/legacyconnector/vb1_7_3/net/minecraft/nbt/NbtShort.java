package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Beta 1.7.3 NBT tag 2 (generated with its siblings; see {@link NbtElement}). */
public class NbtShort extends NbtElement {
    public short value;

    public NbtShort() {}

    public NbtShort(short value) {
        this.value = value;
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeShort(value);
    }

    @Override
    void read(DataInput in) throws IOException {
        value = in.readShort();
    }

    @Override
    public byte getType() {
        return 2;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
