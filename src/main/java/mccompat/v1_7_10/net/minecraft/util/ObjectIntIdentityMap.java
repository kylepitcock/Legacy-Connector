package mccompat.v1_7_10.net.minecraft.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Object <-> numeric id map of a legacy registry (its methods kept their SRG names in 1.7.10). Backed by the
 * registry's ids; ids mods assign themselves are recorded on top (modern registries cannot be renumbered).
 */
public class ObjectIntIdentityMap implements Iterable<Object> {
    private final IntFunction<Object> byId;
    private final ToIntFunction<Object> idOf;
    private final Map<Object, Integer> overrides = new IdentityHashMap<>();
    private final List<Object> byOverride = new ArrayList<>();

    public ObjectIntIdentityMap() {
        this(id -> null, o -> -1);
    }

    public ObjectIntIdentityMap(IntFunction<Object> byId, ToIntFunction<Object> idOf) {
        this.byId = byId;
        this.idOf = idOf;
    }

    /** put(object, id) */
    public void func_148746_a(Object object, int id) {
        overrides.put(object, id);
        while (byOverride.size() <= id) byOverride.add(null);
        byOverride.set(id, object);
    }

    /** getId(object) */
    public int func_148747_b(Object object) {
        Integer id = overrides.get(object);
        return id != null ? id : idOf.applyAsInt(object);
    }

    /** getById(id) */
    public Object func_148745_a(int id) {
        if (id >= 0 && id < byOverride.size() && byOverride.get(id) != null) return byOverride.get(id);
        return byId.apply(id);
    }

    /** contains(id) */
    public boolean func_148744_b(int id) {
        return func_148745_a(id) != null;
    }

    @Override
    public Iterator<Object> iterator() {
        List<Object> all = new ArrayList<>();
        for (Object o : byOverride) if (o != null) all.add(o);
        return all.iterator();
    }
}
