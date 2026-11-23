package legacyconnector.v1_20_1.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.kylepitcock.legacyconnector.compat.CompatFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary, opt-in diagnostic hooks for Palladium Survival flight.
 * Enable with -Dlegacyconnector.debug.palladiumFlight=true.
 *
 * This shared helper deliberately does not reference client-only event classes:
 * FlightHandler is loaded on both logical sides.
 */
public final class PalladiumFlightDebugCompat {
    private static final CompatFlags DEBUG = CompatFlags.PALLADIUM_FLIGHT_DEBUG;
    private static final String DEBUG_PROPERTY = DEBUG.property(); // kept: the shim member snapshot lists it
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/PalladiumFlight");
    private static final AtomicInteger MOVEMENT_SAMPLES = new AtomicInteger();

    private PalladiumFlightDebugCompat() {
    }

    /** Opt-in trace of the network messages of a channel (the SimpleChannel shim calls this for every registration and dispatch). */
    public static void traceNetwork(String stage, net.minecraft.resources.ResourceLocation channel, Object message, String direction) {
        if (!DEBUG.on() || message == null) return;
        String messageName = message instanceof Class<?> type ? type.getName() : message.getClass().getName();
        String lower = messageName.toLowerCase(java.util.Locale.ROOT);
        boolean palladiumCoreChannel = "palladiumcore".equals(channel.getNamespace());
        boolean flightMessage = lower.contains("flight") || lower.contains("flying")
                || lower.contains("movement") || lower.contains("key");
        if (palladiumCoreChannel || flightMessage) {
            LOGGER.info("[PalladiumFlightTrace] network stage={} channel={} direction={} message={}",
                    stage, channel, direction, messageName);
        }
    }

    public static boolean isEnabled() {
        return DEBUG.on();
    }

    /** Always logs once during client bootstrap so a missing JVM argument is visible. */
    public static void logClientBootstrap(boolean globalKeyObserverRegistered) {
        LOGGER.info("[PalladiumFlightTrace] client bootstrap diagnosticsEnabled={} globalKeyObserverRegistered={} property={} rawValue={}",
                isEnabled(), globalKeyObserverRegistered, DEBUG.property(), DEBUG.get("<unset>"));
    }

    /** Called from PalladiumCore's transformed class initializer, proving its callback instrumentation loaded. */
    public static void logInputTraceInstalled(String handlerClass) {
        if (!isEnabled()) return;
        LOGGER.info("[PalladiumFlightTrace] subscriber instrumentation loaded handler={}", handlerClass);
    }

    public static void logKeyEvent(String handler, Object rawEvent) {
        if (!isEnabled() || rawEvent == null) return;

        // Avoid linking this common helper to NeoForge's client-only InputEvent.Key class.
        Object keyValue = invokeNoArgs(rawEvent, "getKey");
        if (!(keyValue instanceof Number keyNumber)) return;
        int key = keyNumber.intValue();
        if (!isFlightKey(key)) return;

        int action = number(invokeNoArgs(rawEvent, "getAction"), -1);
        Object scanCode = invokeNoArgs(rawEvent, "getScanCode");
        Object modifiers = invokeNoArgs(rawEvent, "getModifiers");
        LOGGER.info("[PalladiumFlightTrace] input callback={} key={} code={} action={} scanCode={} modifiers={}",
                handler, keyName(key), key, actionName(action), scanCode, modifiers);
    }

    public static void logFlightType(String stage, Object handler, Object requestedType) {
        if (!isEnabled()) return;
        Object player = readField(handler, "player");
        LOGGER.info("[PalladiumFlightTrace] flight activation stage={} requestedType={} player={}",
                stage, requestedType, player == null ? "null" : player.getClass().getName());
    }

    public static void logFlightState(String stage, Object handler) {
        if (!isEnabled() || handler == null) return;
        if (MOVEMENT_SAMPLES.incrementAndGet() % 10 != 0) return;

        Object player = readField(handler, "player");
        Object flightType = readField(handler, "flightType");
        if (player == null) return;
        Object active = invokeNoArgs(flightType, "isNotNull");
        if (Boolean.FALSE.equals(active)) return;

        LOGGER.info("[PalladiumFlightTrace] flight handler stage={} type={} onGround={} fallFlying={} velocity={}",
                stage, flightType, invokeNoArgs(player, "onGround"),
                invokeNoArgs(player, "isFallFlying"), invokeNoArgs(player, "getDeltaMovement"));
    }

    private static boolean isFlightKey(int key) {
        return key == 32 || key == 65 || key == 68 || key == 83 || key == 87 || key == 340 || key == 344;
    }

    private static String keyName(int key) {
        return switch (key) {
            case 32 -> "SPACE";
            case 65 -> "A";
            case 68 -> "D";
            case 83 -> "S";
            case 87 -> "W";
            case 340 -> "LEFT_SHIFT";
            case 344 -> "RIGHT_SHIFT";
            default -> "GLFW_" + key;
        };
    }

    private static String actionName(int action) {
        return switch (action) {
            case 0 -> "RELEASE";
            case 1 -> "PRESS";
            case 2 -> "REPEAT";
            default -> "UNKNOWN_" + action;
        };
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static Object readField(Object target, String name) {
        if (target == null) return null;
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // Continue through the superclass chain.
            } catch (ReflectiveOperationException | RuntimeException error) {
                return "unavailable:" + error.getClass().getSimpleName();
            }
        }
        return null;
    }

    private static Object invokeNoArgs(Object target, String name) {
        if (target == null || target instanceof String) return null;
        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }
}
