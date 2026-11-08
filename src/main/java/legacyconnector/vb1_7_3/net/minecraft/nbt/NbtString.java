package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Beta 1.7.3 NBT tag 8 (generated with its siblings; see {@link NbtElement}). */
public class NbtString extends NbtElement {
    public String value;

    public NbtString() {}

    public NbtString(String value) {
        this.value = value;
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeUTF(value);
    }

    @Override
    void read(DataInput in) throws IOException {
        value = in.readUTF();
    }

    @Override
    public byte getType() {
        return 8;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
