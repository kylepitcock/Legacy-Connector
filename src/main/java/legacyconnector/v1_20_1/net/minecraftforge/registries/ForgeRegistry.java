package legacyconnector.v1_20_1.net.minecraftforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1's concrete registry class (mods cast {@code ForgeRegistries.X} to it for the numeric ids). The facade is {@link ForgeRegistryWrapper}. */
public abstract class ForgeRegistry<V> implements IForgeRegistryInternal<V> {
    public int getID(V value) {
        return mcc$registry().getId(value);
    }

    public int getID(ResourceLocation key) {
        return mcc$registry().getId(mcc$registry().get(key));
    }

    public V getValue(int id) {
        return mcc$registry().byId(id);
    }

    public ResourceLocation getKey(int id) {
        V v = mcc$registry().byId(id);
        return v == null ? null : mcc$registry().getKey(v);
    }

    public boolean isLocked() {
        return true;
    }

    public void unfreeze() {
    }

    public void freeze() {
    }

    @Override
    public abstract Registry<V> mcc$registry();
}
