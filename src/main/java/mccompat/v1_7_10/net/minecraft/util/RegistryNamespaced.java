package mccompat.v1_7_10.net.minecraft.util;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Read-mostly facade over the modern registries, keyed by legacy names ({@code "minecraft:stone"},
 * {@code "modid:block_name"}) and legacy numeric ids.
 */
public class RegistryNamespaced extends RegistrySimple implements Iterable<Object> {
    private final Function<String, Object> byName;
    private final Function<Object, String> nameOf;
    private final IntFunction<Object> byId;
    private final ToIntFunction<Object> idOf;
    private final Supplier<Iterator<Object>> all;

    public RegistryNamespaced(Function<String, Object> byName, Function<Object, String> nameOf, IntFunction<Object> byId,
                              ToIntFunction<Object> idOf, Supplier<Iterator<Object>> all) {
        this.byName = byName;
        this.nameOf = nameOf;
        this.byId = byId;
        this.idOf = idOf;
        this.all = all;
    }

    public Object getObject(String name) {
        return byName.apply(ensureNamespaced(name));
    }

    @Override
    public Object getObject(Object name) {
        return getObject(String.valueOf(name));
    }

    /**
     * Live name -> object view for code that reads {@code registryObjects} directly. Writes are accepted but only
     * visible through this map (the modern registries are not rewritten).
     */
    @Override
    public java.util.Map<Object, Object> createUnderlyingMap() {
        return new LiveMap();
    }

    /**
     * Live name -> object view for code that reads {@code registryObjects} directly; a BiMap like the legacy
     * HashBiMap. Writes are accepted but only visible through this map (the modern registries are not rewritten).
     */
    private final class LiveMap extends java.util.AbstractMap<Object, Object> implements com.google.common.collect.BiMap<Object, Object> {
        private final java.util.Map<Object, Object> overrides = new java.util.HashMap<>();

        @Override
        public Object get(Object key) {
            Object o = overrides.get(key);
            return o != null ? o : key == null ? null : byName.apply(ensureNamespaced(String.valueOf(key)));
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public Object put(Object key, Object value) {
            Object old = get(key);
            overrides.put(key, value);
            return old;
        }

        @Override
        public Object forcePut(Object key, Object value) {
            return put(key, value);
        }

        private java.util.Map<Object, Object> snapshot() {
            java.util.Map<Object, Object> m = new java.util.LinkedHashMap<>();
            for (Iterator<Object> it = all.get(); it.hasNext(); ) {
                Object o = it.next();
                String n = nameOf.apply(o);
                if (n != null) m.put(n, o);
            }
            m.putAll(overrides);
            return m;
        }

        @Override
        public java.util.Set<Entry<Object, Object>> entrySet() {
            return snapshot().entrySet();
        }

        @Override
        public java.util.Set<Object> values() {
            return new java.util.LinkedHashSet<>(snapshot().values());
        }

        @Override
        public com.google.common.collect.BiMap<Object, Object> inverse() {
            com.google.common.collect.BiMap<Object, Object> inv = com.google.common.collect.HashBiMap.create();
            snapshot().forEach((k, v) -> inv.forcePut(v, k));
            return inv;
        }
    }

    @Override
    public boolean containsKey(Object name) {
        return containsKey(String.valueOf(name));
    }

    public int getIDForObject(Object obj) {
        return idOf.applyAsInt(obj);
    }

    public Object getObjectById(int id) {
        return byId.apply(id);
    }

    public boolean containsId(int id) {
        return byId.apply(id) != null;
    }

    public void addObject(int id, String name, Object obj) {
        throw new UnsupportedOperationException("Direct registry writes are not supported; use GameRegistry");
    }

    @Override
    public Iterator<Object> iterator() {
        return all.get();
    }

    protected static String ensureNamespaced(String name) {
        return name.indexOf(':') == -1 ? "minecraft:" + name : name;
    }
}
