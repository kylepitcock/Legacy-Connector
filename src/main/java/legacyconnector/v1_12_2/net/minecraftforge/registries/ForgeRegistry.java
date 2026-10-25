package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The registry implementation mods cast to for numeric ids (ids are the registration order here). */
public abstract class ForgeRegistry<V extends IForgeRegistryEntry<V>> implements IForgeRegistryModifiable<V> {
    public abstract int getID(V value);

    public abstract int getID(ResourceLocation name);

    public abstract V getValue(int id);

    public int getMaxId() {
        return getValues().size();
    }
}
