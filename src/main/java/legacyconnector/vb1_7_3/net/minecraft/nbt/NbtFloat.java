package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Beta 1.7.3 NBT tag 5 (generated with its siblings; see {@link NbtElement}). */
public class NbtFloat extends NbtElement {
    public float value;

    public NbtFloat() {}

    public NbtFloat(float value) {
        this.value = value;
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeFloat(value);
    }

    @Override
    void read(DataInput in) throws IOException {
        value = in.readFloat();
    }

    @Override
    public byte getType() {
        return 5;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
