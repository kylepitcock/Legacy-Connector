package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A registry whose entries can be removed again. */
public interface IForgeRegistryModifiable<V extends IForgeRegistryEntry<V>> extends IForgeRegistry<V> {
    boolean isLocked();

    void unfreeze();

    V remove(ResourceLocation key);

    /** Removes every entry. */
    default void clear() {
        for (ResourceLocation key : new java.util.ArrayList<>(getKeys())) remove(key);
    }
}
