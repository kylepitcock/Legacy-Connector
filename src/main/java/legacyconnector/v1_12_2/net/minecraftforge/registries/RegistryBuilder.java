package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;

/** Mods create their own registries with this builder (in the NewRegistry event). Callbacks are accepted and not called. */
public class RegistryBuilder<T extends IForgeRegistryEntry<T>> {
    private ResourceLocation registryName;
    private Class<T> registryType;

    public RegistryBuilder<T> setName(ResourceLocation name) {
        this.registryName = name;
        return this;
    }

    public RegistryBuilder<T> setType(Class<T> type) {
        this.registryType = type;
        return this;
    }

    public RegistryBuilder<T> setIDRange(int min, int max) {
        return this;
    }

    public RegistryBuilder<T> setMaxID(int max) {
        return this;
    }

    public RegistryBuilder<T> setDefaultKey(ResourceLocation key) {
        return this;
    }

    public RegistryBuilder<T> allowModification() {
        return this;
    }

    public RegistryBuilder<T> disableSaving() {
        return this;
    }

    public RegistryBuilder<T> disableSync() {
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.AddCallback<T> callback) {
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.ClearCallback<T> callback) {
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.CreateCallback<T> callback) {
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.ValidateCallback<T> callback) {
        return this;
    }

    public RegistryBuilder<T> set(IForgeRegistry.DummyFactory<T> factory) {
        return this;
    }

    public RegistryBuilder<T> set(IForgeRegistry.MissingFactory<T> factory) {
        return this;
    }

    /** Registers whichever callback interfaces {@code inst} implements (accepted like the typed {@code add} methods). */
    public RegistryBuilder<T> addCallback(Object inst) {
        return this;
    }

    public IForgeRegistry<T> create() {
        return Registries1122.custom(registryName, registryType);
    }
}
