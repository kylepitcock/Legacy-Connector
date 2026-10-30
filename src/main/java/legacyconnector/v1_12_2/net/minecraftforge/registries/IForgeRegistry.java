package legacyconnector.v1_12_2.net.minecraftforge.registries;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

public interface IForgeRegistry<V extends IForgeRegistryEntry<V>> extends Iterable<V> {
    Class<V> getRegistrySuperType();

    void register(V value);

    @SuppressWarnings("unchecked")
    void registerAll(V... values);

    boolean containsKey(ResourceLocation key);

    boolean containsValue(V value);

    V getValue(ResourceLocation key);

    ResourceLocation getKey(V value);

    Set<ResourceLocation> getKeys();

    /** A copy of the values in registration order (deprecated in 1.12.2, still the most used). */
    List<V> getValues();

    default Collection<V> getValuesCollection() {
        return getValues();
    }

    Set<Map.Entry<ResourceLocation, V>> getEntries();

    /** A map another system keeps alongside the registry (1.12's "slave maps", e.g. block state ids), set by the registry's callbacks. */
    @SuppressWarnings("unchecked")
    default <T> T getSlaveMap(ResourceLocation slaveMapName, Class<T> type) {
        synchronized (IForgeRegistryInternal.SLAVE_MAPS) {
            var maps = IForgeRegistryInternal.SLAVE_MAPS.get(this);
            Object map = maps == null ? null : maps.get(slaveMapName);
            return type != null && type.isInstance(map) ? (T) map : null;
        }
    }

    /** Called for each entry registered (with the entry it replaced, or null). */
    interface AddCallback<V extends IForgeRegistryEntry<V>> {
        void onAdd(IForgeRegistryInternal<V> owner, RegistryManager stage, int id, V obj, V oldObj);
    }

    /** Called when the registry is emptied (it never is here). */
    interface ClearCallback<V extends IForgeRegistryEntry<V>> {
        void onClear(IForgeRegistryInternal<V> owner, RegistryManager stage);
    }

    /** Called once when the registry is created (mods set up their slave maps here). */
    interface CreateCallback<V extends IForgeRegistryEntry<V>> {
        void onCreate(IForgeRegistryInternal<V> owner, RegistryManager stage);
    }

    /** Called for each entry when the registry is checked after loading. */
    interface ValidateCallback<V extends IForgeRegistryEntry<V>> {
        void onValidate(IForgeRegistryInternal<V> owner, RegistryManager stage, int id, ResourceLocation key, V obj);
    }

    /** Makes the placeholder of a missing entry (for saves; not used here). */
    interface DummyFactory<V extends IForgeRegistryEntry<V>> {
        V createDummy(ResourceLocation key);
    }

    interface MissingFactory<V extends IForgeRegistryEntry<V>> {
        V createMissing(ResourceLocation key, boolean isNetwork);
    }
}
