package legacyconnector.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.Tag;

public class NBTTagByteArray extends NBTBase {
    private final byte[] data;

    /** 1.6.4 tags carried a name: a bare named tag holds the default value. */
    public NBTTagByteArray(String name) {
        this(new byte[0]);
    }

    public NBTTagByteArray(byte[] data) {
        this.data = data == null ? new byte[0] : data;
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
}
