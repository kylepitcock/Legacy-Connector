package legacyconnector.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

public class NBTTagLong extends NBTBase.NBTPrimitive {
    private final long data;

    /** 1.6.4 tags carried a name: a bare named tag holds the default value. */
    public NBTTagLong(String name) {
        this(0L);
    }

    public NBTTagLong(long data) {
        this.data = data;
    }

    @Override
    public byte getId() {
        return 4;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagLong(data);
    }

    @Override
    public Tag mcc$modern() {
        return LongTag.valueOf(data);
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
