package legacyconnector.v1_12_2.net.minecraft.network.datasync;

/** The key of one synchronized entity value: an id unique in its entity class hierarchy and the serializer of its type. */
public class DataParameter<T> {
    private final int id;
    private final DataSerializer<T> serializer;

    public DataParameter(int id, DataSerializer<T> serializer) {
        this.id = id;
        this.serializer = serializer;
    }

    public int getId() {
        return id;
    }

    public DataSerializer<T> getSerializer() {
        return serializer;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DataParameter<?> p && id == p.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
