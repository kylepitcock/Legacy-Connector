package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.12.2 set of objects that also answers "all members of class C" quickly (chunk entity lists, {@code Chunk.getEntityLists}).
 * Added by area W (chunks) as the minimum its chunk views need; it behaves like vanilla's.
 */
public class ClassInheritanceMultiMap<T> extends AbstractSet<T> {
    private static final Set<Class<?>> ALL_KNOWN = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final Map<Class<?>, List<T>> map = new HashMap<>();
    private final Set<Class<?>> knownTypes = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Class<T> baseClass;
    private final List<T> values = new ArrayList<>();

    public ClassInheritanceMultiMap(Class<T> baseClass) {
        this.baseClass = baseClass;
        this.knownTypes.add(baseClass);
        this.map.put(baseClass, this.values);
        for (Class<?> c : ALL_KNOWN) createLookup(c);
    }

    protected void createLookup(Class<?> clazz) {
        ALL_KNOWN.add(clazz);
        for (T value : this.values) {
            if (clazz.isAssignableFrom(value.getClass())) addForClass(value, clazz);
        }
        this.knownTypes.add(clazz);
    }

    protected Class<?> initializeClassLookup(Class<?> clazz) {
        if (this.baseClass.isAssignableFrom(clazz)) {
            if (!this.knownTypes.contains(clazz)) createLookup(clazz);
            return clazz;
        }
        throw new IllegalArgumentException("Don't know how to search for " + clazz);
    }

    @Override
    public boolean add(T value) {
        for (Class<?> c : this.knownTypes) {
            if (c.isAssignableFrom(value.getClass())) addForClass(value, c);
        }
        return true;
    }

    private void addForClass(T value, Class<?> parentClass) {
        List<T> list = this.map.get(parentClass);
        if (list == null) this.map.put(parentClass, new ArrayList<>(List.of(value)));
        else list.add(value);
    }

    @Override
    public boolean remove(Object value) {
        boolean removed = false;
        for (Class<?> c : this.knownTypes) {
            if (c.isAssignableFrom(value.getClass())) {
                List<T> list = this.map.get(c);
                if (list != null && list.remove(value)) removed = true;
            }
        }
        return removed;
    }

    @Override
    public boolean contains(Object value) {
        return value != null && this.map.getOrDefault(initializeClassLookup(value.getClass()), List.of()).contains(value);
    }

    @SuppressWarnings("unchecked")
    public <S> Iterable<S> getByClass(Class<S> clazz) {
        return () -> {
            List<T> list = this.map.get(initializeClassLookup(clazz));
            if (list == null) return Collections.emptyIterator();
            return (Iterator<S>) Collections.unmodifiableList(list).iterator();
        };
    }

    @Override
    public Iterator<T> iterator() {
        return this.values.isEmpty() ? Collections.emptyIterator() : Collections.unmodifiableList(this.values).iterator();
    }

    @Override
    public int size() {
        return this.values.size();
    }
}
