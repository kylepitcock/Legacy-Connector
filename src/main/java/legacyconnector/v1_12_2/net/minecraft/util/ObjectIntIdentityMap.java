package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

/** A two-way map between objects (by identity) and small ints, as 1.12.2's (block state ids, data serializer ids). */
public class ObjectIntIdentityMap<T> implements Iterable<T> {
    protected final IdentityHashMap<T, Integer> identityMap;
    protected final List<T> objectList;

    public ObjectIntIdentityMap() {
        this(512);
    }

    public ObjectIntIdentityMap(int expectedSize) {
        this.objectList = new ArrayList<>(expectedSize);
        this.identityMap = new IdentityHashMap<>(expectedSize);
    }

    public void put(T key, int value) {
        identityMap.put(key, value);
        while (objectList.size() <= value) objectList.add(null);
        objectList.set(value, key);
    }

    public int get(T key) {
        Integer i = identityMap.get(key);
        return i == null ? -1 : i;
    }

    public T getByValue(int value) {
        return value >= 0 && value < objectList.size() ? objectList.get(value) : null;
    }

    @Override
    public Iterator<T> iterator() {
        return objectList.stream().filter(java.util.Objects::nonNull).iterator();
    }

    public int size() {
        return identityMap.size();
    }
}
