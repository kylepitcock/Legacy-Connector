package legacyconnector.v1_12_2.net.minecraft.util.registry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** A registry that also numbers its entries (in registration order). */
public class RegistryNamespaced<K, V> extends RegistrySimple<K, V> {
    protected final List<V> underlyingIntegerMap = new ArrayList<>();
    protected final Map<V, K> inverseObjectRegistry = new IdentityHashMap<>();

    @Override
    public synchronized void putObject(K key, V value) {
        if (!registryObjects.containsKey(key)) underlyingIntegerMap.add(value);
        super.putObject(key, value);
        inverseObjectRegistry.put(value, key);
    }

    public synchronized K getNameForObject(V value) {
        return inverseObjectRegistry.get(value);
    }

    public synchronized int getIDForObject(V value) {
        return underlyingIntegerMap.indexOf(value);
    }

    public synchronized V getObjectById(int id) {
        return id >= 0 && id < underlyingIntegerMap.size() ? underlyingIntegerMap.get(id) : null;
    }
}
