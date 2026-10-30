package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The registry as its callbacks see it: they may keep maps alongside it ({@link #setSlaveMap}). */
public interface IForgeRegistryInternal<V extends IForgeRegistryEntry<V>> extends IForgeRegistry<V> {
    /** The slave maps of each registry (registries are compared by identity). */
    java.util.Map<Object, java.util.Map<ResourceLocation, Object>> SLAVE_MAPS = new java.util.IdentityHashMap<>();

    default void setSlaveMap(ResourceLocation name, Object obj) {
        synchronized (SLAVE_MAPS) {
            SLAVE_MAPS.computeIfAbsent(this, k -> new java.util.HashMap<>()).put(name, obj);
        }
    }
}
