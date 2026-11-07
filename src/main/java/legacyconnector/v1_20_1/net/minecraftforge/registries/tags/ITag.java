package legacyconnector.v1_20_1.net.minecraftforge.registries.tags;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

/** The values of one tag (Forge 1.20.1). */
public interface ITag<V> extends Iterable<V> {
    TagKey<V> getKey();

    Stream<V> stream();

    boolean isEmpty();

    int size();

    boolean contains(V value);

    Optional<V> getRandomElement(RandomSource random);
}
