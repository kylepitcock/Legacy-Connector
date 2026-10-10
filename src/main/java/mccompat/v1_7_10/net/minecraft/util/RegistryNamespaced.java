package mccompat.v1_7_10.net.minecraft.util;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Read-mostly facade over the modern registries, keyed by legacy names ({@code "minecraft:stone"},
 * {@code "modid:block_name"}) and legacy numeric ids.
 */
public class RegistryNamespaced implements Iterable<Object> {
    private final Function<String, Object> byName;
    private final Function<Object, String> nameOf;
    private final IntFunction<Object> byId;
    private final ToIntFunction<Object> idOf;
    private final Supplier<Iterator<Object>> all;

    public RegistryNamespaced(Function<String, Object> byName, Function<Object, String> nameOf, IntFunction<Object> byId,
                              ToIntFunction<Object> idOf, Supplier<Iterator<Object>> all) {
        this.byName = byName;
        this.nameOf = nameOf;
        this.byId = byId;
        this.idOf = idOf;
        this.all = all;
    }

    public Object getObject(String name) {
        return byName.apply(ensureNamespaced(name));
    }

    public Object getObject(Object name) {
        return getObject(String.valueOf(name));
    }

    public String getNameForObject(Object obj) {
        return nameOf.apply(obj);
    }

    public boolean containsKey(String name) {
        return getObject(name) != null;
    }

    public boolean containsKey(Object name) {
        return containsKey(String.valueOf(name));
    }

    public int getIDForObject(Object obj) {
        return idOf.applyAsInt(obj);
    }

    public Object getObjectById(int id) {
        return byId.apply(id);
    }

    public boolean containsId(int id) {
        return byId.apply(id) != null;
    }

    public void addObject(int id, String name, Object obj) {
        throw new UnsupportedOperationException("Direct registry writes are not supported; use GameRegistry");
    }

    @Override
    public Iterator<Object> iterator() {
        return all.get();
    }

    protected static String ensureNamespaced(String name) {
        return name.indexOf(':') == -1 ? "minecraft:" + name : name;
    }
}
