package legacyconnector.v1_12_2.net.minecraft.util.registry;

/** A registry that answers a default object for keys nothing was registered under (dispenser behaviours): never null. */
public class RegistryDefaulted<K, V> extends RegistrySimple<K, V> {
    protected final V defaultObject;

    public RegistryDefaulted(V defaultObjectIn) {
        this.defaultObject = defaultObjectIn;
    }

    @Override
    public V getObject(K name) {
        V v = super.getObject(name);
        return v == null ? defaultObject : v;
    }
}
