package org.kylepitcock.legacyconnector.api.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure ordering logic of addon discovery (no Minecraft types, unit tested). */
public final class AddonOrder {
    /** A discovered addon entry class and where it was declared. */
    public record Entry(String className, String declaredBy, int priority) {}

    private AddonOrder() {}

    /** Parses the value of the {@code legacyconnector-addon} mod property: a class name, or a list of them; blanks are dropped. */
    public static List<String> classNames(Object property) {
        List<String> out = new ArrayList<>();
        if (property instanceof Iterable<?> list) {
            for (Object o : list) addName(out, o);
        } else {
            addName(out, property);
        }
        return out;
    }

    private static void addName(List<String> out, Object o) {
        if (o == null) return;
        String s = o.toString().trim();
        if (!s.isEmpty() && !out.contains(s)) out.add(s);
    }

    /** Lower priority first, then by class name; an entry class declared twice (property and service file) counts once. */
    public static List<Entry> ordered(List<Entry> entries) {
        Map<String, Entry> unique = new LinkedHashMap<>();
        for (Entry e : entries) unique.putIfAbsent(e.className(), e);
        List<Entry> out = new ArrayList<>(unique.values());
        out.sort(Comparator.comparingInt(Entry::priority).thenComparing(Entry::className));
        return out;
    }
}
