package legacyconnector.vb1_7_3.net.minecraft.entity.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Beta 1.7.3 {@code DataTracker} (the entity's synced byte/short/int/string slots, ids 0 to 31). Held per entity; mods store state flags in it
 * ({@code dataTracker.startTracking(16, (byte) 0)}) and read them back. Values are kept locally; the host's own tracking sends what clients need.
 */
public class DataTracker {
    private final Map<Integer, Object> values = new TreeMap<>();
    private final Map<Integer, Boolean> dirtyIds = new TreeMap<>();
    private boolean dirty;

    public void startTracking(int id, Object value) {
        if (id > 31 || id < 0) throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is 31)");
        if (values.containsKey(id)) throw new IllegalArgumentException("Duplicate id value for " + id + "!");
        values.put(id, value);
    }

    public byte getByte(int id) { Object v = values.get(id); return v instanceof Number n ? n.byteValue() : 0; }
    public short getShort(int id) { Object v = values.get(id); return v instanceof Number n ? n.shortValue() : 0; }
    public int getInt(int id) { Object v = values.get(id); return v instanceof Number n ? n.intValue() : 0; }
    public float getFloat(int id) { Object v = values.get(id); return v instanceof Number n ? n.floatValue() : 0; }
    public String getString(int id) { Object v = values.get(id); return v == null ? "" : v.toString(); }

    public void set(int id, Object value) {
        Object old = values.get(id);
        if (old == null || !old.equals(value)) {
            values.put(id, value);
            dirtyIds.put(id, Boolean.TRUE);
            dirty = true;
        }
    }

    public boolean isDirty() { return dirty; }

    public ArrayList<Object> getDirtyEntries() {
        ArrayList<Object> out = new ArrayList<>();
        if (dirty) for (int id : dirtyIds.keySet()) out.add(values.get(id));
        dirtyIds.clear();
        dirty = false;
        return out;
    }
}
