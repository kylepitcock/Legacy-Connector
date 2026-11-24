package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in observations for AlienEvo's Omnitrix transform-bubble coordinate
 * frames. Mixin redirects replay the original PoseStack translations unchanged.
 *
 * Enable with -Dlegacyconnector.debug.alienevoTransformBubble=true.
 */
public final class AlienEvoTransformBubbleDiagnostics1201Runtime {
    private static final String DEBUG_PROPERTY = "legacyconnector.debug.alienevoTransformBubble";
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/AlienEvoTransformBubbleTrace");
    private static final ConcurrentHashMap<String, Long> LAST_LOG_NANOS = new ConcurrentHashMap<>();
    private static final long LOG_INTERVAL_NANOS = 750_000_000L;
    private static final ThreadLocal<TraceContext> CURRENT = new ThreadLocal<>();

    private AlienEvoTransformBubbleDiagnostics1201Runtime() {}

    public static void traceRenderEntry(AbstractClientPlayer player, Vec3 from, Vec3 target,
            float progress, PoseStack poseStack, boolean firstPerson, float partialTick) {
        if (!isEnabled() || player == null || !allow(player.getUUID(), "entry")) return;
        LOGGER.info("[AlienEvoTransformBubbleTrace] renderEntry player={} firstPerson={} progress={} partialTick={} "
                        + "configuredFrom={} target={} incomingPose={}",
                player.getUUID(), firstPerson, progress, partialTick, from, target, pose(poseStack));
    }

    public static void beginTransformSphere(AbstractClientPlayer player, float progress,
            float partialTick, boolean firstPerson) {
        if (!isEnabled() || player == null) {
            CURRENT.remove();
            return;
        }
        CURRENT.set(new TraceContext(player.getUUID(), progress, partialTick, firstPerson));
    }

    public static void endTransformSphere() {
        CURRENT.remove();
    }

    public static void traceHeadCorrection(PoseStack poseStack, double x, double y, double z) {
        TraceContext context = CURRENT.get();
        if (!isEnabled() || context == null || !allow(context.playerId(), "head")) return;
        LOGGER.info("[AlienEvoTransformBubbleTrace] headAnchor player={} firstPerson={} progress={} partialTick={} "
                        + "appliedHeadCorrection=({}, {}, {}) poseAfterHeadCorrection={}",
                context.playerId(), context.firstPerson(), context.progress(), context.partialTick(),
                x, y, z, pose(poseStack));
    }

    public static void traceCameraOffset(PoseStack poseStack, double x, double y, double z) {
        TraceContext context = CURRENT.get();
        if (!isEnabled() || context == null || !context.firstPerson()
                || !allow(context.playerId(), "camera")) return;
        try {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vector3f lookVector = camera.getLookVector();
            Vector3f upVector = camera.getUpVector();
            Vector3f leftVector = camera.getLeftVector();
            Vec3 look = new Vec3(lookVector.x(), lookVector.y(), lookVector.z());
            Vec3 up = new Vec3(upVector.x(), upVector.y(), upVector.z());
            Vec3 negativeLeft = new Vec3(-leftVector.x(), -leftVector.y(), -leftVector.z());
            Vec3 offset = new Vec3(x, y, z);
            double forwardProjection = offset.dot(look);
            double upProjection = offset.dot(up);
            double leftProjection = offset.dot(negativeLeft);
            double inferredTanFovHalf = -upProjection / 2.3;
            LOGGER.info("[AlienEvoTransformBubbleTrace] firstPersonCamera player={} "
                            + "look={} up={} negativeLeft={} appliedCameraOffset={} "
                            + "forwardProjection={} upProjection={} leftProjection={} "
                            + "inferredTanFovHalf={} finalPoseForBeamSpikes={}",
                    context.playerId(), look, up, negativeLeft, offset,
                    forwardProjection, upProjection, leftProjection, inferredTanFovHalf, pose(poseStack));
        } catch (RuntimeException | LinkageError error) {
            LOGGER.warn("[AlienEvoTransformBubbleTrace] firstPersonCamera player={} unavailable={}",
                    context.playerId(), error.toString());
        }
    }

    private static boolean isEnabled() {
        return Boolean.getBoolean(DEBUG_PROPERTY);
    }

    private static boolean allow(UUID playerId, String phase) {
        String key = playerId + ":" + phase;
        long now = System.nanoTime();
        Long previous = LAST_LOG_NANOS.get(key);
        if (previous != null && now - previous < LOG_INTERVAL_NANOS) return false;
        LAST_LOG_NANOS.put(key, now);
        return true;
    }

    private static String pose(PoseStack poseStack) {
        if (poseStack == null) return "null";
        try {
            Matrix4f matrix = poseStack.last().pose();
            Vector3f translation = matrix.getTranslation(new Vector3f());
            return "translation=" + translation + ",matrix=" + matrix;
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    private record TraceContext(UUID playerId, float progress, float partialTick, boolean firstPerson) {}
}
