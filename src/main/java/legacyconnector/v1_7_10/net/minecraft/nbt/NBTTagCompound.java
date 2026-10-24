package legacyconnector.v1_7_10.net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class NBTTagCompound extends NBTBase {
    private final CompoundTag mcc$tag;
    /** Child views handed out, so repeated getCompoundTag(key) calls return the same object like in 1.7.10. */
    private final Map<String, NBTBase> mcc$children = new HashMap<>();

    public NBTTagCompound() {
        this(new CompoundTag());
    }

    public NBTTagCompound(CompoundTag tag) {
        this.mcc$tag = tag;
    }

    @Override
    public CompoundTag mcc$modern() {
        return mcc$tag;
    }

    @Override
    public byte getId() {
        return 10;
    }

    public Set<String> getKeySet() {
        return mcc$tag.getAllKeys();
    }

    public void setTag(String key, NBTBase value) {
        if (value == null) return; // legacy stored null and failed later when writing the tag
        mcc$children.remove(key);
        mcc$tag.put(key, value.mcc$modern());
        if (value instanceof NBTTagCompound || value instanceof NBTTagList) {
            // Keep the caller's object live: legacy code often mutates a tag after inserting it.
            Tag inserted = mcc$tag.get(key);
            NBTBase view = NBTBase.mcc$wrap(inserted, this);
            mcc$children.put(key, view);
            value.mcc$parent = this;
            if (value instanceof NBTTagCompound c && inserted instanceof CompoundTag) {
                mcc$children.put(key, c);
                mcc$tag.put(key, c.mcc$tag);
            } else if (value instanceof NBTTagList l && inserted instanceof ListTag) {
                mcc$children.put(key, l);
                mcc$tag.put(key, l.mcc$modern());
            }
        }
        mcc$markDirty();
    }

    public void setByte(String key, byte value) {
        mcc$tag.putByte(key, value);
        mcc$markDirty();
    }

    public void setShort(String key, short value) {
        mcc$tag.putShort(key, value);
        mcc$markDirty();
    }

    public void setInteger(String key, int value) {
        mcc$tag.putInt(key, value);
        mcc$markDirty();
    }

    public void setLong(String key, long value) {
        mcc$tag.putLong(key, value);
        mcc$markDirty();
    }

    public void setFloat(String key, float value) {
        mcc$tag.putFloat(key, value);
        mcc$markDirty();
    }

    public void setDouble(String key, double value) {
        mcc$tag.putDouble(key, value);
        mcc$markDirty();
    }

    public void setString(String key, String value) {
        mcc$tag.putString(key, value);
        mcc$markDirty();
    }

    /** A null array was storable in 1.7.10; the modern tags cannot write one (the chunk save fails), so it becomes empty. */
    public void setByteArray(String key, byte[] value) {
        mcc$tag.putByteArray(key, value == null ? new byte[0] : value);
        mcc$markDirty();
    }

    public void setIntArray(String key, int[] value) {
        mcc$tag.putIntArray(key, value == null ? new int[0] : value);
        mcc$markDirty();
    }

    public void setBoolean(String key, boolean value) {
        setByte(key, (byte) (value ? 1 : 0));
    }

    public NBTBase getTag(String key) {
        NBTBase child = mcc$children.get(key);
        if (child != null && mcc$tag.get(key) == child.mcc$modern()) return child;
        Tag tag = mcc$tag.get(key);
        if (tag == null) return null;
        NBTBase view = NBTBase.mcc$wrap(tag, this);
        if (view instanceof NBTTagCompound || view instanceof NBTTagList) mcc$children.put(key, view);
        return view;
    }

    public byte getTagId(String key) {
        Tag tag = mcc$tag.get(key);
        return tag == null ? 0 : tag.getId();
    }

    public boolean hasKey(String key) {
        return mcc$tag.contains(key);
    }

    public boolean hasKey(String key, int type) {
        return mcc$tag.contains(key, type);
    }

    public byte getByte(String key) {
        return mcc$tag.getByte(key);
    }

    public short getShort(String key) {
        return mcc$tag.getShort(key);
    }

    public int getInteger(String key) {
        return mcc$tag.getInt(key);
    }

    public long getLong(String key) {
        return mcc$tag.getLong(key);
    }

    public float getFloat(String key) {
        return mcc$tag.getFloat(key);
    }

    public double getDouble(String key) {
        return mcc$tag.getDouble(key);
    }

    public String getString(String key) {
        return mcc$tag.getString(key);
    }

    public byte[] getByteArray(String key) {
        return mcc$tag.get(key) instanceof ByteArrayTag b ? b.getAsByteArray() : new byte[0];
    }

    public int[] getIntArray(String key) {
        return mcc$tag.get(key) instanceof IntArrayTag i ? i.getAsIntArray() : new int[0];
    }

    /** Returns a live view; a missing key yields a detached empty compound, as in 1.7.10. */
    public NBTTagCompound getCompoundTag(String key) {
        NBTBase tag = getTag(key);
        return tag instanceof NBTTagCompound c ? c : new NBTTagCompound();
    }

    public NBTTagList getTagList(String key, int type) {
        NBTBase tag = getTag(key);
        if (tag instanceof NBTTagList list && (list.tagCount() == 0 || list.getTagType() == type)) return list;
        return new NBTTagList();
    }

    public boolean getBoolean(String key) {
        return getByte(key) != 0;
    }

    public void removeTag(String key) {
        mcc$children.remove(key);
        mcc$tag.remove(key);
        mcc$markDirty();
    }

    public boolean hasNoTags() {
        return mcc$tag.isEmpty();
    }

    @Override
    public NBTBase copy() {
        return new NBTTagCompound(mcc$tag.copy());
    }
}
