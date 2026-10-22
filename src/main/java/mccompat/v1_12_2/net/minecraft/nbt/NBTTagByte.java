package mccompat.v1_12_2.net.minecraft.nbt;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;

public class NBTTagByte extends NBTBase.NBTPrimitive {
    private final byte data;

    public NBTTagByte(byte data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 1;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagByte(data);
    }

    @Override
    public Tag mcc$modern() {
        return ByteTag.valueOf(data);
    }

    @Override
    public long getLong() {
        return (long) data;
    }

    @Override
    public int getInt() {
        return (int) data;
    }

    @Override
    public short getShort() {
        return (short) data;
    }

    @Override
    public byte getByte() {
        return (byte) data;
    }

    @Override
    public double getDouble() {
        return (double) data;
    }

    @Override
    public float getFloat() {
        return (float) data;
    }
}
