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
    private static final String RHINO_TYPE_INFO = "dev.latvian.mods.rhino.type.TypeInfo";
    private static final String KUBEJS_EVENT_HANDLER = "dev.latvian.mods.kubejs.event.IEventHandler";
    private static final String KUBEJS_SCRIPT_TYPE = "dev.latvian.mods.kubejs.script.ScriptType";
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
    public static void serverTick(Object context, Object info, Object callback) {
        registerTick(context, info, callback, true, false, 1, 0);
    }

    /** Restores KubeJS 6 serverTick(frequency, offset, callback). */
    public static void serverTick(Object context, Object info, int frequency, int offset, Object callback) {
        registerTick(context, info, callback, true, false, frequency, offset);
    }

    /** Restores KubeJS 6 clientTick(callback). */
    public static void clientTick(Object context, Object info, Object callback) {
        registerTick(context, info, callback, false, true, 1, 0);
    }

    /** Restores KubeJS 6 clientTick(frequency, offset, callback). */
    public static void clientTick(Object context, Object info, int frequency, int offset, Object callback) {
        registerTick(context, info, callback, false, true, frequency, offset);
    }

    /** Restores KubeJS 6 tick(callback), which ran on both logical sides. */
    public static void tick(Object context, Object info, Object callback) {
        registerTick(context, info, callback, true, true, 1, 0);
    }

    /** Restores KubeJS 6 tick(frequency, offset, callback). */
    public static void tick(Object context, Object info, int frequency, int offset, Object callback) {
        registerTick(context, info, callback, true, true, frequency, offset);
    }

    private static void registerTick(
            Object context,
            Object info,
            Object callback,
            boolean server,
            boolean client,
            int frequency,
            int offset) {
        Objects.requireNonNull(info, "KubeJS block entity info");
        Objects.requireNonNull(callback, "KubeJS block entity tick callback");

        try {
            registerTargetedBlockEntityTick(context, info, callback, server, client);
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
     * Register the legacy callback through KubeJS 7's Java listener API.
     *
     * <p>BlockEntityInfo.serverTick/clientTick is called from StartupEvents.registry,
     * which runs after KubeJS has finished loading scripts. EventHandler.call()
     * therefore rejects registration because canListenEvents is already false.
     * The targeted event is also COMMON (server/client), not STARTUP. Convert the
     * Rhino function exactly as EventHandler.call() does, then use listenJava()
     * for the actual logical side(s). This is the supported Java-side registration
     * path and avoids reopening KubeJS's script-loading window.</p>
     */
    private static void registerTargetedBlockEntityTick(
            Object context, Object info, Object callback, boolean server, boolean client)
            throws ReflectiveOperationException {
        Objects.requireNonNull(context, "Rhino script context");
        ClassLoader loader = info.getClass().getClassLoader();
        Object blockBuilder = publicField(info, "blockBuilder").get(info);
        Object blockId = publicField(blockBuilder, "id").get(blockBuilder);
        if (blockId == null) {
            throw new IllegalStateException("KubeJS BlockEntityInfo.blockBuilder.id is null");
        }

        Class<?> blockEvents = Class.forName(BLOCK_EVENTS, true, loader);
        Object eventHandler = blockEvents.getField("BLOCK_ENTITY_TICK").get(null);

        // Match EventHandler.call(): cx.jsToJava(callback, TypeInfo.of(IEventHandler.class)).
        Class<?> handlerInterface = Class.forName(KUBEJS_EVENT_HANDLER, true, loader);
        Class<?> typeInfoClass = Class.forName(RHINO_TYPE_INFO, true, loader);
        Object handlerTypeInfo = typeInfoClass.getMethod("of", Class.class).invoke(null, handlerInterface);
        Method jsToJava = context.getClass().getMethod("jsToJava", Object.class, typeInfoClass);
        Object typedCallback = jsToJava.invoke(context, callback, handlerTypeInfo);
        if (!handlerInterface.isInstance(typedCallback)) {
            throw new IllegalStateException("Rhino did not convert the legacy block-entity callback to KubeJS IEventHandler");
        }

        // BlockEvents.blockEntityTick is COMMON, so listeners must be registered
        // independently for SERVER and CLIENT; STARTUP is explicitly not accepted.
        Class<?> scriptTypeClass = Class.forName(KUBEJS_SCRIPT_TYPE, true, loader);
        Object serverType = scriptTypeClass.getField("SERVER").get(null);
        Object clientType = scriptTypeClass.getField("CLIENT").get(null);
        Method listenJava = eventHandler.getClass().getMethod(
                "listenJava", scriptTypeClass, Object.class, handlerInterface);
        if (server) {
            listenJava.invoke(eventHandler, serverType, blockId, typedCallback);
        }
        if (client) {
            listenJava.invoke(eventHandler, clientType, blockId, typedCallback);
        }
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
