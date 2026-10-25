package legacyconnector.v1_12_2.net.minecraftforge.common.property;

/** A property that is not part of the saved state (rendering data a tile entity supplies). */
public interface IUnlistedProperty<V> {
    String getName();

    boolean isValid(V value);

    Class<V> getType();

    String valueToString(V value);
}
