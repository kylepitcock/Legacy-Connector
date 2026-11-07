package legacyconnector.v1_20_1.net.minecraftforge.registries;

import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import legacyconnector.v1_20_1.net.minecraftforge.registries.tags.IReverseTag;
import legacyconnector.v1_20_1.net.minecraftforge.registries.tags.ITag;
import legacyconnector.v1_20_1.net.minecraftforge.registries.tags.ITagManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * An {@link IForgeRegistry} answering from a vanilla registry. Static registries are bound directly; data-driven ones (biomes, enchantments in 1.21.1) are
 * looked up in the running server's registry access on every call.
 */
public final class ForgeRegistryWrapper<V> implements IForgeRegistry<V> {
    private final ResourceKey<? extends Registry<V>> key;
    private final Registry<V> fixed;

    public ForgeRegistryWrapper(Registry<V> registry) {
        this.fixed = registry;
        this.key = registry.key();
    }

    public ForgeRegistryWrapper(ResourceKey<? extends Registry<V>> key) {
        this.fixed = null;
        this.key = key;
    }

    @Override
    public Registry<V> mcc$registry() {
        if (fixed != null) return fixed;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess().registryOrThrow(key);
        throw new IllegalStateException("The registry " + key.location() + " is data driven and no world is loaded yet");
    }

    private Registry<V> registryOrNull() {
        try {
            return mcc$registry();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Override
    public ResourceKey<? extends Registry<V>> getRegistryKey() {
        return key;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return key.location();
    }

    @Override
    public boolean containsKey(ResourceLocation k) {
        Registry<V> r = registryOrNull();
        return r != null && r.containsKey(k);
    }

    @Override
    public boolean containsValue(V value) {
        Registry<V> r = registryOrNull();
        return r != null && r.getKey(value) != null && r.containsKey(r.getKey(value));
    }

    @Override
    public boolean isEmpty() {
        Registry<V> r = registryOrNull();
        return r == null || r.keySet().isEmpty();
    }

    @Override
    public V getValue(ResourceLocation k) {
        Registry<V> r = registryOrNull();
        return r == null ? null : r.get(k);
    }

    @Override
    public ResourceLocation getKey(V value) {
        Registry<V> r = registryOrNull();
        return r == null ? null : r.getKey(value);
    }

    @Override
    public ResourceLocation getDefaultKey() {
        Registry<V> r = registryOrNull();
        return r instanceof net.minecraft.core.DefaultedRegistry<V> d ? d.getDefaultKey() : null;
    }

    @Override
    public Optional<ResourceKey<V>> getResourceKey(V value) {
        Registry<V> r = registryOrNull();
        return r == null ? Optional.empty() : r.getResourceKey(value);
    }

    @Override
    public Set<ResourceLocation> getKeys() {
        Registry<V> r = registryOrNull();
        return r == null ? Set.of() : Collections.unmodifiableSet(r.keySet());
    }

    @Override
    public Collection<V> getValues() {
        Registry<V> r = registryOrNull();
        return r == null ? java.util.List.of() : r.stream().collect(Collectors.toList());
    }

    @Override
    public Set<Map.Entry<ResourceKey<V>, V>> getEntries() {
        Registry<V> r = registryOrNull();
        Map<ResourceKey<V>, V> out = new LinkedHashMap<>();
        if (r != null) for (var e : r.entrySet()) out.put(e.getKey(), e.getValue());
        return Collections.unmodifiableSet(out.entrySet());
    }

    @Override
    public Codec<V> getCodec() {
        return mcc$registry().byNameCodec();
    }

    @Override
    public Optional<Holder.Reference<V>> getHolder(ResourceKey<V> k) {
        Registry<V> r = registryOrNull();
        return r == null ? Optional.empty() : r.getHolder(k);
    }

    @Override
    public Optional<Holder.Reference<V>> getHolder(ResourceLocation location) {
        return getHolder(ResourceKey.create(key, location));
    }

    @Override
    public Optional<Holder.Reference<V>> getHolder(V value) {
        return getResourceKey(value).flatMap(this::getHolder);
    }

    @Override
    public void register(ResourceLocation k, V value) {
        if (mcc$registry() instanceof WritableRegistry<V> w) Registry.register(w, k, value);
        else throw new UnsupportedOperationException("The registry " + key.location() + " cannot be written directly");
    }

    @Override
    public void register(String k, V value) {
        register(ResourceLocation.parse(k.contains(":") ? k : "minecraft:" + k), value);
    }

    @Override
    public Iterator<V> iterator() {
        return getValues().iterator();
    }

    // ---- tags --------------------------------------------------------------------------------------------------------------

    private final ITagManager<V> tags = new Tags();

    @Override
    public ITagManager<V> tags() {
        return tags;
    }

    private final class Tags implements ITagManager<V> {
        @Override
        public ITag<V> getTag(TagKey<V> name) {
            return new TagView(name);
        }

        @Override
        public Optional<IReverseTag<V>> getReverseTag(V value) {
            return getHolder(value).map(h -> new IReverseTag<V>() {
                @Override
                public Stream<TagKey<V>> getTagKeys() {
                    return h.tags();
                }

                @Override
                public boolean containsTag(TagKey<V> k) {
                    return h.is(k);
                }
            });
        }

        @Override
        public boolean isKnownTagName(TagKey<V> name) {
            Registry<V> r = registryOrNull();
            return r != null && r.getTag(name).isPresent();
        }

        @Override
        public Stream<ITag<V>> stream() {
            return getTagNames().map(this::getTag);
        }

        @Override
        public Stream<TagKey<V>> getTagNames() {
            Registry<V> r = registryOrNull();
            return r == null ? Stream.empty() : r.getTagNames();
        }

        @Override
        public TagKey<V> createTagKey(ResourceLocation location) {
            return TagKey.create(key, location);
        }

        @Override
        public TagKey<V> createOptionalTagKey(ResourceLocation location, Set<? extends Supplier<V>> defaults) {
            return createTagKey(location);
        }

        @Override
        public void addOptionalTagDefaults(TagKey<V> name, Set<? extends Supplier<V>> defaults) {
        }

        @Override
        public Iterator<ITag<V>> iterator() {
            return stream().iterator();
        }
    }

    private final class TagView implements ITag<V> {
        private final TagKey<V> tag;

        TagView(TagKey<V> tag) {
            this.tag = tag;
        }

        private Stream<V> values() {
            Registry<V> r = registryOrNull();
            if (r == null) return Stream.empty();
            return r.getTag(tag).map(HolderSet.Named::stream).orElseGet(Stream::empty).map(Holder::value);
        }

        @Override
        public TagKey<V> getKey() {
            return tag;
        }

        @Override
        public Stream<V> stream() {
            return values();
        }

        @Override
        public boolean isEmpty() {
            return values().findAny().isEmpty();
        }

        @Override
        public int size() {
            return (int) values().count();
        }

        @Override
        public boolean contains(V value) {
            return getHolder(value).map(h -> h.is(tag)).orElse(false);
        }

        @Override
        public Optional<V> getRandomElement(RandomSource random) {
            var list = values().toList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(random.nextInt(list.size())));
        }

        @Override
        public Iterator<V> iterator() {
            return values().iterator();
        }
    }
}
