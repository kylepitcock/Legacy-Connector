package legacyconnector.v1_20_1;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import net.neoforged.neoforge.client.event.InputEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary, opt-in diagnostic hooks for Palladium Survival flight.
 * Enable with -Dlegacyconnector.debug.palladiumFlight=true.
 */
public final class PalladiumFlightDebugCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/PalladiumFlight");
    private static final AtomicInteger MOVEMENT_SAMPLES = new AtomicInteger();

    private PalladiumFlightDebugCompat() {
    }

    private static boolean enabled() {
        return Boolean.getBoolean("legacyconnector.debug.palladiumFlight");
    }

    public static void logKeyEvent(String handler, Object rawEvent) {
        if (!enabled() || !(rawEvent instanceof InputEvent.Key event)) return;
        int key = event.getKey();
        if (!isFlightKey(key)) return;
        LOGGER.info("[PalladiumFlightTrace] input callback={} key={} code={} action={} scanCode={} modifiers={}",
                handler, keyName(key), key, actionName(event.getAction()), event.getScanCode(), event.getModifiers());
    }

    public static void logFlightType(String stage, Object handler, Object requestedType) {
        if (!enabled()) return;
        Object player = readField(handler, "player");
        LOGGER.info("[PalladiumFlightTrace] flight activation stage={} requestedType={} player={}",
                stage, requestedType, player == null ? "null" : player.getClass().getName());
    }

    public static void logFlightState(String stage, Object handler) {
        if (!enabled() || handler == null) return;
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
