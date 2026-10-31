package org.kylepitcock.legacyconnector.compat.architectury;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * An {@link ArrayList} that other threads may add to while one thread iterates it: changes are synchronized and iteration walks a
 * snapshot. Used for Architectury's event listener lists (org.kylepitcock.legacyconnector.mixin.compat.ArchitecturyEventImplMixin).
 */
public final class ThreadSafeListenerList<T> extends ArrayList<T> {
    private volatile Object[] snapshot = new Object[0];

    private void refresh() {
        snapshot = super.toArray();
    }

    @Override
    public synchronized boolean add(T t) {
        boolean r = super.add(t);
        refresh();
        return r;
    }

    @Override
    public synchronized void add(int index, T t) {
        super.add(index, t);
        refresh();
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c) {
        boolean r = super.addAll(c);
        refresh();
        return r;
    }

    @Override
    public synchronized boolean remove(Object o) {
        boolean r = super.remove(o);
        refresh();
        return r;
    }

    @Override
    public synchronized T remove(int index) {
        T r = super.remove(index);
        refresh();
        return r;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        refresh();
    }

    @Override
    public synchronized T set(int index, T t) {
        T r = super.set(index, t);
        refresh();
        return r;
    }

    @Override
    public synchronized void trimToSize() {
        super.trimToSize();
    }

    @Override
    public int size() {
        return snapshot.length;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        return (T) snapshot[index];
    }

    @Override
    public boolean isEmpty() {
        return snapshot.length == 0;
    }

    @Override
    public synchronized boolean contains(Object o) {
        return super.contains(o);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        return (Iterator<T>) List.of(snapshot).iterator();
    }

    @Override
    public Object[] toArray() {
        return snapshot.clone();
    }
}
