package org.kylepitcock.legacyconnector.core;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development aid: every 30 s, the number of loaded entities and chunks per level and the most common entity types, so runaway
 * spawning or chunk loading shows in the log. On with -Dlegacyconnector.memoryLog=true, the screenshot test and the server self
 * test.
 */
public final class EntityCensus {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Census");
    private static int ticks;

    private EntityCensus() {}

    private static int count(Iterable<?> it) {
        int n = 0;
        for (Object o : it) n++;
        return n;
    }

    public static void register() {
        if (!Boolean.getBoolean("legacyconnector.memoryLog") && System.getProperty("legacyconnector.shotScenario") == null
                && !Boolean.getBoolean("legacyconnector.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (++ticks % 600 != 0) return;
            int levels = 0, allEntities = 0, allChunks = 0;
            for (ServerLevel level : e.getServer().getAllLevels()) {
                levels++;
                allEntities += level.getEntities().getAll() instanceof Iterable<?> it ? count(it) : 0;
                allChunks += level.getChunkSource().getLoadedChunksCount();
            }
            LOG.info("Census: {} levels, {} entities, {} loaded chunks in all", levels, allEntities, allChunks);
            for (ServerLevel level : e.getServer().getAllLevels()) {
                Map<String, Integer> types = new HashMap<>();
                int total = 0;
                for (var entity : level.getAllEntities()) {
                    total++;
                    types.merge(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), 1, Integer::sum);
                }
                int chunks = level.getChunkSource().getLoadedChunksCount();
                if (total < 50 && chunks < 500) continue;
                var top = types.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5).toList();
                LOG.info("Census {}: {} entities, {} loaded chunks; most: {}", level.dimension().location(), total, chunks, top);
            }
        });
    }
}
