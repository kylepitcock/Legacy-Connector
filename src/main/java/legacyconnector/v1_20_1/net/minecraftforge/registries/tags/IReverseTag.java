package legacyconnector.v1_20_1.net.minecraftforge.registries.tags;

import java.util.stream.Stream;
import net.minecraft.tags.TagKey;

/** The tags a value belongs to (Forge 1.20.1). */
public interface IReverseTag<V> {
    Stream<TagKey<V>> getTagKeys();

    boolean containsTag(TagKey<V> key);
}
