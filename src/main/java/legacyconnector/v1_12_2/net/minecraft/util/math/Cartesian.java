package legacyconnector.v1_12_2.net.minecraft.util.math;

import java.util.ArrayList;
import java.util.List;

/** Cartesian product of lists of values. */
public final class Cartesian {
    private Cartesian() {
    }

    public static <T> Iterable<T[]> cartesianProduct(Class<T> clazz, Iterable<? extends Iterable<? extends T>> sets) {
        List<T[]> out = new ArrayList<>();
        List<List<T>> lists = new ArrayList<>();
        for (Iterable<? extends T> it : sets) {
            List<T> l = new ArrayList<>();
            for (T t : it) l.add(t);
            lists.add(l);
        }
        @SuppressWarnings("unchecked")
        T[] proto = (T[]) java.lang.reflect.Array.newInstance(clazz, lists.size());
        fill(lists, 0, proto.clone(), out);
        return out;
    }

    private static <T> void fill(List<List<T>> lists, int index, T[] current, List<T[]> out) {
        if (index == lists.size()) {
            out.add(current.clone());
            return;
        }
        for (T t : lists.get(index)) {
            current[index] = t;
            fill(lists, index + 1, current, out);
        }
    }

    public static <T> Iterable<List<T>> cartesianProduct(Iterable<? extends Iterable<? extends T>> sets) {
        List<List<T>> out = new ArrayList<>();
        List<List<T>> lists = new ArrayList<>();
        for (Iterable<? extends T> it : sets) {
            List<T> l = new ArrayList<>();
            for (T t : it) l.add(t);
            lists.add(l);
        }
        fillLists(lists, 0, new ArrayList<>(), out);
        return out;
    }

    private static <T> void fillLists(List<List<T>> lists, int index, List<T> current, List<List<T>> out) {
        if (index == lists.size()) {
            out.add(new ArrayList<>(current));
            return;
        }
        for (T t : lists.get(index)) {
            current.add(t);
            fillLists(lists, index + 1, current, out);
            current.remove(current.size() - 1);
        }
    }
}
