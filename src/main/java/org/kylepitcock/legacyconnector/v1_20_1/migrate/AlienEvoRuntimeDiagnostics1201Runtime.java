package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
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

    public static void traceRollSinceRegistration(String parser, boolean registered, String detail) {
        if (!isEnabled()) return;
        String key = "roll-since-registration:" + parser + ":" + registered;
        if (!shouldLog(key, DEFAULT_INTERVAL_NANOS)) return;
        if (registered) {
            LOGGER.info("[AlienEvoRuntimeTrace] rollSinceRegistration parser={} registered=true detail={}",
                    parser, detail);
        } else {
            LOGGER.warn("[AlienEvoRuntimeTrace] rollSinceRegistration parser={} registered=false detail={}",
                    parser, detail);
        }
    }

    public static void traceAzureRollSince(Object entity, double animationTime, double keyframeSeconds, float result) {
        if (!isEnabled() || entity == null) return;
        try {
            String uuid = String.valueOf(invokeNoArgs(entity, "getUUID"));
            String key = "azure-roll-since:" + uuid;
            if (!shouldLog(key, DEFAULT_INTERVAL_NANOS)) return;
            LOGGER.info("[AlienEvoRuntimeTrace] rollSinceEvaluate parser=AzureLib entity={} animationTime={} "
                            + "keyframeSeconds={} result={}",
                    uuid, animationTime, keyframeSeconds, result);
        } catch (Throwable ignored) {
            // Diagnostics must never change the animation result.
        }
    }

    public static void traceCannonboltSpinGate(Object player, boolean enabled, Object state) {
        if (!isEnabled() || player == null) return;
        try {
            String uuid = String.valueOf(invokeNoArgs(player, "getUUID"));
            String key = "cannonbolt-spin-gate:" + uuid + ":" + enabled;
            if (!shouldLog(key, DEFAULT_INTERVAL_NANOS)) return;
            Object anchors = readField(state, "rollAnchors");
            int anchorCount = anchors instanceof Map<?, ?> map ? map.size() : -1;
            LOGGER.info("[AlienEvoRuntimeTrace] cannonboltSpinGate enabled={} player={} state={}",
                    enabled, playerSnapshot(player),
                    "lastTick=" + readField(state, "lastTick")
                            + ",ticksSinceEnable=" + readField(state, "ticksSinceEnable")
                            + ",phasePrev=" + readField(state, "phasePrev")
                            + ",phaseCurr=" + readField(state, "phaseCurr")
                            + ",justStartedSpinning=" + readField(state, "justStartedSpinning")
                            + ",rollAnchorCount=" + anchorCount);
        } catch (Throwable ignored) {
            // The probe is observational only and must not interrupt rendering.
        }
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

    /**
     * Captures the exact bounds and fit inputs used by AlienEvo's off-screen Badge
     * portrait renderer. This is observational and disabled unless the runtime
     * diagnostics system property is enabled.
     */
    public static void traceBadgePortraitDraw(
            Object entity,
            Object probeKey,
            float[] outline,
            float x,
            float y,
            float size,
            float depthScale,
            boolean hologram) {
        if (!isEnabled() || entity == null) return;
        try {
            String uuid = String.valueOf(invokeNoArgs(entity, "getUUID"));
            String key = "badge-portrait:" + uuid + ":" 
                    + (probeKey == null ? "null" : probeKey.getClass().getName());
            if (!shouldLog(key, DEFAULT_INTERVAL_NANOS)) return;
            LOGGER.info("[AlienEvoRuntimeTrace] badgePortraitDraw entity={} probeKey={} hologram={} "
                            + "fitOrigin=({}, {}) size={} depthScale={} outline[minX,maxX,minY,maxY]={} "
                            + "entityState={}",
                    uuid, probeKey, hologram, x, y, size, depthScale,
                    Arrays.toString(outline), describePortraitEntity(entity));
        } catch (Throwable ignored) {
            // Portrait diagnostics must never interrupt a GUI render.
        }
    }

    /**
     * Captures the real Badge screen render boundary, including page/widget
     * fields and direct call sites. This helps identify the self-portrait path
     * when the lower-level MobPortrait probe is not reached.
     */
    public static void traceBadgeScreenRender(
            Object screen,
            Object graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            String callSites) {
        if (!isEnabled() || screen == null) return;
        try {
            Object player = Minecraft.getInstance().player;
            String playerId = player == null ? "none" : String.valueOf(invokeNoArgs(player, "getUUID"));
            String key = "badge-screen-render:" + System.identityHashCode(screen);
            if (!shouldLog(key, DEFAULT_INTERVAL_NANOS)) return;
            LOGGER.info("[AlienEvoRuntimeTrace] badgeScreenRender screenClass={} player={} "
                            + "gui={}x{} mouse=({}, {}) partialTick={} screenFields={} callSites={}",
                    screen.getClass().getName(), playerId,
                    invokeNoArgs(graphics, "guiWidth"), invokeNoArgs(graphics, "guiHeight"),
                    mouseX, mouseY, partialTick,
                    describeBadgeScreenFields(screen), callSites);
        } catch (Throwable ignored) {
            // Badge diagnostics must never interrupt GUI rendering.
        }
    }

    private static String describeBadgeScreenFields(Object screen) {
        StringBuilder fields = new StringBuilder();
        for (Class<?> type = screen.getClass();
                type != null && type != Object.class && !type.getName().startsWith("net.minecraft.");
                type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                if (fields.length() >= 2400) {
                    fields.append(" ...");
                    return fields.toString();
                }
                if (fields.length() > 0) fields.append(", ");
                fields.append(type.getSimpleName()).append('.').append(field.getName()).append('=');
                try {
                    field.setAccessible(true);
                    fields.append(summarizeBadgeField(field.get(screen)));
                } catch (Throwable inaccessible) {
                    fields.append("unavailable:").append(inaccessible.getClass().getSimpleName());
                }
            }
        }
        return fields.length() == 0 ? "none" : fields.toString();
    }

    private static String summarizeBadgeField(Object value) {
        if (value == null
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value.getClass().isEnum()) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) return value.getClass().getSimpleName() + "(size=" + map.size() + ")";
        if (value instanceof java.util.Collection<?> collection) {
            Object first = collection.isEmpty() ? null : collection.iterator().next();
            return collection.getClass().getSimpleName() + "(size=" + collection.size()
                    + ",first=" + (first == null ? "none" : first.getClass().getName()) + ")";
        }
        if (value instanceof net.minecraft.world.entity.Entity entity) {
            return value.getClass().getName() + "(" + entity.getUUID() + ",type=" + entity.getType()
                    + ",bounds=" + entity.getBoundingBox() + ")";
        }
        return value.getClass().getName();
    }

    /**
     * Captures the legacy anchor/cursor contract and the 1.21.1 clipping rectangle
     * used by the regular Omnitrix Power Screen preview.
     */
    public static void traceOmnitrixPowerPreview(
            Object entity,
            int anchorX,
            int anchorY,
            int scale,
            int guiWidth,
            int guiHeight,
            float legacyMouseX,
            float legacyMouseY,
            float absoluteMouseX,
            float absoluteMouseY,
            int left,
            int top,
            int right,
            int bottom) {
        if (!isEnabled() || entity == null) return;
        try {
            String uuid = String.valueOf(invokeNoArgs(entity, "getUUID"));
            if (!shouldLog("omnitrix-preview:" + uuid, DEFAULT_INTERVAL_NANOS)) return;
            LOGGER.info("[AlienEvoRuntimeTrace] omnitrixPowerPreview entity={} anchor=({}, {}) scale={} "
                            + "gui={}x{} legacyMouseDelta=({}, {}) absoluteMouse=({}, {}) "
                            + "viewport=[{},{}..{},{}] entityState={}",
                    uuid, anchorX, anchorY, scale, guiWidth, guiHeight,
                    legacyMouseX, legacyMouseY, absoluteMouseX, absoluteMouseY,
                    left, top, right, bottom, describePortraitEntity(entity));
        } catch (Throwable ignored) {
            // Preview diagnostics must never interrupt a GUI render.
        }
    }

    private static String describePortraitEntity(Object entity) {
        return "type=" + invokeNoArgs(entity, "getType")
                + ",pose=" + invokeNoArgs(entity, "getPose")
                + ",bbox=" + invokeNoArgs(entity, "getBoundingBox")
                + ",width=" + invokeNoArgs(entity, "getBbWidth")
                + ",height=" + invokeNoArgs(entity, "getBbHeight")
                + ",eyeHeight=" + invokeNoArgs(entity, "getEyeHeight")
                + ",position=(" + invokeNoArgs(entity, "getX") + ","
                + invokeNoArgs(entity, "getY") + "," + invokeNoArgs(entity, "getZ") + ")";
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
