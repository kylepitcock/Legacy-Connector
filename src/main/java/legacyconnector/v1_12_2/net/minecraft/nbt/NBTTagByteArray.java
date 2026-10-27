package legacyconnector.v1_12_2.net.minecraft.nbt;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.Tag;

public class NBTTagByteArray extends NBTBase {
    private final byte[] data;

    public NBTTagByteArray(byte[] data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 7;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagByteArray(data.clone());
    }

    @Override
    public Tag mcc$modern() {
        return new ByteArrayTag(data.clone());
    }

    public byte[] getByteArray() {
        return data;
    }


    public NBTTagByteArray(java.util.List<Byte> values) {
        this(mcc$bytes(values));
    }

    private static byte[] mcc$bytes(java.util.List<Byte> values) {
        byte[] out = new byte[values.size()];
        for (int i = 0; i < out.length; i++) {
            Byte v = values.get(i);
            out[i] = v == null ? 0 : v;
        }
        return out;
    }
}
