package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in, rate-limited runtime observations for AlienEvo beam anchors and XLR8
 * animation. These methods only log; they never alter the observed game state.
 */
public final class AlienEvoRuntimeDiagnostics1201Runtime {
    private static final String DEBUG_PROPERTY = "legacyconnector.debug.alienevoRuntime";
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/AlienEvoRuntimeTrace");
    private static final ConcurrentHashMap<String, Long> LAST_LOG_NANOS = new ConcurrentHashMap<>();
    private static final long DEFAULT_INTERVAL_NANOS = 500_000_000L;

    private AlienEvoRuntimeDiagnostics1201Runtime() {}

    public static boolean isEnabled() {
        return Boolean.getBoolean(DEBUG_PROPERTY);
    }

    public static void traceBeamAnchor(
            int entityId,
            String boneName,
            boolean firstPerson,
            float fovScale,
            Matrix3f inverseView,
            Vec3 cameraArgument,
            Matrix4f capturedPose,
            Vector3f scaledRotatedTranslation,
            Vec3 worldOrigin) {
        if (!isEnabled() || boneName == null || !boneName.toLowerCase(java.util.Locale.ROOT).startsWith("beam")) return;
        String key = "beam:" + entityId + ":" + boneName + ":" + firstPerson;
        if (!shouldLog(key, DEFAULT_INTERVAL_NANOS)) return;

        Vector3f rawTranslation = capturedPose == null ? null : capturedPose.getTranslation(new Vector3f());
        LOGGER.info("[AlienEvoRuntimeTrace] beamAnchor entityId={} bone={} firstPerson={} fovScale={} "
                        + "cameraArgument={} mainCamera={} inverseView={} capturedPoseTranslation={} "
                        + "capturedPose={} scaledRotatedTranslation={} worldOrigin={}",
                entityId, boneName, firstPerson, fovScale, cameraArgument, mainCameraSnapshot(),
                inverseView, rawTranslation, capturedPose, scaledRotatedTranslation, worldOrigin);
    }

    public static void traceMissingBeamAnchor(int entityId, String boneName, boolean firstPerson) {
        if (!isEnabled() || boneName == null || !boneName.toLowerCase(java.util.Locale.ROOT).startsWith("beam")) return;
        String key = "beam-missing:" + entityId + ":" + boneName + ":" + firstPerson;
        if (shouldLog(key, DEFAULT_INTERVAL_NANOS)) {
            LOGGER.warn("[AlienEvoRuntimeTrace] beamAnchorMissing entityId={} bone={} firstPerson={} "
                            + "reason=GeckoModelStorage returned null or its 250ms capture expired",
                    entityId, boneName, firstPerson);
        }
    }

    public static void traceKineceleranRun(Object builder, Object player, Object firstPersonContext) {
        if (!isEnabled() || player == null) return;
        String uuid = String.valueOf(invokeNoArgs(player, "getUUID"));
        Object firstPerson = invokeNoArgs(firstPersonContext, "firstPerson");
        String key = "kineceleran-run:" + uuid + ":" + firstPerson;
        if (!shouldLog(key, DEFAULT_INTERVAL_NANOS)) return;

        LOGGER.info("[AlienEvoRuntimeTrace] kineceleranRun firstPerson={} player={} animationOperations={}",
                firstPerson, playerSnapshot(player), describeAnimationBuilder(builder));
    }

    public static void traceFirstPersonArmApply(Object modelPartKind, Object animationData, Object modelPart) {
        if (!isEnabled()) return;
        String key = "fp-arm:" + String.valueOf(modelPartKind);
        if (!shouldLog(key, DEFAULT_INTERVAL_NANOS)) return;

        LOGGER.info("[AlienEvoRuntimeTrace] firstPersonArmApply part={} animationData={} finalModelPart={}",
                modelPartKind, describeArmAnimation(animationData), describeModelPart(modelPart));
    }

    /** rawProgress is the vanilla value; appliedProgress is the value returned by the suppressor mixin. */
    public static void traceVanillaSwing(float rawProgress, float appliedProgress) {
        if (!isEnabled() || rawProgress == 0.0F && appliedProgress == 0.0F) return;
        if (!shouldLog("vanilla-swing", 250_000_000L)) return;
        Object player = currentPlayer();
        LOGGER.info("[AlienEvoRuntimeTrace] firstPersonVanillaSwing rawProgress={} appliedProgress={} player={}",
                rawProgress, appliedProgress, player == null ? "null" : playerSnapshot(player));
    }

