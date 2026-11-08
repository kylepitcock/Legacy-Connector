package legacyconnector.v1_20_1.net.minecraftforge.registries;

import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import legacyconnector.v1_20_1.net.minecraftforge.registries.tags.ITagManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Forge 1.20.1's registry handle ({@code ForgeRegistries.BLOCKS}...), which NeoForge 1.21.1 dropped in favour of Minecraft's own {@link Registry}.
 * The facade ({@link ForgeRegistryWrapper}) answers every call from the vanilla registry behind it.
 */
public interface IForgeRegistry<V> extends Iterable<V> {
    ResourceKey<? extends Registry<V>> getRegistryKey();

    ResourceLocation getRegistryName();

    boolean containsKey(ResourceLocation key);

    boolean containsValue(V value);

    boolean isEmpty();

    V getValue(ResourceLocation key);

    ResourceLocation getKey(V value);

    ResourceLocation getDefaultKey();

    Optional<ResourceKey<V>> getResourceKey(V value);

    Set<ResourceLocation> getKeys();

    Collection<V> getValues();

    Set<Map.Entry<ResourceKey<V>, V>> getEntries();

    Codec<V> getCodec();

    Optional<Holder.Reference<V>> getHolder(ResourceKey<V> key);

    Optional<Holder.Reference<V>> getHolder(ResourceLocation location);

    Optional<Holder.Reference<V>> getHolder(V value);

    default Holder.Reference<V> getDelegateOrThrow(V value) {
        return getHolder(value).orElseThrow(() -> new IllegalArgumentException("No delegate exists for value " + value));
    }

    default Holder.Reference<V> getDelegateOrThrow(ResourceLocation key) {
        return getHolder(key).orElseThrow(() -> new IllegalArgumentException("No delegate exists for key " + key));
    }

    default Holder.Reference<V> getDelegateOrThrow(ResourceKey<V> key) {
        return getHolder(key).orElseThrow(() -> new IllegalArgumentException("No delegate exists for key " + key));
    }

    default Optional<Holder.Reference<V>> getDelegate(V value) {
        return getHolder(value);
    }

    default Optional<Holder.Reference<V>> getDelegate(ResourceLocation key) {
        return getHolder(key);
    }

    ITagManager<V> tags();

    void register(ResourceLocation key, V value);

    void register(String key, V value);

    @FunctionalInterface
    interface AddCallback<V> {
        void onAdd(IForgeRegistryInternal<V> owner, RegistryManager stage, int id, ResourceKey<V> key, V obj, V oldObj);
    }

    @FunctionalInterface
    interface ClearCallback<V> {
        void onClear(IForgeRegistryInternal<V> owner, RegistryManager stage);
    }

    @FunctionalInterface
    interface BakeCallback<V> {
        void onBake(IForgeRegistryInternal<V> owner, RegistryManager stage);
    }

    @FunctionalInterface
    interface CreateCallback<V> {
        void onCreate(IForgeRegistryInternal<V> owner, RegistryManager stage);
    }

    @FunctionalInterface
    interface ValidateCallback<V> {
        void onValidate(IForgeRegistryInternal<V> owner, RegistryManager stage, int id, ResourceLocation key, V obj);
    }

    /** The vanilla registry behind this handle (not part of Forge's API). */
    Registry<V> mcc$registry();
}
