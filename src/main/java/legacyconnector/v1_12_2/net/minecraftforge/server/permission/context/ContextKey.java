package legacyconnector.v1_12_2.net.minecraftforge.server.permission.context;

/** A typed key of a permission context value. */
public final class ContextKey<T> {
    private final String ID;
    private final Class<T> typeClass;

    private ContextKey(String id, Class<T> c) {
        this.ID = id;
        this.typeClass = c;
    }

    public static <E> ContextKey<E> create(String id, Class<E> c) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("ID can't be blank!");
        return new ContextKey<>(id, c);
    }

    @Override
    public String toString() {
        return "ContextKey:" + ID;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ContextKey<?> k && ID.equals(k.ID);
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    public String name() {
        return ID;
    }

    public Class<T> getType() {
        return typeClass;
    }
}
