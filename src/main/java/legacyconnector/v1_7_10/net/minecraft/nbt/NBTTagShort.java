package legacyconnector.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;

public class NBTTagShort extends NBTBase.NBTPrimitive {
    private final short data;

    public NBTTagShort(short data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 2;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagShort(data);
    }

    @Override
    public Tag mcc$modern() {
        return ShortTag.valueOf(data);
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
