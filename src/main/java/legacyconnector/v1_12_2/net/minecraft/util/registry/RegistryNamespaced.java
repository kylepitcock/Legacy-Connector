package legacyconnector.v1_12_2.net.minecraft.util.registry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** A registry that also numbers its entries: the next free id in registration order, or the id given to {@link #register}. */
public class RegistryNamespaced<K, V> extends RegistrySimple<K, V> {
    protected final List<V> underlyingIntegerMap = new ArrayList<>();
    protected final Map<V, K> inverseObjectRegistry = new IdentityHashMap<>();
    /** Object to id (lookups stay O(1)). */
    private final Map<V, Integer> mcc$ids = new IdentityHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static RegistryNamespaced mcc$synthDefault(String key) {
        return RegistryNamespacedDefaultedByKey.mcc$synthDefault(key);
    }

    /** Registers the object under the key with that numeric id. */
    public synchronized void register(int id, K key, V value) {
        while (underlyingIntegerMap.size() <= id) underlyingIntegerMap.add(null);
        V previous = underlyingIntegerMap.get(id);
        if (previous != null) mcc$ids.remove(previous);
        underlyingIntegerMap.set(id, value);
        mcc$ids.put(value, id);
        super.putObject(key, value);
        inverseObjectRegistry.put(value, key);
    }

    @Override
    public synchronized void putObject(K key, V value) {
        if (!registryObjects.containsKey(key)) {
            register(underlyingIntegerMap.size(), key, value);
            return;
        }
        super.putObject(key, value);
        inverseObjectRegistry.put(value, key);
    }

    public synchronized K getNameForObject(V value) {
        return inverseObjectRegistry.get(value);
    }

    public synchronized int getIDForObject(V value) {
        Integer id = mcc$ids.get(value);
        return id == null ? -1 : id;
    }

    public synchronized V getObjectById(int id) {
        return id >= 0 && id < underlyingIntegerMap.size() ? underlyingIntegerMap.get(id) : null;
    }
}
