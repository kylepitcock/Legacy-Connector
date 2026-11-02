package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 1.12.2's identity hash map from objects to ints with a reverse array: {@code byId[id]} is the object of an id (a block state
 * palette is read straight from that array), {@code values}/{@code intKeys} are the open-addressed table.
 */
public class IntIdentityHashBiMap<K> implements Iterable<K> {
    public static final Object EMPTY = null;
    private K[] values;
    private int[] intKeys;
    private K[] byId;
    private int nextFreeIndex;
    private int mapSize;

    @SuppressWarnings("unchecked")
    public IntIdentityHashBiMap(int initialCapacity) {
        initialCapacity = Math.max(4, (int) ((float) initialCapacity / 0.8F));
        this.values = (K[]) new Object[initialCapacity];
        this.intKeys = new int[initialCapacity];
        this.byId = (K[]) new Object[initialCapacity];
    }

    /** The id of the object, or -1. */
    public int getId(K target) {
        int index = getIndex(target, hashObject(target));
        return index == -1 ? -1 : intKeys[index];
    }

    public K get(int idIn) {
        return idIn >= 0 && idIn < byId.length ? byId[idIn] : null;
    }

    /** Adds the object under the next free id and returns it. */
    public int add(K objectIn) {
        int i = nextId();
        put(objectIn, i);
        return i;
    }

    private int nextId() {
        while (nextFreeIndex < byId.length && byId[nextFreeIndex] != null) ++nextFreeIndex;
        return nextFreeIndex;
    }

    @SuppressWarnings("unchecked")
    private void grow(int capacity) {
        K[] oldValues = values;
        int[] oldKeys = intKeys;
        K[] oldById = byId;
        values = (K[]) new Object[capacity];
        intKeys = new int[capacity];
        byId = (K[]) new Object[capacity];
        nextFreeIndex = 0;
        mapSize = 0;
        for (int i = 0; i < oldValues.length; ++i) if (oldValues[i] != null && oldById[oldKeys[i]] == oldValues[i]) put(oldValues[i], oldKeys[i]);
    }

    public void put(K objectIn, int intKey) {
        if (objectIn == null) throw new NullPointerException();
        int existing = getIndex(objectIn, hashObject(objectIn));
        if (existing != -1) {
            // already in: it moves to the new id
            int oldId = intKeys[existing];
            if (oldId == intKey) return;
            if (intKey >= byId.length) grow(Math.max(byId.length << 1, intKey + 1));
            existing = getIndex(objectIn, hashObject(objectIn));
            byId[oldId] = null;
            if (oldId < nextFreeIndex) nextFreeIndex = oldId;
            intKeys[existing] = intKey;
            byId[intKey] = objectIn;
            return;
        }
        int needed = Math.max(intKey, mapSize + 1);
        if ((float) needed >= (float) values.length * 0.8F) {
            int capacity = values.length << 1;
            while (capacity <= intKey) capacity <<= 1;
            grow(capacity);
        }
        int k = findEmpty(hashObject(objectIn));
        values[k] = objectIn;
        intKeys[k] = intKey;
        byId[intKey] = objectIn;
        ++mapSize;
        if (intKey == nextFreeIndex) ++nextFreeIndex;
    }

    private int hashObject(K object) {
        int h = System.identityHashCode(object);
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        return (h & Integer.MAX_VALUE) % values.length;
    }

    private int getIndex(K object, int start) {
        for (int i = start; i < values.length; ++i) {
            if (values[i] == object) return i;
            if (values[i] == EMPTY) return -1;
        }
        for (int j = 0; j < start; ++j) {
            if (values[j] == object) return j;
            if (values[j] == EMPTY) return -1;
        }
        return -1;
    }

    private int findEmpty(int start) {
        for (int i = start; i < values.length; ++i) if (values[i] == EMPTY) return i;
        for (int j = 0; j < start; ++j) if (values[j] == EMPTY) return j;
        throw new RuntimeException("Overflowed :(");
    }

    @Override
    public Iterator<K> iterator() {
        final K[] ids = byId;
        return new Iterator<K>() {
            private int next = advance(0);

            private int advance(int from) {
                while (from < ids.length && ids[from] == null) from++;
                return from;
            }

            @Override
            public boolean hasNext() {
                return next < ids.length;
            }

            @Override
            public K next() {
                if (next >= ids.length) throw new NoSuchElementException();
                K k = ids[next];
                next = advance(next + 1);
                return k;
            }
        };
    }

    public void clear() {
        Arrays.fill(values, null);
        Arrays.fill(byId, null);
        nextFreeIndex = 0;
        mapSize = 0;
    }

    public int size() {
        return mapSize;
    }
}
