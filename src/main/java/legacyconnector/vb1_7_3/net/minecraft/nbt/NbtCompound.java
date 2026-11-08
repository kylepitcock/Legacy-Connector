package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Beta 1.7.3 NBT compound (tag 10): named tags; absent keys read as 0, "", an empty array, list or compound, as the original did. */
public class NbtCompound extends NbtElement {
    private Map<String, NbtElement> entries = new HashMap<>();

    public NbtCompound() {}

    @Override
    void write(DataOutput out) throws IOException {
        for (NbtElement e : entries.values()) writeTag(e, out);
        out.writeByte(0);
    }

    @Override
    void read(DataInput in) throws IOException {
        entries.clear();
        for (NbtElement e; (e = readTag(in)).getType() != 0; ) entries.put(e.getKey(), e);
    }

    public Collection<NbtElement> values() {
        return entries.values();
    }

    @Override
    public byte getType() {
        return 10;
    }

    public void put(String key, NbtElement value) {
        entries.put(key, value.setKey(key));
    }

    public void putByte(String key, byte value) {
        entries.put(key, new NbtByte(value).setKey(key));
    }

    public void putShort(String key, short value) {
        entries.put(key, new NbtShort(value).setKey(key));
    }

    public void putInt(String key, int value) {
        entries.put(key, new NbtInt(value).setKey(key));
    }

    public void putLong(String key, long value) {
        entries.put(key, new NbtLong(value).setKey(key));
    }

    public void putFloat(String key, float value) {
        entries.put(key, new NbtFloat(value).setKey(key));
    }

    public void putDouble(String key, double value) {
        entries.put(key, new NbtDouble(value).setKey(key));
    }

    public void putString(String key, String value) {
        entries.put(key, new NbtString(value).setKey(key));
    }

    public void putByteArray(String key, byte[] value) {
        entries.put(key, new NbtByteArray(value).setKey(key));
    }

    public void put(String key, NbtCompound value) {
        entries.put(key, value.setKey(key));
    }

    public void putBoolean(String key, boolean value) {
        putByte(key, (byte) (value ? 1 : 0));
    }

    public boolean contains(String key) {
        return entries.containsKey(key);
    }

    public byte getByte(String key) {
        return entries.containsKey(key) ? ((NbtByte) entries.get(key)).value : 0;
    }

    public short getShort(String key) {
        return entries.containsKey(key) ? ((NbtShort) entries.get(key)).value : 0;
    }

    public int getInt(String key) {
        return entries.containsKey(key) ? ((NbtInt) entries.get(key)).value : 0;
    }

    public long getLong(String key) {
        return entries.containsKey(key) ? ((NbtLong) entries.get(key)).value : 0L;
    }

    public float getFloat(String key) {
        return entries.containsKey(key) ? ((NbtFloat) entries.get(key)).value : 0.0F;
    }

    public double getDouble(String key) {
        return entries.containsKey(key) ? ((NbtDouble) entries.get(key)).value : 0.0;
    }

    public String getString(String key) {
        return entries.containsKey(key) ? ((NbtString) entries.get(key)).value : "";
    }

    public byte[] getByteArray(String key) {
        return entries.containsKey(key) ? ((NbtByteArray) entries.get(key)).value : new byte[0];
    }

    public NbtCompound getCompound(String key) {
        return entries.containsKey(key) ? (NbtCompound) entries.get(key) : new NbtCompound();
    }

    public NbtList getList(String key) {
        return entries.containsKey(key) ? (NbtList) entries.get(key) : new NbtList();
    }

    public boolean getBoolean(String key) {
        return getByte(key) != 0;
    }

    @Override
    public String toString() {
        return entries.size() + " entries";
    }
}
