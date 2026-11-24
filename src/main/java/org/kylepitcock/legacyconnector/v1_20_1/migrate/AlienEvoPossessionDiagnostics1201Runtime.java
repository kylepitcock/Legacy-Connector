package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in observations for the actual ItemInHandRenderer possession path.
 * This helper only reads state and writes a rate-limited log line.
 *
 * Enable with -Dlegacyconnector.debug.alienevoPossession=true.
 */
public final class AlienEvoPossessionDiagnostics1201Runtime {
    private static final String PROPERTY = "legacyconnector.debug.alienevoPossession";
    private static final String ABILITY = "net.habb.alienevo.ability.PossessionAbility";
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/AlienEvoPossessionTrace");
    private static final ConcurrentHashMap<String, Long> LAST_LOG = new ConcurrentHashMap<>();

    private AlienEvoPossessionDiagnostics1201Runtime() {}

    public static void traceHandRenderState(LocalPlayer player, String phase) {
        if (!Boolean.getBoolean(PROPERTY) || player == null) return;
        UUID id = player.getUUID();
        String key = id + ":" + phase;
        long now = System.nanoTime();
        Long previous = LAST_LOG.put(key, now);
        if (previous != null && now - previous < 500_000_000L) return;

        try {
            Class<?> ability = Class.forName(ABILITY, false, player.getClass().getClassLoader());
            Object clientEntity = invokeStatic(ability, "getClientEntity", Player.class, player);
            Object clientProfile = invokeStatic(ability, "getClientPlayerProfile", Player.class, player);
            Object victim = invokeStatic(ability, "getClientVictimUUID", Player.class, player);
            Object playerPossession = invokeStatic(ability, "isPlayerPossession", Player.class, player);
            Object renderingPossessed = staticField(ability, "renderingPossessed");
            Object possessorMainArm = staticField(ability, "possessorMainArm");
            Entity camera = Minecraft.getInstance().getCameraEntity();

            LOGGER.info("[AlienEvoPossessionTrace] handRender phase={} player={} cameraEntity={} "
                            + "clientPossessedEntity={} clientPlayerProfile={} clientVictimUUID={} "
                            + "isPlayerPossession={} renderingPossessed={} possessorMainArm={} "
                            + "playerMainArm={} invisible={} spectator={} mainHand={} offHand={}",
                    phase, describe(player), describe(camera), describe(clientEntity), clientProfile, victim,
                    playerPossession, renderingPossessed, possessorMainArm, player.getMainArm(),
                    player.isInvisible(), player.isSpectator(), player.getMainHandItem(), player.getOffhandItem());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.warn("[AlienEvoPossessionTrace] handRender phase={} player={} unavailable={}",
                    phase, id, error.toString());
        }
    }

    /**
     * Observes the real client attack/use input path while AlienEvo possession diagnostics
     * are enabled. This does not cancel input or alter the target/result.
     */
    public static void traceAttackInput(Minecraft minecraft, String phase, Boolean result) {
        if (!Boolean.getBoolean(PROPERTY) || minecraft == null || minecraft.player == null) return;
        LocalPlayer player = minecraft.player;
        UUID id = player.getUUID();
        String key = "input:" + phase;
        long now = System.nanoTime();
        Long previous = LAST_LOG.put(id + ":" + key, now);
        if (previous != null && now - previous < 500_000_000L) return;

        try {
            Class<?> ability = Class.forName(ABILITY, false, player.getClass().getClassLoader());
            Object clientEntity = invokeStatic(ability, "getClientEntity", Player.class, player);
            Object clientProfile = invokeStatic(ability, "getClientPlayerProfile", Player.class, player);
            Object victim = invokeStatic(ability, "getClientVictimUUID", Player.class, player);
            Object playerPossession = invokeStatic(ability, "isPlayerPossession", Player.class, player);
            Object renderingPossessed = staticField(ability, "renderingPossessed");
            Object possessorMainArm = staticField(ability, "possessorMainArm");
            Entity camera = minecraft.getCameraEntity();

            LOGGER.info("[AlienEvoPossessionTrace] input phase={} result={} player={} cameraEntity={} "
                            + "crosshair={} clientPossessedEntity={} clientPlayerProfile={} clientVictimUUID={} "
                            + "isPlayerPossession={} renderingPossessed={} possessorMainArm={} "
                            + "playerMainArm={} invisible={} spectator={} mainHand={} offHand={} "
                            + "usingItem={} useItem={}",
                    phase, result, describe(player), describe(camera), describeHit(minecraft.hitResult),
                    describe(clientEntity), clientProfile, victim, playerPossession, renderingPossessed,
                    possessorMainArm, player.getMainArm(), player.isInvisible(), player.isSpectator(),
                    player.getMainHandItem(), player.getOffhandItem(), player.isUsingItem(), player.getUseItem());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.warn("[AlienEvoPossessionTrace] input phase={} player={} unavailable={}",
                    phase, id, error.toString());
        }
    }

    private static String describeHit(HitResult hit) {
        if (hit == null) return "null";
        if (hit instanceof EntityHitResult entityHit) return "entity:" + describe(entityHit.getEntity());
        return hit.getType() + "@" + hit.getLocation();
    }

    private static Object invokeStatic(Class<?> type, String name, Class<?> parameter, Object argument)
            throws ReflectiveOperationException {
        Method method = type.getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        return method.invoke(null, argument);
    }

    private static Object staticField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static String describe(Object value) {
        if (!(value instanceof Entity entity)) return String.valueOf(value);
        return entity.getClass().getName() + "{uuid=" + entity.getUUID() + ",id=" + entity.getId()
                + ",pos=" + entity.position() + ",invisible=" + entity.isInvisible() + "}";
    }
}
