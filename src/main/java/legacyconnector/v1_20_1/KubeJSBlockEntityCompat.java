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
 * Restores KubeJS 6 BlockEntityInfo conveniences on KubeJS 7.
 *
 * <p>KubeJS 6 exposed inventory(width, height), rightClickOpensInventory(),
 * and callback-based serverTick/clientTick/tick methods. KubeJS 7 stores
 * inventories as named attachments and routes block-entity ticks through the
 * targeted BlockEvents.blockEntityTick registry event.</p>
 *
 * <p>All KubeJS types are accessed reflectively so the adapter remains optional.</p>
 */
public final class KubeJSBlockEntityCompat {
    private static final String DIRECTION_WRAPPER =
            "dev.latvian.mods.kubejs.plugin.builtin.wrapper.DirectionWrapper";
    private static final String BLOCK_EVENTS =
            "dev.latvian.mods.kubejs.plugin.builtin.event.BlockEvents";
    private static final String RHINO_CONTEXT = "dev.latvian.mods.rhino.Context";
    private static final String RHINO_SCRIPTABLE = "dev.latvian.mods.rhino.Scriptable";
    private static final String INVENTORY_KJS = "dev.latvian.mods.kubejs.core.InventoryKJS";

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

    /** Restores KubeJS 6 serverTick(callback). */
    public static void serverTick(Object info, Object callback) {
        registerTick(info, callback, true, false, 1, 0);
    }

    /** Restores KubeJS 6 serverTick(frequency, offset, callback). */
    public static void serverTick(Object info, int frequency, int offset, Object callback) {
        registerTick(info, callback, true, false, frequency, offset);
    }

    /** Restores KubeJS 6 clientTick(callback). */
    public static void clientTick(Object info, Object callback) {
        registerTick(info, callback, false, true, 1, 0);
    }

    /** Restores KubeJS 6 clientTick(frequency, offset, callback). */
    public static void clientTick(Object info, int frequency, int offset, Object callback) {
        registerTick(info, callback, false, true, frequency, offset);
    }

    /** Restores KubeJS 6 tick(callback), which ran on both logical sides. */
    public static void tick(Object info, Object callback) {
        registerTick(info, callback, true, true, 1, 0);
    }

    /** Restores KubeJS 6 tick(frequency, offset, callback). */
    public static void tick(Object info, int frequency, int offset, Object callback) {
        registerTick(info, callback, true, true, frequency, offset);
    }

    private static void registerTick(
            Object info,
            Object callback,
            boolean server,
            boolean client,
            int frequency,
            int offset) {
        Objects.requireNonNull(info, "KubeJS block entity info");
        Objects.requireNonNull(callback, "KubeJS block entity tick callback");

        try {
            registerTargetedBlockEntityTick(info, callback);
            if (server) {
                invokeNoArgs(info, "serverTicking");
            }
            if (client) {
                invokeNoArgs(info, "clientTicking");
            }

            // KubeJS 6 ran every tick whenever frequency <= 1, irrespective of
            // offset. KubeJS 7 uses a modulo comparison, so normalize that case.
            int normalizedFrequency = Math.max(1, frequency);
            int normalizedOffset = normalizedFrequency <= 1 ? 0 : Math.max(0, offset);
            invokeInt(info, "tickFrequency", normalizedFrequency);
            invokeInt(info, "tickOffset", normalizedOffset);
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure("serverTick/clientTick/tick", error);
        }
    }

    /**
     * Register the legacy callback through KubeJS 7's own targeted event function.
     * Calling EventHandler.call() is intentional: KubeJS converts the Rhino function
     * to its IEventHandler type and records the current startup script type/source.
     */
    private static void registerTargetedBlockEntityTick(Object info, Object callback)
            throws ReflectiveOperationException {
        ClassLoader loader = info.getClass().getClassLoader();
        Object blockBuilder = publicField(info, "blockBuilder").get(info);
        Object blockId = publicField(blockBuilder, "id").get(blockBuilder);
        if (blockId == null) {
            throw new IllegalStateException("KubeJS BlockEntityInfo.blockBuilder.id is null");
        }

        Class<?> blockEvents = Class.forName(BLOCK_EVENTS, true, loader);
        Object eventHandler = blockEvents.getField("BLOCK_ENTITY_TICK").get(null);
        Class<?> contextClass = Class.forName(RHINO_CONTEXT, true, loader);
        Class<?> scriptableClass = Class.forName(RHINO_SCRIPTABLE, true, loader);
        Object context = contextClass.getMethod("getCurrentContext").invoke(null);
        if (context == null) {
            throw new IllegalStateException("No active Rhino Context while registering a legacy block-entity tick callback");
        }

        Method call = eventHandler.getClass().getMethod(
                "call", contextClass, scriptableClass, scriptableClass, Object[].class);
        call.invoke(eventHandler, context, null, null, new Object[] {blockId, callback});
    }

