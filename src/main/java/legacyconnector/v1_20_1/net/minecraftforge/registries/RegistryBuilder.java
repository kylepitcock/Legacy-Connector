package legacyconnector.v1_20_1.net.minecraftforge.registries;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.callback.AddCallback;
import net.neoforged.neoforge.registries.callback.BakeCallback;
import net.neoforged.neoforge.registries.callback.ClearCallback;

/**
 * Forge 1.20.1's {@code RegistryBuilder}. Collects the settings and applies them to NeoForge's builder
 * ({@code ForgeApi1201.newRegistryCreate} / {@code makeRegistry}); the Forge callbacks are adapted to NeoForge's.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RegistryBuilder<T> {
    private ResourceLocation registryName;
    private ResourceLocation defaultKey;
    private int maxId = -1;
    private boolean sync = true;
    private final List<IForgeRegistry.AddCallback<T>> add = new ArrayList<>();
    private final List<IForgeRegistry.ClearCallback<T>> clear = new ArrayList<>();
    private final List<IForgeRegistry.BakeCallback<T>> bake = new ArrayList<>();

    public RegistryBuilder<T> setName(ResourceLocation name) {
        this.registryName = name;
        return this;
    }

    public RegistryBuilder<T> setType(Class<T> type) {
        return this;
    }

    public RegistryBuilder<T> setIDRange(int min, int max) {
        this.maxId = max;
        return this;
    }

    public RegistryBuilder<T> setMaxID(int max) {
        this.maxId = max;
        return this;
    }

    public RegistryBuilder<T> setDefaultKey(ResourceLocation key) {
        this.defaultKey = key;
        return this;
    }

    public RegistryBuilder<T> disableSaving() {
        return this;
    }

    public RegistryBuilder<T> disableSync() {
        this.sync = false;
        return this;
    }

    public RegistryBuilder<T> disableOverrides() {
        return this;
    }

    public RegistryBuilder<T> allowModification() {
        return this;
    }

    public RegistryBuilder<T> hasWrapper() {
        return this;
    }

    public RegistryBuilder<T> hasTags() {
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.AddCallback<T> cb) {
        add.add(cb);
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.ClearCallback<T> cb) {
        clear.add(cb);
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.BakeCallback<T> cb) {
        bake.add(cb);
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.CreateCallback<T> cb) {
        return this;
    }

    public RegistryBuilder<T> add(IForgeRegistry.ValidateCallback<T> cb) {
        return this;
    }

    public ResourceLocation mcc$name() {
        return registryName;
    }

    /** The NeoForge builder with this builder's settings. */
    public net.neoforged.neoforge.registries.RegistryBuilder<T> mcc$toNeo(ResourceKey<? extends net.minecraft.core.Registry<T>> key) {
        var b = new net.neoforged.neoforge.registries.RegistryBuilder<T>(key);
        if (defaultKey != null) b.defaultKey(defaultKey);
        if (maxId >= 0) b.maxId(maxId);
        b.sync(sync);
        for (var cb : add) {
            b.onAdd((AddCallback<T>) (reg, id, k, value) -> cb.onAdd(new ForgeRegistryWrapper(reg), RegistryManager.ACTIVE, id, k, value, null));
        }
        for (var cb : clear) {
            b.onClear((ClearCallback<T>) (reg, full) -> cb.onClear(new ForgeRegistryWrapper(reg), RegistryManager.ACTIVE));
        }
        for (var cb : bake) {
            b.onBake((BakeCallback<T>) reg -> cb.onBake(new ForgeRegistryWrapper(reg), RegistryManager.ACTIVE));
        }
        return b;
    }
}
