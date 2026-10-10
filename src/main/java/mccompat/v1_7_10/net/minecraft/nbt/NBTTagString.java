package mccompat.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class NBTTagString extends NBTBase {
    private final String data;

    public NBTTagString() {
        this("");
    }

    public NBTTagString(String data) {
        if (data == null) throw new IllegalArgumentException("Empty string not allowed");
        this.data = data;
    }

    @Override
    public byte getId() {
        return 8;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagString(data);
    }

    @Override
    public Tag mcc$modern() {
        return StringTag.valueOf(data);
    }

    @Override
    public String getString() {
        return data;
    }
}
