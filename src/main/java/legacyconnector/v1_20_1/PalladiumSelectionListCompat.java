package legacyconnector.v1_20_1;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stores the three rendering preferences formerly held privately by
 * Minecraft 1.20.1 AbstractSelectionList. Minecraft 1.21.1 replaced those
 * flags/setters with overridable rendering hooks; the bytecode adapter on
 * Palladium's affected list classes consults this state from those hooks.
 *
 * A missing setting means the legacy default (enabled). Weak keys avoid
 * retaining closed screens and their list widgets.
 */
public final class PalladiumSelectionListCompat {
    private record Flags(Boolean background, Boolean topAndBottom, Boolean selection) {}

    private static final Map<Object, Flags> FLAGS = new WeakHashMap<>();

    private PalladiumSelectionListCompat() {}

    private static synchronized Flags flags(Object list) {
        return FLAGS.getOrDefault(list, new Flags(null, null, null));
    }

    private static synchronized void update(Object list, Flags flags) {
        if (list != null) FLAGS.put(list, flags);
    }

    public static void setRenderBackground(Object list, boolean enabled) {
        Flags old = flags(list);
        update(list, new Flags(enabled, old.topAndBottom(), old.selection()));
    }

    public static void setRenderTopAndBottom(Object list, boolean enabled) {
        Flags old = flags(list);
        update(list, new Flags(old.background(), enabled, old.selection()));
    }

    public static void setRenderSelection(Object list, boolean enabled) {
        Flags old = flags(list);
        update(list, new Flags(old.background(), old.topAndBottom(), enabled));
    }

    public static boolean renderBackground(Object list) {
        Boolean enabled = flags(list).background();
        return enabled == null || enabled;
    }

    public static boolean renderTopAndBottom(Object list) {
        Boolean enabled = flags(list).topAndBottom();
        return enabled == null || enabled;
    }

    public static boolean renderSelection(Object list) {
        Boolean enabled = flags(list).selection();
        return enabled == null || enabled;
    }
}
