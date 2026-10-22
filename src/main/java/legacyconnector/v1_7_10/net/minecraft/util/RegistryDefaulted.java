package legacyconnector.v1_7_10.net.minecraft.util;

/** A registry that answers a default object for keys nothing was registered under (dispenser behaviours). */
public class RegistryDefaulted extends RegistrySimple {
    private final Object defaultObject;

    public RegistryDefaulted(Object defaultObject) {
        this.defaultObject = defaultObject;
    }

    @Override
    public Object getObject(Object key) {
        Object value = super.getObject(key);
        return value == null ? defaultObject : value;
    }
}
