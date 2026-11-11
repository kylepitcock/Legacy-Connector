package legacyconnector.v1_20_1;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Restores the KubeJS 6 BlockEntityInfo inventory convenience methods on KubeJS 7.
 *
 * <p>KubeJS 6 exposed inventory(width, height) and rightClickOpensInventory().
 * KubeJS 7 stores inventories as named attachments and requires an attachment id
 * plus a side set. The empty direction set is KubeJS 7's own default for an
 * attachment available from any side.</p>
 *
 * <p>All KubeJS types are accessed reflectively so this adapter remains optional.</p>
 */
public final class KubeJSBlockEntityCompat {
    private static final String DIRECTION_WRAPPER =
            "dev.latvian.mods.kubejs.plugin.builtin.wrapper.DirectionWrapper";

    private static final Map<Object, LegacyState> STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final class LegacyState {
        String firstInventoryId;
        boolean rightClickRequestedBeforeInventory;
    }

    private KubeJSBlockEntityCompat() {
    }

    /** Restores the old two-number inventory(width, height) call. */
    public static void inventory(Object info, int width, int height) {
        Objects.requireNonNull(info, "KubeJS block entity info");

        try {
            Object rawAttachments = publicField(info, "attachments").get(info);
            if (!(rawAttachments instanceof Map<?, ?> attachments)) {
                throw new IllegalStateException("KubeJS BlockEntityInfo.attachments is not a Map");
            }

            String id = nextInventoryId(attachments);
            ClassLoader loader = info.getClass().getClassLoader();
            Class<?> directionWrapper = Class.forName(DIRECTION_WRAPPER, true, loader);
            Object directions = directionWrapper.getField("EMPTY_SET").get(null);
            if (!(directions instanceof Set<?>)) {
                throw new IllegalStateException("KubeJS DirectionWrapper.EMPTY_SET is not a Set");
            }

            Method inventory = info.getClass().getMethod(
                    "inventory", String.class, Set.class, int.class, int.class);
            inventory.invoke(info, id, directions, width, height);

            String firstId;
            boolean restorePendingRightClick;
            synchronized (STATES) {
                LegacyState state = STATES.computeIfAbsent(info, ignored -> new LegacyState());
                if (state.firstInventoryId == null) {
                    state.firstInventoryId = id;
                }
                firstId = state.firstInventoryId;
                restorePendingRightClick = state.rightClickRequestedBeforeInventory;
                state.rightClickRequestedBeforeInventory = false;
            }

            // If the legacy no-argument method ran first, it initially used the
            // default id. Replace that callback now that the actual id is known.
            if (restorePendingRightClick) {
                invokeRightClickOpensInventory(info, firstId);
            }
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure("inventory(int, int)", error);
        }
    }

    /** Restores the old no-argument rightClickOpensInventory() call. */
    public static void rightClickOpensInventory(Object info) {
        Objects.requireNonNull(info, "KubeJS block entity info");

        String id;
        synchronized (STATES) {
            LegacyState state = STATES.computeIfAbsent(info, ignored -> new LegacyState());
            id = state.firstInventoryId;
            if (id == null) {
                state.rightClickRequestedBeforeInventory = true;
                id = "inventory";
            }
        }

        invokeRightClickOpensInventory(info, id);
    }

    private static String nextInventoryId(Map<?, ?> attachments) {
        String id = "inventory";
        for (int suffix = 1; attachments.containsKey(id); suffix++) {
            id = "inventory_" + suffix;
        }
        return id;
    }

    private static void invokeRightClickOpensInventory(Object info, String id) {
        try {
            Method method = info.getClass().getMethod("rightClickOpensInventory", String.class);
            method.invoke(info, id);
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure("rightClickOpensInventory()", error);
        }
    }

    private static Field publicField(Object owner, String name) throws NoSuchFieldException {
        return owner.getClass().getField(name);
    }

    private static IllegalStateException compatibilityFailure(String operation, Throwable cause) {
        Throwable root = cause instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : cause;
        return new IllegalStateException(
                "KubeJS 7 block-entity " + operation + " compatibility could not access the inspected public API",
                root);
    }
}
