package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.HashMap;
import java.util.Map;

/** 1.7.10's map keyed by longs (chunk coordinates, block positions). */
public class LongHashMap {
    private final Map<Long, Object> values = new HashMap<>();

    public int getNumHashElements() {
        return values.size();
    }

    public Object getValueByKey(long key) {
        return values.get(key);
    }

    public boolean containsItem(long key) {
        return values.containsKey(key);
    }

    public void add(long key, Object value) {
        values.put(key, value);
    }

    public Object remove(long key) {
        return values.remove(key);
    }
}
