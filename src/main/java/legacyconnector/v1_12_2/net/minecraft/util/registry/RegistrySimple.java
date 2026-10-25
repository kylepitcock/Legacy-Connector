package legacyconnector.v1_12_2.net.minecraft.util.registry;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RegistrySimple<K, V> implements IRegistry<K, V> {
    protected final Map<K, V> registryObjects = new LinkedHashMap<>();

    @Override
    public V getObject(K name) {
        return registryObjects.get(name);
    }

    @Override
    public void putObject(K key, V value) {
        registryObjects.put(key, value);
    }

    @Override
    public Set<K> getKeys() {
        return java.util.Collections.unmodifiableSet(registryObjects.keySet());
    }

    public boolean containsKey(K key) {
        return registryObjects.containsKey(key);
    }

    @Override
    public Iterator<V> iterator() {
        return registryObjects.values().iterator();
    }
}
