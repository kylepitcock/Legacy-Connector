package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development check ({@code -Dlegacyconnector.entitySelfTest=true}): spawns one of every legacy entity type near spawn,
 * round-trips it through save/load, and after 100 ticks reports which are alive, gone or failed.
 */
public final class EntitySelfTest {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/EntityTest");
    private static final List<Entity> SPAWNED = new ArrayList<>();
    /** Where each test entity started: after 100 ticks the distance walked shows mobs that move far too fast. */
    private static final Map<Entity, net.minecraft.world.phys.Vec3> START = new java.util.IdentityHashMap<>();
    private static final Map<String, String> RESULT = new TreeMap<>();
    private static int ticks = -1;

    private EntitySelfTest() {}

    public static void register() {
        if (!Boolean.getBoolean("legacyconnector.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> start(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick());
        // Other dev fixtures may stop the server early: report what there is.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> {
            if (ticks >= 0 && ticks < 100) {
                ticks = 99;
                tick();
            }
        });
    }

    private static final java.util.Set<Long> FORCED = new java.util.HashSet<>();
    private static int TYPES;

    /** The legacy object a host runs (1.7.10 or 1.12.2), or null. */
    private static Object legacyOf(Entity e) {
        if (e instanceof LegacyHost h) return h.mcc$legacy();
        if (e instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122 h) return h.mcc$legacy();
        return null;
    }

    private static void start(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int i = 0;
        java.util.Map<String, EntityType<?>> types = new java.util.LinkedHashMap<>();
        for (LegacyHosts.Info info : LegacyHosts.all()) types.put(info.id().toString(), info.type());
        for (var info : org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.all()) types.put(info.id().toString(), info.type());
        TYPES = types.size();
        for (var entry : types.entrySet()) {
            String name = entry.getKey();
            EntityType<?> type = entry.getValue();
            try {
                // away from the block self test, which runs at the same time around spawn (mobs broke its blocks)
                int x = spawn.getX() + 1000 + (i % 16) * 3 - 24;
                int z = spawn.getZ() + (i / 16) * 3 - 24;
                i++;
                // forced for the test: an unticketed chunk unloads whenever the server lags, and its entities counted as removed
                if (FORCED.add(net.minecraft.world.level.ChunkPos.asLong(x >> 4, z >> 4))) level.setChunkForced(x >> 4, z >> 4, true);
                level.getChunk(x >> 4, z >> 4);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                Entity e = type.create(level);
                if (e == null) {
                    RESULT.put(name, "create returned null");
                    continue;
                }
                if (legacyOf(e) == null) {
                    RESULT.put(name, "no legacy object (construction failed)");
                    continue;
                }
                e.moveTo(x + 0.5, y, z + 0.5, 0, 0);
                if (e instanceof net.minecraft.world.entity.Mob mob) {
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.COMMAND, null);
                }
                // Save and load into a fresh entity.
                CompoundTag tag = new CompoundTag();
                e.saveWithoutId(tag);
                Entity copy = type.create(level);
                if (copy == null) {
                    RESULT.put(name, "second create returned null");
                    continue;
                }
                copy.load(tag);
                if (!level.addFreshEntity(copy)) {
                    RESULT.put(name, "addFreshEntity refused");
                    continue;
                }
                SPAWNED.add(copy);
                START.put(copy, copy.position());
                RESULT.put(name, "spawned");
            } catch (RuntimeException | LinkageError ex) {
                RESULT.put(name, "spawn threw " + describe(ex));
            }
        }
        ticks = 0;
        LOG.info("Spawned {} legacy entities for the self test", SPAWNED.size());
    }

    /** Root cause plus the first frame outside Minecraft and the JDK (where the legacy code failed). */
    private static String describe(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String where = "";
        for (StackTraceElement e : root.getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("net.minecraft.") || c.startsWith("java.") || c.startsWith("jdk.") || c.startsWith("com.mojang.")) continue;
            where = " at " + c + "." + e.getMethodName() + ":" + e.getLineNumber();
            break;
        }
        return root + where;
    }

    private static void tick() {
        if (ticks < 0 || ++ticks != 100) return;
        int alive = 0, gone = 0, failed = 0;
        for (Entity e : SPAWNED) {
            String name = EntityType.getKey(e.getType()).toString();
            Object legacy = legacyOf(e);
            if (legacy == null || LegacyHosts.hadServerTickFailure(legacy.getClass())
                    || org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.hasFailed(legacy.getClass())) {
                failed++;
                RESULT.put(name, "FAILED (see log)");
            } else if (e.isAlive()) {
                alive++;
                RESULT.put(name, "alive at " + e.blockPosition().toShortString());
            } else {
                gone++;
                RESULT.put(name, "removed (" + e.getRemovalReason() + ")");
            }
        }
        java.util.List<String> fast = new ArrayList<>();
        for (Entity e : SPAWNED) {
            var from = START.get(e);
            if (from == null || !e.isAlive() || !(e instanceof net.minecraft.world.entity.Mob)) continue;
            double moved = Math.hypot(e.getX() - from.x, e.getZ() - from.z);
            fast.add(String.format(java.util.Locale.ROOT, "%08.2f %s", moved, EntityType.getKey(e.getType())));
        }
        java.util.Collections.sort(fast, java.util.Collections.reverseOrder());
        LOG.info("Mob walking distance in 100 ticks (blocks), fastest first: {}", fast.subList(0, Math.min(12, fast.size())));
        StringBuilder sb = new StringBuilder();
        RESULT.forEach((k, v) -> sb.append("\n  ").append(k).append(": ").append(v));
        if (!SPAWNED.isEmpty()) {
            var level = SPAWNED.get(0).level();
            if (level instanceof ServerLevel server) {
                for (long c : FORCED) server.setChunkForced(net.minecraft.world.level.ChunkPos.getX(c), net.minecraft.world.level.ChunkPos.getZ(c), false);
            }
        }
        FORCED.clear();
        LOG.info("ENTITY SELF TEST: {} alive, {} removed, {} failed, {} not spawned of {} types{}", alive, gone, failed,
                TYPES - SPAWNED.size(), TYPES, sb);
    }
}
