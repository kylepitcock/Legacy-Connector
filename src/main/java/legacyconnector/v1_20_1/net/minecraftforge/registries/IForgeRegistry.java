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

    ITagManager<V> tags();

    void register(ResourceLocation key, V value);

    void register(String key, V value);

    /** The vanilla registry behind this handle (not part of Forge's API). */
    Registry<V> mcc$registry();
}
