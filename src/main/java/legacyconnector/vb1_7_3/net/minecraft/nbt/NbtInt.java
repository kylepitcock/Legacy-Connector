package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Beta 1.7.3 NBT tag 3 (generated with its siblings; see {@link NbtElement}). */
public class NbtInt extends NbtElement {
    public int value;

    public NbtInt() {}

    public NbtInt(int value) {
        this.value = value;
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeInt(value);
    }

    @Override
    void read(DataInput in) throws IOException {
        value = in.readInt();
    }

    @Override
    public byte getType() {
        return 3;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
