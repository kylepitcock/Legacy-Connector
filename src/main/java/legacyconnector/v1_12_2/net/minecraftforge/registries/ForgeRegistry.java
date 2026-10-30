package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The registry implementation mods cast to for numeric ids (ids are the registration order here). */
public abstract class ForgeRegistry<V extends IForgeRegistryEntry<V>> implements IForgeRegistryInternal<V>, IForgeRegistryModifiable<V> {
    /** The builder's add callbacks (called after each registration). */
    private final java.util.List<IForgeRegistry.AddCallback<V>> mcc$addCallbacks = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void mcc$addCallback(IForgeRegistry.AddCallback<V> callback) {
        if (callback != null) mcc$addCallbacks.add(callback);
    }

    /** Tells the add callbacks about a registered entry. */
    protected void mcc$added(V value) {
        if (mcc$addCallbacks.isEmpty()) return;
        for (IForgeRegistry.AddCallback<V> c : mcc$addCallbacks) c.onAdd(this, RegistryManager.ACTIVE, getID(value), value, null);
    }

    public abstract int getID(V value);

    public abstract int getID(ResourceLocation name);

    public abstract V getValue(int id);

    public int getMaxId() {
        return getValues().size();
    }
}
