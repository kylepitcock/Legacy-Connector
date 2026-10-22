package mccompat.v1_12_2.net.minecraft.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.Validate;

public class NonNullList<E> extends AbstractList<E> {
    private final List<E> delegate;
    private final E defaultElement;

    public static <E> NonNullList<E> create() {
        return new NonNullList<>();
    }

    public static <E> NonNullList<E> withSize(int size, E fill) {
        Validate.notNull(fill);
        Object[] objects = new Object[size];
        Arrays.fill(objects, fill);
        return new NonNullList<>((List<E>) Arrays.asList(objects), fill);
    }

    @SafeVarargs
    public static <E> NonNullList<E> from(E defaultElementIn, E... elements) {
        return new NonNullList<>(Arrays.asList(elements), defaultElementIn);
    }

    protected NonNullList() {
        this(new java.util.ArrayList<>(), null);
    }

    protected NonNullList(List<E> delegateIn, E listType) {
        this.delegate = delegateIn;
        this.defaultElement = listType;
    }

    @Override
    public E get(int index) {
        return delegate.get(index);
    }

    @Override
    public E set(int index, E element) {
        Validate.notNull(element);
        return delegate.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        Validate.notNull(element);
        delegate.add(index, element);
    }

    @Override
    public E remove(int index) {
        return delegate.remove(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void clear() {
        if (defaultElement == null) {
            super.clear();
        } else {
            for (int i = 0; i < size(); i++) set(i, defaultElement);
        }
    }
}
