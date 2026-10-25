package legacyconnector.v1_12_2.net.minecraftforge.registries;

import java.util.Collection;
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

    Collection<V> getValues();

    Set<Map.Entry<ResourceLocation, V>> getEntries();

    /** Callbacks of a registry builder: accepted by it, never called (a mod implements their methods itself). */
    interface AddCallback<V extends IForgeRegistryEntry<V>> {
    }

    interface ClearCallback<V extends IForgeRegistryEntry<V>> {
    }

    interface CreateCallback<V extends IForgeRegistryEntry<V>> {
    }

    interface ValidateCallback<V extends IForgeRegistryEntry<V>> {
    }

    interface DummyFactory<V extends IForgeRegistryEntry<V>> {
    }

    interface MissingFactory<V extends IForgeRegistryEntry<V>> {
    }
}
