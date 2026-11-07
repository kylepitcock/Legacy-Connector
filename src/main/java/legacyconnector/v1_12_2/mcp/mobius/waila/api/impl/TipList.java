package legacyconnector.v1_12_2.mcp.mobius.waila.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.ITaggedList;

/** The tooltip line list Waila handed to providers: lines can carry tags (AE2 removes lines by tag). */
public class TipList<E, T> extends ArrayList<E> implements ITaggedList<E, T> {
    private final Map<E, Set<T>> tags = new LinkedHashMap<>();

    @Override
    public boolean add(E e, T tag) {
        boolean r = add(e);
        addTag(e, tag);
        return r;
    }

    @Override
    public boolean add(E e, Collection<? extends T> tagList) {
        boolean r = add(e);
        for (T t : tagList) addTag(e, t);
        return r;
    }

    @Override
    public Set<T> getTags(E e) {
        Set<T> s = tags.get(e);
        return s == null ? new HashSet<>() : s;
    }

    @Override
    public Set<T> getTags(int index) {
        return getTags(get(index));
    }

    @Override
    public void addTag(E e, T tag) {
        tags.computeIfAbsent(e, k -> new HashSet<>()).add(tag);
    }

    @Override
    public void addTag(int index, T tag) {
        addTag(get(index), tag);
    }

    @Override
    public void removeTag(E e, T tag) {
        Set<T> s = tags.get(e);
        if (s != null) s.remove(tag);
    }

    @Override
    public void removeTag(int index, T tag) {
        removeTag(get(index), tag);
    }

    @Override
    public Set<E> getEntries(T tag) {
        Set<E> out = new HashSet<>();
        for (var e : tags.entrySet()) if (e.getValue().contains(tag)) out.add(e.getKey());
        return out;
    }

    @Override
    public void removeEntries(T tag) {
        for (E e : getEntries(tag)) {
            remove(e);
            tags.remove(e);
        }
    }

    @Override
    public String getTagsAsString(E e) {
        return String.valueOf(getTags(e));
    }
}
