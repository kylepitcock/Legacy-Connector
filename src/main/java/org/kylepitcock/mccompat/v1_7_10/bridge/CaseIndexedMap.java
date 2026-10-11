package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Insertion-ordered map with a second, lower-cased index: legacy ids are case-insensitive ("IC2:blockOre"), and mods
 * look up thousands of names that do not exist (an integration for an absent mod) - a scan per miss is far too slow.
 */
final class CaseIndexedMap<V> extends LinkedHashMap<String, V> {
    private static final long serialVersionUID = 1L;
    private final transient Map<String, V> lower = new HashMap<>();

    @Override
    public V put(String key, V value) {
        lower.putIfAbsent(key.toLowerCase(Locale.ROOT), value);
        return super.put(key, value);
    }

    @Override
    public V putIfAbsent(String key, V value) {
        V existing = super.putIfAbsent(key, value);
        if (existing == null) lower.putIfAbsent(key.toLowerCase(Locale.ROOT), value);
        return existing;
    }

    @Override
    public V remove(Object key) {
        V old = super.remove(key);
        if (old != null && key instanceof String s) lower.remove(s.toLowerCase(Locale.ROOT), old);
        return old;
    }

    @Override
    public void clear() {
        super.clear();
        lower.clear();
    }

    V getIgnoreCase(String key) {
        return lower.get(key.toLowerCase(Locale.ROOT));
    }
}
