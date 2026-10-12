package mccompat.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

public class NBTTagList extends NBTBase {
    private final ListTag mcc$list;

    /** Phantom field: accesses are redirected to {@link #mcc$get$tagList} (mods read the tags directly). */
    @Deprecated
    public java.util.List tagList;

    /** A live view of the tags as legacy wrappers; writes go through to the list. */
    public static java.util.List mcc$get$tagList(NBTTagList list) {
        ListTag backing = list.mcc$list;
        return new java.util.AbstractList<NBTBase>() {
            @Override
            public NBTBase get(int index) {
                return NBTBase.mcc$wrap(backing.get(index), list);
            }

            @Override
            public int size() {
                return backing.size();
            }

            @Override
            public NBTBase set(int index, NBTBase value) {
                NBTBase old = get(index);
                backing.set(index, value.mcc$modern());
                list.mcc$markDirty();
                return old;
            }

            @Override
            public void add(int index, NBTBase value) {
                backing.add(index, value.mcc$modern());
                list.mcc$markDirty();
            }

            @Override
            public NBTBase remove(int index) {
                NBTBase old = get(index);
                backing.remove(index);
                list.mcc$markDirty();
                return old;
            }
        };
    }

    public NBTTagList() {
        this(new ListTag());
    }

    public NBTTagList(ListTag list) {
        this.mcc$list = list;
    }

    @Override
    public ListTag mcc$modern() {
        return mcc$list;
    }

    @Override
    public byte getId() {
        return 9;
    }

    public void appendTag(NBTBase tag) {
        if (mcc$list.isEmpty() || mcc$list.getElementType() == tag.getId()) {
            mcc$list.add(tag.mcc$modern());
            if (tag instanceof NBTTagCompound || tag instanceof NBTTagList) tag.mcc$parent = this;
            mcc$markDirty();
        }
        // 1.7.10 logged "Adding mismatching tag types to tag list" and dropped the tag.
    }

    public void setTag(int index, NBTBase tag) {
        mcc$list.set(index, tag.mcc$modern());
        mcc$markDirty();
    }

    public NBTBase removeTag(int index) {
        Tag removed = mcc$list.remove(index);
        mcc$markDirty();
        return NBTBase.mcc$wrap(removed, null);
    }

    public NBTTagCompound getCompoundTagAt(int index) {
        if (index >= 0 && index < mcc$list.size() && mcc$list.get(index) instanceof CompoundTag c) {
            NBTTagCompound view = new NBTTagCompound(c);
            view.mcc$parent = this;
            return view;
        }
        return new NBTTagCompound();
    }

    public int[] getIntArrayAt(int index) {
        return index >= 0 && index < mcc$list.size() && mcc$list.get(index) instanceof IntArrayTag t ? t.getAsIntArray() : new int[0];
    }

    public double getDoubleAt(int index) {
        return index >= 0 && index < mcc$list.size() && mcc$list.get(index) instanceof NumericTag n ? n.getAsDouble() : 0;
    }

    public float getFloatAt(int index) {
        return index >= 0 && index < mcc$list.size() && mcc$list.get(index) instanceof NumericTag n ? n.getAsFloat() : 0;
    }

    public String getStringTagAt(int index) {
        return index >= 0 && index < mcc$list.size() ? mcc$list.get(index).getAsString() : "";
    }

    public int tagCount() {
        return mcc$list.size();
    }

    public int getTagType() {
        return mcc$list.getElementType();
    }

    @Override
    public NBTBase copy() {
        return new NBTTagList(mcc$list.copy());
    }
}
