package legacyconnector.v1_12_2.mezz.jei.collect;

/** JEI 4 (Minecraft 1.12.2) internals some plugins reach into; here a small working copy. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public class ListMultiMap<K, V> {
    private final com.google.common.collect.ListMultimap<K, V> map = com.google.common.collect.ArrayListMultimap.create();

    public boolean put(K key, V value) {
        return map.put(key, value);
    }

    public java.util.List<V> get(K key) {
        return map.get(key);
    }

    public com.google.common.collect.ImmutableMultimap<K, V> toImmutable() {
        return com.google.common.collect.ImmutableListMultimap.copyOf(map);
    }
}
