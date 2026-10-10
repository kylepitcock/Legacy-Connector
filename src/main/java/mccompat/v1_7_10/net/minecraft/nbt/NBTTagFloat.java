package mccompat.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;

public class NBTTagFloat extends NBTBase.NBTPrimitive {
    private final float data;

    public NBTTagFloat(float data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 5;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagFloat(data);
    }

    @Override
    public Tag mcc$modern() {
        return FloatTag.valueOf(data);
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
