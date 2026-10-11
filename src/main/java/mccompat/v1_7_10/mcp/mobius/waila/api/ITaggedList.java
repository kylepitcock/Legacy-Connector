package mccompat.v1_7_10.mcp.mobius.waila.api;

import java.util.List;
import java.util.Set;

/** A tooltip line list whose entries can carry tags. Waila passed one as the "currenttip" argument. */
public interface ITaggedList<E, T> extends List<E> {
    boolean add(E e, T tag);

    boolean add(E e, java.util.Collection<? extends T> tags);

    Set<T> getTags(E e);

    Set<T> getTags(int index);

    void addTag(E e, T tag);

    void addTag(int index, T tag);

    void removeTag(E e, T tag);

    void removeTag(int index, T tag);

    Set<E> getEntries(T tag);

    void removeEntries(T tag);

    String getTagsAsString(E e);
}
