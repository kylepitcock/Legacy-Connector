package legacyconnector.v1_12_2.net.minecraft.util.registry;

/** A namespaced registry whose lookups of unknown names give the default entry. */
public class RegistryNamespacedDefaultedByKey<K, V> extends RegistryNamespaced<K, V> {
    private final K defaultValueKey;
    private V defaultValue;

    public RegistryNamespacedDefaultedByKey(K defaultValueKey) {
        this.defaultValueKey = defaultValueKey;
    }

    @Override
    public synchronized void putObject(K key, V value) {
        if (defaultValueKey.equals(key)) defaultValue = value;
        super.putObject(key, value);
    }

    @Override
    public synchronized V getObject(K name) {
        V v = super.getObject(name);
        return v == null ? defaultValue : v;
    }
}
