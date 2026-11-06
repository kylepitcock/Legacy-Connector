package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Beta 1.7.3 NBT list (tag 9): elements of one type. */
public class NbtList extends NbtElement {
    private List<NbtElement> value = new ArrayList<>();
    private byte type;

    public NbtList() {}

    @Override
    void write(DataOutput out) throws IOException {
        type = value.isEmpty() ? 1 : value.get(0).getType();
        out.writeByte(type);
        out.writeInt(value.size());
        for (NbtElement e : value) e.write(out);
    }

    @Override
    void read(DataInput in) throws IOException {
        type = in.readByte();
        int n = in.readInt();
        value = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            NbtElement e = createTypeFromId(type);
            e.read(in);
            value.add(e);
        }
    }

    @Override
    public byte getType() {
        return 9;
    }

    @Override
    public String toString() {
        return value.size() + " entries of type " + getTypeNameFromId(type);
    }

    public void add(NbtElement e) {
        type = e.getType();
        value.add(e);
    }

    public NbtElement get(int index) {
        return value.get(index);
    }

    public int size() {
        return value.size();
    }

    /** The element type (for the conversion to a modern list). */
    public byte mcc$elementType() {
        return value.isEmpty() ? type : value.get(0).getType();
    }
}
