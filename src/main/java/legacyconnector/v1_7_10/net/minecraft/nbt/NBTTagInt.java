package legacyconnector.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

public class NBTTagInt extends NBTBase.NBTPrimitive {
    private final int data;

    public NBTTagInt(int data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 3;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagInt(data);
    }

    @Override
    public Tag mcc$modern() {
        return IntTag.valueOf(data);
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
