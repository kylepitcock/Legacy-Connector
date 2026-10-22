package legacyconnector.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Legacy NBT objects are views over modern {@link Tag}s. Compound and list views know their parent so that a
 * mutation anywhere in a tree marks the root dirty; owners (item stacks) use that to write changes back into
 * immutable modern data components.
 */
public abstract class NBTBase {
    public static final String[] NBTTypes = {"END", "BYTE", "SHORT", "INT", "LONG", "FLOAT", "DOUBLE", "BYTE[]", "STRING", "LIST", "COMPOUND", "INT[]"};

    NBTBase mcc$parent;
    Runnable mcc$onChange;

    public abstract byte getId();

    public abstract NBTBase copy();

    public abstract Tag mcc$modern();

    protected String getString() {
        return mcc$modern().getAsString();
    }

    @Override
    public String toString() {
        return mcc$modern().toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NBTBase other && mcc$modern().equals(other.mcc$modern());
    }

    @Override
    public int hashCode() {
        return mcc$modern().hashCode();
    }

    public static NBTBase createNewByType(byte id) {
        return switch (id) {
            case 1 -> new NBTTagByte((byte) 0);
            case 2 -> new NBTTagShort((short) 0);
            case 3 -> new NBTTagInt(0);
            case 4 -> new NBTTagLong(0);
            case 5 -> new NBTTagFloat(0);
            case 6 -> new NBTTagDouble(0);
            case 7 -> new NBTTagByteArray(new byte[0]);
            case 8 -> new NBTTagString("");
            case 9 -> new NBTTagList();
            case 10 -> new NBTTagCompound();
            case 11 -> new NBTTagIntArray(new int[0]);
            default -> null;
        };
    }

    /** Called by mutators; walks to the root and fires its change listener. */
    protected void mcc$markDirty() {
        NBTBase n = this;
        while (n != null) {
            if (n.mcc$onChange != null) n.mcc$onChange.run();
            n = n.mcc$parent;
        }
    }

    public void mcc$setChangeListener(Runnable listener) {
        this.mcc$onChange = listener;
    }

    /** Wraps a modern tag. Compounds and lists become live views; primitives are copied (they are immutable). */
    public static NBTBase mcc$wrap(Tag tag, NBTBase parent) {
        NBTBase out;
        if (tag instanceof CompoundTag c) out = new NBTTagCompound(c);
        else if (tag instanceof ListTag l) out = new NBTTagList(l);
        else if (tag instanceof ByteTag b) out = new NBTTagByte(b.getAsByte());
        else if (tag instanceof ShortTag s) out = new NBTTagShort(s.getAsShort());
        else if (tag instanceof IntTag i) out = new NBTTagInt(i.getAsInt());
        else if (tag instanceof LongTag l) out = new NBTTagLong(l.getAsLong());
        else if (tag instanceof FloatTag f) out = new NBTTagFloat(f.getAsFloat());
        else if (tag instanceof DoubleTag d) out = new NBTTagDouble(d.getAsDouble());
        else if (tag instanceof ByteArrayTag b) out = new NBTTagByteArray(b.getAsByteArray());
        else if (tag instanceof StringTag s) out = new NBTTagString(s.getAsString());
        else if (tag instanceof IntArrayTag i) out = new NBTTagIntArray(i.getAsIntArray());
        else if (tag instanceof LongArrayTag l) out = new NBTTagIntArray(java.util.Arrays.stream(l.getAsLongArray()).mapToInt(v -> (int) v).toArray());
        else return null;
        out.mcc$parent = parent;
        return out;
    }

    public abstract static class NBTPrimitive extends NBTBase {
        public abstract long getLong();

        public abstract int getInt();

        public abstract short getShort();

        public abstract byte getByte();

        public abstract double getDouble();

        public abstract float getFloat();
    }
}
