package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in observations for AlienEvo's Omnitrix transform-bubble coordinate
 * frames. The hooks only read the already-computed values and never alter them.
 *
 * Enable with -Dlegacyconnector.debug.alienevoTransformBubble=true.
 */
public final class AlienEvoTransformBubbleDiagnostics1201Runtime {
    private static final String DEBUG_PROPERTY = "legacyconnector.debug.alienevoTransformBubble";
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/AlienEvoTransformBubbleTrace");
    private static final ConcurrentHashMap<String, Long> LAST_LOG_NANOS = new ConcurrentHashMap<>();
    private static final long LOG_INTERVAL_NANOS = 750_000_000L;

    private AlienEvoTransformBubbleDiagnostics1201Runtime() {}

    public static void traceRenderEntry(AbstractClientPlayer player, Vec3 from, Vec3 target,
            float progress, PoseStack poseStack, boolean firstPerson, float partialTick) {
        if (!isEnabled() || player == null || !allow(player.getUUID(), "entry", firstPerson)) return;
        LOGGER.info("[AlienEvoTransformBubbleTrace] renderEntry player={} firstPerson={} progress={} partialTick={} "
                        + "configuredFrom={} target={} incomingPose={}",
                player.getUUID(), firstPerson, progress, partialTick, from, target, pose(poseStack));
    }

    public static void traceHeadAnchor(AbstractClientPlayer player, Vec3 headPosition, Vec3 playerPosition,
            double worldY, float boundingHeight, float modelHeightScale, boolean firstPerson,
            float partialTick, PoseStack poseStack) {
        if (!isEnabled() || player == null || headPosition == null || playerPosition == null
                || !allow(player.getUUID(), "head", firstPerson)) return;
        double dx = playerPosition.x - headPosition.x;
        double dy = worldY - headPosition.y;
        double dz = playerPosition.z - headPosition.z;
        LOGGER.info("[AlienEvoTransformBubbleTrace] headAnchor player={} firstPerson={} partialTick={} "
                        + "headWorld={} playerWorld={} selectedWorldY={} headCorrection=({}, {}, {}) "
                        + "bbHeight={} modelHeightScale={} poseAfterHeadCorrection={}",
                player.getUUID(), firstPerson, partialTick, headPosition, playerPosition, worldY,
                dx, dy, dz, boundingHeight, modelHeightScale, pose(poseStack));
    }

    public static void traceCameraOffset(AbstractClientPlayer player, Object camera,
            Vec3 look, Vec3 up, Vec3 negativeLeft, double lookDistance, double fovTan,
            double verticalOffset, float fovScale, PoseStack poseStack) {
        if (!isEnabled() || player == null || look == null || up == null || negativeLeft == null
                || !allow(player.getUUID(), "camera", true)) return;
        Vec3 offset = look.scale(lookDistance).subtract(up.scale(verticalOffset));
        LOGGER.info("[AlienEvoTransformBubbleTrace] firstPersonCamera player={} camera={} "
                        + "look={} up={} negativeLeft={} lookDistance={} tanFovHalf={} verticalOffset={} "
                        + "computedCameraOffset={} fovScale={} finalPoseForBeamSpikes={}",
                player.getUUID(), camera, look, up, negativeLeft, lookDistance, fovTan,
                verticalOffset, offset, fovScale, pose(poseStack));
    }

    private static boolean isEnabled() {
        return Boolean.getBoolean(DEBUG_PROPERTY);
    }

    private static boolean allow(UUID playerId, String phase, boolean firstPerson) {
        String key = String.valueOf(playerId) + ":" + phase + ":" + firstPerson;
        long now = System.nanoTime();
        Long previous = LAST_LOG_NANOS.put(key, now);
        return previous == null || now - previous >= LOG_INTERVAL_NANOS;
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
}
