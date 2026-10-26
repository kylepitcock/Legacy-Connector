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

    /** Phantom field: accesses are redirected to {@link #mcc$get$tagMap} (mods made it public to walk or edit the tags). */
    @Deprecated
    public Map tagMap;

    /**
     * A live view of the tags by name, as the HashMap 1.7.10 kept (mods cast it): reads wrap the tags, writes go through
     * {@link #setTag}/{@link #removeTag}.
     */
    public static Map mcc$get$tagMap(NBTTagCompound compound) {
        return new TagMapView(compound);
    }

    private static final class TagMapView extends HashMap<String, NBTBase> {
        private final NBTTagCompound owner;

        TagMapView(NBTTagCompound owner) {
            super(0);
            this.owner = owner;
        }

        @Override public int size() { return owner.mcc$tag.size(); }
        @Override public boolean isEmpty() { return owner.mcc$tag.isEmpty(); }
        @Override public boolean containsKey(Object key) { return key instanceof String s && owner.mcc$tag.contains(s); }
        @Override public NBTBase get(Object key) { return key instanceof String s ? owner.getTag(s) : null; }
        @Override public NBTBase getOrDefault(Object key, NBTBase fallback) { NBTBase v = get(key); return v != null ? v : fallback; }
        @Override public boolean containsValue(Object value) { return values().contains(value); }

        @Override
        public NBTBase put(String key, NBTBase value) {
            NBTBase old = owner.getTag(key);
            owner.setTag(key, value);
            return old;
        }

        @Override
        public NBTBase remove(Object key) {
            if (!(key instanceof String s)) return null;
            NBTBase old = owner.getTag(s);
            owner.removeTag(s);
            return old;
        }

        @Override
        public void putAll(Map<? extends String, ? extends NBTBase> m) {
            for (Map.Entry<? extends String, ? extends NBTBase> e : m.entrySet()) put(e.getKey(), e.getValue());
        }

        @Override
        public void clear() {
            for (String key : new java.util.ArrayList<>(owner.mcc$tag.getAllKeys())) owner.removeTag(key);
        }

        @Override
        public Set<String> keySet() {
            return new java.util.AbstractSet<>() {
                @Override public int size() { return owner.mcc$tag.size(); }
                @Override public boolean contains(Object o) { return containsKey(o); }
                @Override public boolean remove(Object o) { return TagMapView.this.remove(o) != null; }

                @Override
                public java.util.Iterator<String> iterator() {
                    java.util.Iterator<String> keys = new java.util.ArrayList<>(owner.mcc$tag.getAllKeys()).iterator();
                    return new java.util.Iterator<>() {
                        String last;
                        @Override public boolean hasNext() { return keys.hasNext(); }
                        @Override public String next() { return last = keys.next(); }
                        @Override public void remove() { owner.removeTag(last); }
                    };
                }
            };
        }

        @Override
        public java.util.Collection<NBTBase> values() {
            java.util.List<NBTBase> out = new java.util.ArrayList<>();
            for (String key : owner.mcc$tag.getAllKeys()) out.add(owner.getTag(key));
            return out;
        }

        @Override
        public Set<Map.Entry<String, NBTBase>> entrySet() {
            return new java.util.AbstractSet<>() {
                @Override public int size() { return owner.mcc$tag.size(); }

                @Override
                public java.util.Iterator<Map.Entry<String, NBTBase>> iterator() {
                    java.util.Iterator<String> keys = keySet().iterator();
                    return new java.util.Iterator<>() {
                        @Override public boolean hasNext() { return keys.hasNext(); }
                        @Override public void remove() { keys.remove(); }

                        @Override
                        public Map.Entry<String, NBTBase> next() {
                            String key = keys.next();
                            return new java.util.AbstractMap.SimpleEntry<>(key, owner.getTag(key)) {
                                @Override
                                public NBTBase setValue(NBTBase value) {
                                    super.setValue(value);
                                    return put(key, value);
                                }
                            };
                        }
                    };
                }
            };
        }

        @Override public void forEach(java.util.function.BiConsumer<? super String, ? super NBTBase> action) {
            for (Map.Entry<String, NBTBase> e : entrySet()) action.accept(e.getKey(), e.getValue());
        }

        @Override public boolean equals(Object o) { return o instanceof Map<?, ?> m && entrySet().equals(m.entrySet()); }
        @Override public int hashCode() { return entrySet().hashCode(); }
        @Override public String toString() { return owner.toString(); }
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
