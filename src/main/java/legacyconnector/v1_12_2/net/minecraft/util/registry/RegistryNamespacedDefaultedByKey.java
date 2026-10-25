package legacyconnector.v1_12_2.net.minecraft.util.registry;

/** A namespaced registry whose lookups of unknown names give the default entry. */
public class RegistryNamespacedDefaultedByKey<K, V> extends RegistryNamespaced<K, V> {
    private final K defaultValueKey;
    private V defaultValue;

    /** Vanilla registries held in static fields of synthesized classes (PotionType.REGISTRY): views of the registries the layer keeps. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static RegistryNamespacedDefaultedByKey mcc$synthDefault(String key) {
        if (key.equals("net.minecraft.potion.PotionType.REGISTRY")) {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTION_TYPES, "empty");
        }
        return new RegistryNamespacedDefaultedByKey(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation("air"));
    }

    public RegistryNamespacedDefaultedByKey(K defaultValueKey) {
        this.defaultValueKey = defaultValueKey;
    }

    @Override
    public synchronized void putObject(K key, V value) {
        if (defaultValueKey.equals(key)) defaultValue = value;
        super.putObject(key, value);
    }

    @Override
    public synchronized V getObject(K name) {
        V v = super.getObject(name);
        return v == null ? defaultValue : v;
    }
}
