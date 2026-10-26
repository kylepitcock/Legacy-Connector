package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import legacyconnector.v1_7_10.net.minecraft.util.RegistryNamespaced;

/** FML's typed block/item registry, a view over the modern registries like {@link RegistryNamespaced}. */
@SuppressWarnings("unchecked")
public class FMLControlledNamespacedRegistry<I> extends RegistryNamespaced {
    public FMLControlledNamespacedRegistry(Function<String, Object> byName, Function<Object, String> nameOf, IntFunction<Object> byId,
                                           ToIntFunction<Object> idOf, Supplier<Iterator<Object>> all) {
        this(byName, nameOf, byId, idOf, all, null);
    }

    /** {@code direct} takes a mod's direct {@code addObject(id, name, object)} (true when it registered the object). */
    public FMLControlledNamespacedRegistry(Function<String, Object> byName, Function<Object, String> nameOf, IntFunction<Object> byId,
                                           ToIntFunction<Object> idOf, Supplier<Iterator<Object>> all, java.util.function.BiPredicate<String, Object> direct) {
        super(byName, nameOf, byId, idOf, all);
        this.mcc$direct = direct;
    }

    private final java.util.function.BiPredicate<String, Object> mcc$direct;

    @Override
    public void addObject(int id, String name, Object obj) {
        if (mcc$direct != null && mcc$direct.test(name, obj)) return;
        super.addObject(id, name, obj);
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

    /** Writes name -> id with FML's discriminator prefix (char 1 for blocks, char 2 for items), as saved in level.dat. */
    public void serializeInto(java.util.Map<String, Integer> idMapping) {
        for (Iterator<Object> it = iterator(); it.hasNext(); ) {
            Object o = it.next();
            String name = getNameForObject(o);
            if (name == null) continue;
            char discriminator = o instanceof legacyconnector.v1_7_10.net.minecraft.block.Block ? '\u0001' : '\u0002';
            idMapping.put(discriminator + name, getIDForObject(o));
        }
    }

    public RegistryDelegate<I> getDelegate(I thing, Class<I> type) {
        return new RegistryDelegate.Delegate<>(thing, type, this);
    }
}
