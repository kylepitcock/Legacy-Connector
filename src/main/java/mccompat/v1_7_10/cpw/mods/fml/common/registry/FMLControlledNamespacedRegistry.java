package mccompat.v1_7_10.cpw.mods.fml.common.registry;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import mccompat.v1_7_10.net.minecraft.util.RegistryNamespaced;

/** FML's typed block/item registry, a view over the modern registries like {@link RegistryNamespaced}. */
@SuppressWarnings("unchecked")
public class FMLControlledNamespacedRegistry<I> extends RegistryNamespaced {
    public FMLControlledNamespacedRegistry(Function<String, Object> byName, Function<Object, String> nameOf, IntFunction<Object> byId,
                                           ToIntFunction<Object> idOf, Supplier<Iterator<Object>> all) {
        super(byName, nameOf, byId, idOf, all);
    }

    public I get(int id) {
        return (I) getObjectById(id);
    }

    public I get(String name) {
        return (I) getObject(name);
    }

    public I getRaw(int id) {
        return get(id);
    }

    public I getRaw(String name) {
        return get(name);
    }

    public int getId(Object thing) {
        return getIDForObject(thing);
    }

    public int getId(String name) {
        Object o = getObject(name);
        return o == null ? -1 : getIDForObject(o);
    }

    public boolean contains(String name) {
        return containsKey(name);
    }

    public Iterable<I> typeSafeIterable() {
        return () -> (Iterator<I>) iterator();
    }

    public I getDefaultValue() {
        return null;
    }

    public RegistryDelegate<I> getDelegate(I thing, Class<I> type) {
        return new RegistryDelegate.Delegate<>(thing, type, this);
    }
}
