package legacyconnector.v1_12_2.net.minecraft.network.datasync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;

/**
 * An entity's synchronized values (health, flags, mod data) by key. Kept on the legacy entity; values a mod sets are read back the same
 * way on this side (they are not sent to clients by this layer).
 */
public class EntityDataManager {
    private static final Map<Class<? extends Entity>, Integer> NEXT_ID_MAP = new HashMap<>();
    private final Entity entity;
    private final Map<Integer, DataEntry<?>> entries = new LinkedHashMap<>();
    private boolean empty = true;
    private boolean dirty;

    public EntityDataManager(Entity entity) {
        this.entity = entity;
    }

    /** A new key for the class: the next id after its own last key or its nearest superclass's (ids are unique along a hierarchy). */
    public static synchronized <T> DataParameter<T> createKey(Class<? extends Entity> clazz, DataSerializer<T> serializer) {
        int id;
        if (NEXT_ID_MAP.containsKey(clazz)) {
            id = NEXT_ID_MAP.get(clazz) + 1;
        } else {
            id = 0;
            for (Class<?> c = clazz.getSuperclass(); c != null && c != Object.class; c = c.getSuperclass()) {
                Integer last = NEXT_ID_MAP.get(c);
                if (last != null) {
                    id = last + 1;
                    break;
                }
            }
            // the vanilla keys of the classes above (Entity has 6, EntityLivingBase 5 more, ...) are not created here: start after them
            if (id == 0) id = vanillaKeyCount(clazz);
        }
        if (id > 254) throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is 254)");
        NEXT_ID_MAP.put(clazz, id);
        return serializer.createKey(id);
    }

    /** How many data keys vanilla 1.12.2 declares on the classes above a mod entity class. */
    private static int vanillaKeyCount(Class<?> clazz) {
        int n = 6; // Entity: flags, air, custom name, name visible, silent, no gravity
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            String name = c.getName();
            if (name.endsWith(".net.minecraft.entity.EntityLivingBase")) n += 5;
            else if (name.endsWith(".net.minecraft.entity.EntityLiving")) n += 1;
            else if (name.endsWith(".net.minecraft.entity.EntityAgeable")) n += 1;
            else if (name.endsWith(".net.minecraft.entity.passive.EntityTameable")) n += 2;
            else if (name.endsWith(".net.minecraft.entity.player.EntityPlayer")) n += 6;
        }
        return n;
    }

    public synchronized <T> void register(DataParameter<T> key, T value) {
        if (key instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122<T> v && v.handles(entity)) return;
        int id = key.getId();
        if (id > 254) throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is 254)");
        if (entries.containsKey(id)) throw new IllegalArgumentException("Duplicate id value for " + id + "!");
        entries.put(id, new DataEntry<>(key, value));
        empty = false;
    }

    @SuppressWarnings("unchecked")
    private <T> DataEntry<T> getEntry(DataParameter<T> key) {
        DataEntry<?> e = entries.get(key.getId());
        if (e == null) {
            // vanilla threw for unregistered keys; a mod reading a vanilla key the layer never registered gets a fresh entry
            e = new DataEntry<>(key, null);
            entries.put(key.getId(), e);
        }
        return (DataEntry<T>) e;
    }

    public synchronized <T> T get(DataParameter<T> key) {
        if (key instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122<T> v && v.handles(entity)) return v.read(entity);
        return getEntry(key).getValue();
    }

    public synchronized <T> void set(DataParameter<T> key, T value) {
        if (key instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122<T> v && v.handles(entity)) {
            v.write(entity, value);
            return;
        }
        DataEntry<T> e = getEntry(key);
        if (!java.util.Objects.equals(value, e.getValue())) {
            e.setValue(value);
            e.setDirty(true);
            dirty = true;
        }
    }

    public synchronized <T> void setDirty(DataParameter<T> key) {
        getEntry(key).setDirty(true);
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public static void writeEntries(List<DataEntry<?>> entries, PacketBuffer buf) throws IOException {
        if (entries != null) for (DataEntry<?> e : entries) writeEntry(buf, e);
        buf.writeByte(255);
    }

    public synchronized List<DataEntry<?>> getDirty() {
        List<DataEntry<?>> out = null;
        if (dirty) {
            for (DataEntry<?> e : entries.values()) {
                if (e.isDirty()) {
                    e.setDirty(false);
                    if (out == null) out = new ArrayList<>();
                    out.add(e.copy());
                }
            }
        }
        dirty = false;
        return out;
    }

    public synchronized void writeEntries(PacketBuffer buf) throws IOException {
        for (DataEntry<?> e : entries.values()) writeEntry(buf, e);
        buf.writeByte(255);
    }

    public synchronized List<DataEntry<?>> getAll() {
        List<DataEntry<?>> out = null;
        for (DataEntry<?> e : entries.values()) {
            if (out == null) out = new ArrayList<>();
            out.add(e.copy());
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void writeEntry(PacketBuffer buf, DataEntry<?> entry) {
        DataParameter key = entry.getKey();
        int id = DataSerializers.getSerializerId(key.getSerializer());
        if (id < 0) throw new io.netty.handler.codec.EncoderException("Unknown serializer type " + key.getSerializer());
        buf.writeByte(key.getId());
        buf.writeVarInt(id);
        key.getSerializer().write(buf, entry.getValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<DataEntry<?>> readEntries(PacketBuffer buf) throws IOException {
        List<DataEntry<?>> out = null;
        int id;
        while ((id = buf.readUnsignedByte()) != 255) {
            if (out == null) out = new ArrayList<>();
            int type = buf.readVarInt();
            DataSerializer serializer = DataSerializers.getSerializer(type);
            if (serializer == null) throw new io.netty.handler.codec.DecoderException("Unknown serializer type " + type);
            out.add(new DataEntry(serializer.createKey(id), serializer.read(buf)));
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void setEntryValues(List<DataEntry<?>> list) {
        for (DataEntry<?> e : list) {
            DataEntry existing = entries.get(e.getKey().getId());
            if (existing != null) setEntryValue(existing, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T> void setEntryValue(DataEntry<T> target, DataEntry<?> source) {
        target.setValue((T) source.getValue());
    }

    public boolean isEmpty() {
        return empty;
    }

    public synchronized void setClean() {
        dirty = false;
        for (DataEntry<?> e : entries.values()) e.setDirty(false);
    }

    public Entity mcc$entity() {
        return entity;
    }

    /** One value: its key, the value and whether it changed since the last sync. */
    public static class DataEntry<T> {
        private final DataParameter<T> key;
        private T value;
        private boolean dirty;

        public DataEntry(DataParameter<T> key, T value) {
            this.key = key;
            this.value = value;
            this.dirty = true;
        }

        public DataParameter<T> getKey() {
            return key;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public boolean isDirty() {
            return dirty;
        }

        public void setDirty(boolean dirty) {
            this.dirty = dirty;
        }

        public DataEntry<T> copy() {
            return new DataEntry<>(key, key.getSerializer() == null || value == null ? value : key.getSerializer().copyValue(value));
        }
    }
}