    private static boolean shouldLog(String key, long intervalNanos) {
        long now = System.nanoTime();
        Long previous = LAST_LOG_NANOS.put(key, now);
        return previous == null || now - previous >= intervalNanos;
    }

    private static Object currentPlayer() {
        try {
            return readField(Minecraft.getInstance(), "player");
        } catch (LinkageError | RuntimeException error) {
            return null;
        }
    }

    private static String mainCameraSnapshot() {
        try {
            Object minecraft = Minecraft.getInstance();
            Object renderer = readField(minecraft, "gameRenderer");
            Object camera = invokeNoArgs(renderer, "getMainCamera");
            Object position = invokeNoArgs(camera, "getPosition");
            if (position == null) position = invokeNoArgs(camera, "position");
            Object rotation = invokeNoArgs(camera, "rotation");
            return "position=" + position + ",rotation=" + rotation;
        } catch (LinkageError | RuntimeException error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    private static String playerSnapshot(Object player) {
        return "uuid=" + invokeNoArgs(player, "getUUID")
                + ",sprinting=" + invokeNoArgs(player, "isSprinting")
                + ",onGround=" + invokeNoArgs(player, "onGround")
                + ",velocity=" + invokeNoArgs(player, "getDeltaMovement")
                + ",walkAnimation=" + invokeNoArgs(readField(player, "walkAnimation"), "speed");
    }

    private static String describeAnimationBuilder(Object builder) {
        Object data = invokeNoArgs(builder, "getAnimationData");
        if (!(data instanceof Map<?, ?> parts)) return String.valueOf(data);

        StringBuilder result = new StringBuilder();
        for (Map.Entry<?, ?> part : parts.entrySet()) {
            if (result.length() > 0) result.append(';');
            result.append(part.getKey()).append('{');
            Object operationMap = readField(part.getValue(), "operations");
            if (operationMap instanceof Map<?, ?> operations) {
                boolean first = true;
                for (Map.Entry<?, ?> operationGroup : operations.entrySet()) {
                    Object values = operationGroup.getValue();
                    if (!(values instanceof Iterable<?> list)) continue;
                    for (Object operation : list) {
                        if (!first) result.append(',');
                        first = false;
                        result.append(operationGroup.getKey()).append(':')
                                .append(readField(operation, "type")).append('=')
                                .append(readField(operation, "value"));
                    }
                }
            } else {
                result.append("operations=").append(operationMap);
            }
            result.append('}');
        }
        return result.toString();
    }

    private static String describeArmAnimation(Object animationData) {
        if (animationData == null) return "null";
        return "xRotDeg=" + readField(animationData, "xRotDeg")
                + ",yRotDeg=" + readField(animationData, "yRotDeg")
                + ",zRotDeg=" + readField(animationData, "zRotDeg")
                + ",moveX=" + readField(animationData, "moveX")
                + ",moveY=" + readField(animationData, "moveY")
                + ",moveZ=" + readField(animationData, "moveZ");
    }

    private static String describeModelPart(Object modelPart) {
        if (modelPart == null) return "null";
        return "xRot=" + readField(modelPart, "xRot")
                + ",yRot=" + readField(modelPart, "yRot")
                + ",zRot=" + readField(modelPart, "zRot")
                + ",x=" + readField(modelPart, "x")
                + ",y=" + readField(modelPart, "y")
                + ",z=" + readField(modelPart, "z");
    }

    private static Object readField(Object target, String name) {
        if (target == null) return null;
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // Check the superclass.
            } catch (ReflectiveOperationException | RuntimeException error) {
                return "unavailable:" + error.getClass().getSimpleName();
            }
        }
        return null;
    }

    private static Object invokeNoArgs(Object target, String name) {
        if (target == null || target instanceof String) return null;
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Check the superclass.
            } catch (ReflectiveOperationException | RuntimeException error) {
                return "unavailable:" + error.getClass().getSimpleName();
            }
        }
        return null;
    }
}
