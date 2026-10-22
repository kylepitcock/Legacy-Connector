package legacyconnector.v1_12_2.net.minecraft.nbt;

import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;

public class NBTTagDouble extends NBTBase.NBTPrimitive {
    private final double data;

    public NBTTagDouble(double data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 6;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagDouble(data);
    }

    @Override
    public Tag mcc$modern() {
        return DoubleTag.valueOf(data);
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
