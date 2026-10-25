package legacyconnector.v1_12_2.net.minecraft.util.registry;

import java.util.Set;

public interface IRegistry<K, V> extends Iterable<V> {
    V getObject(K name);

    void putObject(K key, V value);

    Set<K> getKeys();
}
