package org.kylepitcock.mccompat.core.pack;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.server.packs.PackType;

/** Version-independent generated data: the world generation hook that runs legacy chunk decorators. */
public final class CoreData implements GeneratedPack.Contributor {
    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        put(out, "data/retrofit/worldgen/configured_feature/legacy_worldgen.json", "{\"type\":\"retrofit:legacy_worldgen\",\"config\":{}}");
        put(out, "data/retrofit/worldgen/placed_feature/legacy_worldgen.json", "{\"feature\":\"retrofit:legacy_worldgen\",\"placement\":[]}");
        put(out, "data/retrofit/worldgen/configured_feature/legacy_biome.json", "{\"type\":\"retrofit:legacy_biome\",\"config\":{}}");
        put(out, "data/retrofit/worldgen/placed_feature/legacy_biome.json", "{\"feature\":\"retrofit:legacy_biome\",\"placement\":[]}");
        put(out, "data/retrofit/neoforge/biome_modifier/legacy_worldgen.json",
                "{\"type\":\"neoforge:add_features\",\"biomes\":{\"type\":\"neoforge:any\"},\"features\":\"retrofit:legacy_worldgen\",\"step\":\"underground_ores\"}");
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
