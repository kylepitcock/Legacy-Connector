package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtByte;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtByteArray;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtDouble;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtElement;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtFloat;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtInt;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtList;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtLong;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtShort;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtString;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** b1.7.3 NBT and modern NBT, both ways (same tag types 1-10, so nothing is lost). */
public final class NbtB173 {
    private NbtB173() {}

    public static Tag toModern(NbtElement e) {
        if (e instanceof NbtCompound c) {
            CompoundTag out = new CompoundTag();
            for (NbtElement v : c.values()) {
                Tag t = toModern(v);
                if (t != null) out.put(v.getKey(), t);
            }
            return out;
        }
        if (e instanceof NbtList l) {
            ListTag out = new ListTag();
            for (int i = 0; i < l.size(); i++) {
                Tag t = toModern(l.get(i));
                if (t != null) out.add(t);
            }
            return out;
        }
        if (e instanceof NbtByte b) return ByteTag.valueOf(b.value);
        if (e instanceof NbtShort s) return ShortTag.valueOf(s.value);
        if (e instanceof NbtInt i) return IntTag.valueOf(i.value);
        if (e instanceof NbtLong l) return LongTag.valueOf(l.value);
        if (e instanceof NbtFloat f) return FloatTag.valueOf(f.value);
        if (e instanceof NbtDouble d) return DoubleTag.valueOf(d.value);
        if (e instanceof NbtByteArray a) return new ByteArrayTag(a.value == null ? new byte[0] : a.value);
        if (e instanceof NbtString s) return StringTag.valueOf(s.value == null ? "" : s.value);
        return null;
    }

    public static CompoundTag toModern(NbtCompound c) {
        return (CompoundTag) toModern((NbtElement) c);
    }

    public static NbtElement fromModern(Tag t) {
        if (t instanceof CompoundTag c) {
            NbtCompound out = new NbtCompound();
            for (String key : c.getAllKeys()) {
                NbtElement v = fromModern(c.get(key));
                if (v != null) out.put(key, v);
            }
            return out;
        }
        if (t instanceof ListTag l) {
            NbtList out = new NbtList();
            for (Tag v : l) {
                NbtElement e = fromModern(v);
                if (e != null) out.add(e);
            }
            return out;
        }
        if (t instanceof ByteTag b) return new NbtByte(b.getAsByte());
        if (t instanceof ShortTag s) return new NbtShort(s.getAsShort());
        if (t instanceof IntTag i) return new NbtInt(i.getAsInt());
        if (t instanceof LongTag l) return new NbtLong(l.getAsLong());
        if (t instanceof FloatTag f) return new NbtFloat(f.getAsFloat());
        if (t instanceof DoubleTag d) return new NbtDouble(d.getAsDouble());
        if (t instanceof ByteArrayTag a) return new NbtByteArray(a.getAsByteArray());
        if (t instanceof StringTag s) return new NbtString(s.getAsString());
        return null;
    }

    public static NbtCompound fromModern(CompoundTag c) {
        return (NbtCompound) fromModern((Tag) c);
    }
}
