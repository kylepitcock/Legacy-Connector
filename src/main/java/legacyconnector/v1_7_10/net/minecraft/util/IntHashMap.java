package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.HashMap;
import java.util.Map;

/** 1.7.10's map keyed by ints (entity ids, packet ids). */
public class IntHashMap {
    private final Map<Integer, Object> values = new HashMap<>();

    public Object lookup(int key) {
        return values.get(key);
    }

    public boolean containsItem(int key) {
        return values.containsKey(key);
    }

    public void addKey(int key, Object value) {
        values.put(key, value);
    }

    public Object removeObject(int key) {
        return values.remove(key);
    }

    public void clearMap() {
        values.clear();
    }
}
