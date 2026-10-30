package legacyconnector.v1_12_2.net.minecraft.util.registry;

/** A simple registry whose lookups of unknown keys give a default object. */
public class RegistryDefaulted<K, V> extends RegistrySimple<K, V> {
    private final V defaultObject;

    public RegistryDefaulted(V defaultObjectIn) {
        this.defaultObject = defaultObjectIn;
    }

    @Override
    public V getObject(K name) {
        V v = super.getObject(name);
        return v == null ? defaultObject : v;
    }
}
