package legacyconnector.v1_12_2.net.minecraft.util;

/** A map from ints to values. */
public class IntHashMap<V> {
    private final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<V> map = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();

    public IntHashMap() {
    }

    public V lookup(int hashEntry) {
        return map.get(hashEntry);
    }

    public boolean containsItem(int hashEntry) {
        return map.containsKey(hashEntry);
    }

    public void addKey(int hashEntry, V valueEntry) {
        map.put(hashEntry, valueEntry);
    }

    public V removeObject(int o) {
        return map.remove(o);
    }

    public void clearMap() {
        map.clear();
    }
}
