package mccompat.v1_7_10.net.minecraft.nbt;

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
}
