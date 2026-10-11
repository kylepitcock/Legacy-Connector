package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyItemHolder;
import org.kylepitcock.mccompat.v1_7_10.entity.LegacyHosts;
import org.slf4j.LoggerFactory;

/** Logs, once the server has started, how many items, blocks and entity types each legacy mod registered. */
public final class LegacyContentSummary {
    private LegacyContentSummary() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> log());
    }

    private static void log() {
        Map<String, int[]> byMod = new TreeMap<>();
        BuiltInRegistries.ITEM.entrySet().forEach(e -> {
            if (e.getValue() instanceof LegacyItemHolder) byMod.computeIfAbsent(e.getKey().location().getNamespace(), k -> new int[3])[0]++;
        });
        BuiltInRegistries.BLOCK.entrySet().forEach(e -> {
            if (e.getValue() instanceof LegacyBlockAdapter) byMod.computeIfAbsent(e.getKey().location().getNamespace(), k -> new int[3])[1]++;
        });
        for (LegacyHosts.Info i : LegacyHosts.all()) byMod.computeIfAbsent(i.id().getNamespace(), k -> new int[3])[2]++;
        StringBuilder sb = new StringBuilder();
        int items = 0, blocks = 0, entities = 0;
        for (var e : byMod.entrySet()) {
            int[] c = e.getValue();
            items += c[0];
            blocks += c[1];
            entities += c[2];
            sb.append(String.format("%n  %-28s %5d items %5d blocks %4d entities", e.getKey(), c[0], c[1], c[2]));
        }
        LoggerFactory.getLogger("Retrofit/Content").info("Legacy content: {} mods, {} items, {} blocks, {} entity types{}", byMod.size(), items, blocks,
                entities, sb);
    }
}
