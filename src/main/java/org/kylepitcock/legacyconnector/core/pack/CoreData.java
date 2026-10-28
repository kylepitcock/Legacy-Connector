package org.kylepitcock.legacyconnector.core.pack;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.server.packs.PackType;

/** Version-independent generated data: the world generation hook that runs legacy chunk decorators, legacy fishing loot. */
public final class CoreData implements GeneratedPack.Contributor {
    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        put(out, "data/legacyconnector/worldgen/configured_feature/legacy_worldgen.json", "{\"type\":\"legacyconnector:legacy_worldgen\",\"config\":{}}");
        put(out, "data/legacyconnector/worldgen/placed_feature/legacy_worldgen.json", "{\"feature\":\"legacyconnector:legacy_worldgen\",\"placement\":[]}");
        put(out, "data/legacyconnector/worldgen/configured_feature/legacy_biome.json", "{\"type\":\"legacyconnector:legacy_biome\",\"config\":{}}");
        put(out, "data/legacyconnector/worldgen/placed_feature/legacy_biome.json", "{\"feature\":\"legacyconnector:legacy_biome\",\"placement\":[]}");
        // what 1.7.10 mods added to fishing (FishingHooks)
        put(out, "data/legacyconnector/loot_modifiers/legacy_fishing.json", org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyFishingLoot.MODIFIER_JSON);
        put(out, "data/neoforge/loot_modifiers/global_loot_modifiers.json", org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyFishingLoot.LIST_JSON);
        put(out, "data/legacyconnector/neoforge/biome_modifier/legacy_worldgen.json",
                "{\"type\":\"neoforge:add_features\",\"biomes\":{\"type\":\"neoforge:any\"},\"features\":\"legacyconnector:legacy_worldgen\",\"step\":\"underground_ores\"}");
    }

    private static void put(Map<String, Supplier<byte[]>> out, String path, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }

    @Override
    public String toString() {
        return "core";
    }
}
