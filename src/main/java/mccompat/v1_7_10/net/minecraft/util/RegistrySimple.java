package mccompat.v1_7_10.net.minecraft.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Legacy key -> object registry. Superclass of {@link RegistryNamespaced}, as in 1.7.10. */
public class RegistrySimple {
    protected final Map<Object, Object> registryObjects = createUnderlyingMap();

    public Map<Object, Object> createUnderlyingMap() {
        return new HashMap<>();
    }

    public Object getObject(Object key) {
        return registryObjects.get(key);
    }

    public void putObject(Object key, Object value) {
        registryObjects.put(key, value);
    }

    public Set<Object> getKeys() {
        return registryObjects.keySet();
    }

    public boolean containsKey(Object key) {
        return registryObjects.containsKey(key);
    }
}