    /** Legacy BlockEntityJS state forwarded by the KubeJS 7 tick-event mixin. */
    public static Object eventData(Object event) {
        return fieldValue(blockEntityFromTickEvent(event), "data", "BlockEntityTickKubeEvent.data");
    }

    public static int eventCoordinate(Object event, String coordinate) {
        Object entity = blockEntityFromTickEvent(event);
        Object value = fieldValue(entity, coordinate, "BlockEntityTickKubeEvent." + coordinate);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("KubeJS block entity coordinate '" + coordinate + "' is not numeric");
        }
        return number.intValue();
    }

    public static Object eventInventory(Object event) {
        Object entity = blockEntityFromTickEvent(event);
        Object raw = fieldValue(entity, "attachments", "KubeBlockEntity.attachments");
        if (!(raw instanceof Map<?, ?> attachments)) {
            throw new IllegalStateException("KubeJS KubeBlockEntity.attachments is not a Map");
        }
        for (Object value : attachments.values()) {
            if (implementsInterface(value.getClass(), INVENTORY_KJS)) {
                return value;
            }
        }
        return null;
    }

    public static Object eventPlacer(Object event) {
        return invoke(blockEntityFromTickEvent(event), "getPlacer", new Class<?>[0], new Object[0]);
    }

    public static Object eventPlacerId(Object event) {
        Object entity = blockEntityFromTickEvent(event);
        return fieldValue(entity, "placerId", "KubeBlockEntity.placerId");
    }

    public static void eventSave(Object event) {
        invokeNoArgs(blockEntityFromTickEvent(event), "save");
    }

    public static void eventSync(Object event) {
        invokeNoArgs(blockEntityFromTickEvent(event), "sync");
    }

    public static void eventSendBlockEvent(Object event, int eventId, int data) {
        invoke(blockEntityFromTickEvent(event), "sendEvent", new Class<?>[] {int.class, int.class},
                new Object[] {eventId, data});
    }

    public static Object eventBlockEntity(Object event) {
        return blockEntityFromTickEvent(event);
    }

    private static Object blockEntityFromTickEvent(Object event) {
        Objects.requireNonNull(event, "KubeJS block entity tick event");
        try {
            Field field = findField(event.getClass(), "entity");
            if (!field.canAccess(event)) {
                field.setAccessible(true);
            }
            Object entity = field.get(event);
            if (entity == null) {
                throw new IllegalStateException("KubeJS BlockEntityTickKubeEvent.entity is null");
            }
            return entity;
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure("BlockEntityTickKubeEvent legacy view", error);
        }
    }

    private static boolean implementsInterface(Class<?> type, String interfaceName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Class<?> contract : current.getInterfaces()) {
                if (contract.getName().equals(interfaceName) || implementsInterface(contract, interfaceName)) {
                    return true;
                }
            }
        }
        return false;
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

    private static void invokeNoArgs(Object owner, String name) {
        invoke(owner, name, new Class<?>[0], new Object[0]);
    }

    private static void invokeInt(Object owner, String name, int value) {
        invoke(owner, name, new Class<?>[] {int.class}, new Object[] {value});
    }

    private static Object invoke(Object owner, String name, Class<?>[] parameterTypes, Object[] arguments) {
        try {
            Method method = owner.getClass().getMethod(name, parameterTypes);
            return method.invoke(owner, arguments);
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure(name, error);
        }
    }

    private static Object fieldValue(Object owner, String name, String operation) {
        try {
            return publicField(owner, name).get(owner);
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure(operation, error);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Continue through the superclass chain.
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
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
