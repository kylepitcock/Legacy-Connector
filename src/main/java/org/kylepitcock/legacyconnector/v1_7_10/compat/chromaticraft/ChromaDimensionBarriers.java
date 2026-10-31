package org.kylepitcock.legacyconnector.v1_7_10.compat.chromaticraft;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MOD-SPECIFIC COMPAT (mod id "chromaticraft"): ChromatiCraft's coremod patches {@code World.getCollidingBoundingBoxes} so that, in
 * its dimension, "blocked air" (the invisible walls around biomes a player has not unlocked yet) is solid for players, and it tells
 * the player why ({@code ChromaDimensionManager.onPlayerBlockedFromBiome}). The modern engine computes collisions itself, so the
 * wall is enforced here instead: every tick, a player standing inside one of the mod's surrogate collision boxes is moved back to
 * the last position that was free. The boxes come from the mod's own static {@code ChromaAux.getSurrogateCollidingAABBs}. A no-op
 * without the mod and outside its dimension; remove this class and its one call in {@code LegacyConnector} to drop the compat.
 * (The coremod's other patches, the ore-clip noclip ability and sky-river slow fall, are client-prediction only and stay unmet.)
 */
public final class ChromaDimensionBarriers {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ChromatiCraft");
    private static final String MOD_ID = "chromaticraft";

    private static volatile boolean resolved;
    private static volatile boolean absent;
    private static Method surrogateBoxes;
    private static final Map<UUID, double[]> LAST_FREE = new ConcurrentHashMap<>();
    private static int failures;

    private ChromaDimensionBarriers() {}

    public static void register(IEventBus game) {
        game.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
    }

    private static boolean ready() {
        if (resolved) return !absent;
        LegacyModRecord rec = LegacyMods.byLegacyId(MOD_ID);
        if (rec == null) {
            absent = true;
            resolved = true;
            return false;
        }
        if (rec.mainClass == null) return false;
        synchronized (ChromaDimensionBarriers.class) {
            if (resolved) return !absent;
            try {
                Class<?> aux = Class.forName("Reika.ChromatiCraft.Auxiliary.ChromaAux", true, rec.mainClass.getClassLoader());
                for (Method m : aux.getMethods()) {
                    if (m.getName().equals("getSurrogateCollidingAABBs") && m.getParameterCount() == 3) surrogateBoxes = m;
                }
                if (surrogateBoxes == null) throw new NoSuchMethodException("ChromaAux.getSurrogateCollidingAABBs");
            } catch (ReflectiveOperationException | LinkageError e) {
                LOG.warn("ChromatiCraft's dimension barriers are not available: {}", e.toString());
                absent = true;
            }
            resolved = true;
            return !absent;
        }
    }

    private static void tick(net.minecraft.server.MinecraftServer server) {
        if (resolved && absent) return;
        if (resolved == false && LegacyMods.byLegacyId(MOD_ID) == null) {
            absent = true;
            resolved = true;
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var key = player.level().dimension().location();
            if (!key.getNamespace().equals("legacyconnector") || !key.getPath().startsWith("dim_")) continue;
            if (player.isSpectator() || player.getAbilities().instabuild) continue;
            if (!ready()) return;
            try {
                check(player);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                if (failures++ < 3) LOG.warn("ChromatiCraft barrier check failed: {}", e.toString());
            }
        }
    }

    private static void check(ServerPlayer player) throws ReflectiveOperationException {
        AABB box = player.getBoundingBox();
        Object world = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(player.level());
        Object legacyPlayer = EntityBridge.wrap(player);
        Object legacyBox = legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.mcc$of(box);
        Object result = surrogateBoxes.invoke(null, world, legacyPlayer, legacyBox);
        boolean blocked = result instanceof List<?> list && !list.isEmpty() && inside(list, box);
        if (!blocked) {
            LAST_FREE.put(player.getUUID(), new double[] {player.getX(), player.getY(), player.getZ()});
            return;
        }
        double[] back = LAST_FREE.get(player.getUUID());
        if (back == null) return;
        player.connection.teleport(back[0], back[1], back[2], player.getYRot(), player.getXRot());
        player.setDeltaMovement(0, 0, 0);
    }

    private static boolean inside(List<?> boxes, AABB player) {
        for (Object o : boxes) {
            if (o instanceof legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB b
                    && b.maxX > player.minX && b.minX < player.maxX && b.maxY > player.minY && b.minY < player.maxY
                    && b.maxZ > player.minZ && b.minZ < player.maxZ) return true;
        }
        return false;
    }
}
