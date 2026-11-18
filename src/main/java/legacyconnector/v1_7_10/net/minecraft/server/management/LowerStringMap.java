package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** A string-keyed map that ignores the case of its keys (1.7.10 {@code LowerStringMap}): the command and player-data lookups use it. */
public class LowerStringMap implements Map {
    private final Map internalMap = Maps.newLinkedHashMap();

    @Override
    public int size() {
        return internalMap.size();
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return internalMap.containsKey(key.toString().toLowerCase());
    }

    @Override
    public boolean containsValue(Object value) {
        return internalMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return internalMap.get(key.toString().toLowerCase());
    }

    public Object put(String key, Object value) {
        return internalMap.put(key.toLowerCase(), value);
    }

    @Override
    public Object put(Object key, Object value) {
        return put((String) key, value);
    }

    @Override
    public Object remove(Object key) {
        return internalMap.remove(key.toString().toLowerCase());
    }

    @Override
    public void putAll(Map map) {
        for (Object o : map.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            put((String) entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        internalMap.clear();
    }

    @Override
    public Set keySet() {
        return internalMap.keySet();
    }

    @Override
    public Collection values() {
        return internalMap.values();
    }

    @Override
    public Set entrySet() {
        return internalMap.entrySet();
    }
}
