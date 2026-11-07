package legacyconnector.v1_20_1.net.minecraftforge.registries.tags;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/** Forge 1.20.1's tag access of a registry ({@code ForgeRegistries.ITEMS.tags()}). */
public interface ITagManager<V> extends Iterable<ITag<V>> {
    ITag<V> getTag(TagKey<V> name);

    Optional<IReverseTag<V>> getReverseTag(V value);

    boolean isKnownTagName(TagKey<V> name);

    Stream<ITag<V>> stream();

    Stream<TagKey<V>> getTagNames();

    TagKey<V> createTagKey(ResourceLocation location);

    TagKey<V> createOptionalTagKey(ResourceLocation location, Set<? extends Supplier<V>> defaults);

    void addOptionalTagDefaults(TagKey<V> name, Set<? extends Supplier<V>> defaults);
}
