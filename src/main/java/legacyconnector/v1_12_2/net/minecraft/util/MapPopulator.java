package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapPopulator {
    public static <K, V> Map<K, V> createMap(Iterable<K> keys, Iterable<V> values) {
        Map<K, V> map = new LinkedHashMap<>();
        Iterator<V> it = values.iterator();
        for (K key : keys) {
            if (!it.hasNext()) break;
            map.put(key, it.next());
        }
        return map;
    }
}
