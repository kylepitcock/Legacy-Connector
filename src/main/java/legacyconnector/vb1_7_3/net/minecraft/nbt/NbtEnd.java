package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;

/** Beta 1.7.3 NBT end marker (tag 0). */
public class NbtEnd extends NbtElement {
    public NbtEnd() {}

    @Override
    void write(DataOutput out) {}

    @Override
    void read(DataInput in) {}

    @Override
    public byte getType() {
        return 0;
    }

    @Override
    public String toString() {
        return "END";
    }
}
