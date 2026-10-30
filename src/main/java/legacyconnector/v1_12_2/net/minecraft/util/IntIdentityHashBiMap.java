package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Objects (by identity) to ids and back: {@link #add} gives the next free id; ids are what block state palettes store. */
public class IntIdentityHashBiMap<K> implements Iterable<K> {
    private final Map<K, Integer> ids;
    private final List<K> byId;
    private int nextFreeIndex;

    public IntIdentityHashBiMap(int initialCapacity) {
        this.ids = new IdentityHashMap<>(initialCapacity);
        this.byId = new ArrayList<>(initialCapacity);
    }

    /** The id of the object, or -1. */
    public int getId(K value) {
        Integer id = ids.get(value);
        return id == null ? -1 : id;
    }

    public K get(int idIn) {
        return idIn >= 0 && idIn < byId.size() ? byId.get(idIn) : null;
    }

    /** Adds the object under the next free id and returns it. */
    public int add(K value) {
        int id = nextId();
        put(value, id);
        return id;
    }

    private int nextId() {
        while (nextFreeIndex < byId.size() && byId.get(nextFreeIndex) != null) nextFreeIndex++;
        return nextFreeIndex;
    }

    public void put(K value, int intKey) {
        Objects.requireNonNull(value);
        while (byId.size() <= intKey) byId.add(null);
        K previous = byId.get(intKey);
        if (previous != null) ids.remove(previous);
        Integer old = ids.put(value, intKey);
        if (old != null && old != intKey) byId.set(old, null);
        byId.set(intKey, value);
        if (nextFreeIndex == intKey) nextFreeIndex++;
    }

    @Override
    public Iterator<K> iterator() {
        List<K> present = new ArrayList<>(ids.size());
        for (K k : byId) if (k != null) present.add(k);
        return present.iterator();
    }

    public void clear() {
        ids.clear();
        byId.clear();
        nextFreeIndex = 0;
    }

    public int size() {
        return ids.size();
    }
}
