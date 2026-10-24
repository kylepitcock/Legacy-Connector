package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.EntityRegistry;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts;

/** Legacy {@code EntityRegistry.addSpawn} entries as NeoForge add_spawns biome modifiers. */
public final class LegacyEntitySpawns {
    /** Modern ids of the 1.7.10 vanilla biomes, by legacy biome id (mutated variants at id + 128). */
    private static final Map<Integer, String> VANILLA = Map.ofEntries(
            Map.entry(0, "ocean"), Map.entry(1, "plains"), Map.entry(2, "desert"), Map.entry(3, "windswept_hills"),
            Map.entry(4, "forest"), Map.entry(5, "taiga"), Map.entry(6, "swamp"), Map.entry(7, "river"),
            Map.entry(8, "nether_wastes"), Map.entry(9, "the_end"), Map.entry(10, "frozen_ocean"), Map.entry(11, "frozen_river"),
            Map.entry(12, "snowy_plains"), Map.entry(13, "snowy_plains"), Map.entry(14, "mushroom_fields"), Map.entry(15, "mushroom_fields"),
            Map.entry(16, "beach"), Map.entry(17, "desert"), Map.entry(18, "forest"), Map.entry(19, "taiga"),
            Map.entry(20, "windswept_hills"), Map.entry(21, "jungle"), Map.entry(22, "jungle"), Map.entry(23, "sparse_jungle"),
            Map.entry(24, "deep_ocean"), Map.entry(25, "stony_shore"), Map.entry(26, "snowy_beach"), Map.entry(27, "birch_forest"),
            Map.entry(28, "birch_forest"), Map.entry(29, "dark_forest"), Map.entry(30, "snowy_taiga"), Map.entry(31, "snowy_taiga"),
            Map.entry(32, "old_growth_pine_taiga"), Map.entry(33, "old_growth_pine_taiga"), Map.entry(34, "windswept_forest"),
            Map.entry(35, "savanna"), Map.entry(36, "savanna_plateau"), Map.entry(37, "badlands"), Map.entry(38, "wooded_badlands"),
            Map.entry(39, "badlands"), Map.entry(129, "sunflower_plains"), Map.entry(130, "desert"), Map.entry(131, "windswept_gravelly_hills"),
            Map.entry(132, "flower_forest"), Map.entry(133, "taiga"), Map.entry(134, "swamp"), Map.entry(140, "ice_spikes"),
            Map.entry(149, "jungle"), Map.entry(151, "sparse_jungle"), Map.entry(155, "old_growth_birch_forest"),
            Map.entry(156, "old_growth_birch_forest"), Map.entry(157, "dark_forest"), Map.entry(158, "snowy_taiga"),
            Map.entry(160, "old_growth_spruce_taiga"), Map.entry(161, "old_growth_spruce_taiga"), Map.entry(162, "windswept_gravelly_hills"),
            Map.entry(163, "windswept_savanna"), Map.entry(164, "windswept_savanna"), Map.entry(165, "eroded_badlands"),
            Map.entry(166, "wooded_badlands"), Map.entry(167, "badlands"));

    private static volatile Map<ResourceLocation, Integer> reverse;

    private LegacyEntitySpawns() {}

    /** The legacy biome a modern biome corresponds to (a generated mod biome, or the vanilla legacy biome). */
    public static BiomeGenBase legacyBiome(ResourceLocation modern) {
        if (modern == null) return null;
        BiomeGenBase mod = LegacyBiomes.byModernId(modern);
        if (mod != null) return mod;
        Map<ResourceLocation, Integer> r = reverse;
        if (r == null) {
            java.util.Map<ResourceLocation, Integer> m = new java.util.HashMap<>();
            new java.util.TreeMap<>(VANILLA).forEach((id, name) -> m.putIfAbsent(ResourceLocation.withDefaultNamespace(name), id));
            m.put(ResourceLocation.withDefaultNamespace("meadow"), 1);
            m.put(ResourceLocation.withDefaultNamespace("cherry_grove"), 4);
            m.put(ResourceLocation.withDefaultNamespace("grove"), 30);
            m.put(ResourceLocation.withDefaultNamespace("snowy_slopes"), 13);
            m.put(ResourceLocation.withDefaultNamespace("frozen_peaks"), 13);
            m.put(ResourceLocation.withDefaultNamespace("jagged_peaks"), 13);
            m.put(ResourceLocation.withDefaultNamespace("stony_peaks"), 3);
            m.put(ResourceLocation.withDefaultNamespace("mangrove_swamp"), 6);
            m.put(ResourceLocation.withDefaultNamespace("lush_caves"), 4);
            m.put(ResourceLocation.withDefaultNamespace("dripstone_caves"), 1);
            m.put(ResourceLocation.withDefaultNamespace("deep_dark"), 1);
            m.put(ResourceLocation.withDefaultNamespace("soul_sand_valley"), 8);
            m.put(ResourceLocation.withDefaultNamespace("crimson_forest"), 8);
            m.put(ResourceLocation.withDefaultNamespace("warped_forest"), 8);
            m.put(ResourceLocation.withDefaultNamespace("basalt_deltas"), 8);
            r = reverse = m;
        }
        Integer id = r.get(modern);
        if (id == null) {
            String path = modern.getPath();
            id = path.contains("ocean") ? 0 : path.contains("end") ? 9 : 1;
        }
        return BiomeGenBase.mcc$biomeOrOcean(id);
    }

    /** The modern biome a legacy biome generates as, or null. */
    public static ResourceLocation modernBiome(BiomeGenBase biome) {
        if (biome == null) return null;
        if (biome.mcc$isModBiome()) return biome.mcc$modernId();
        String v = VANILLA.get(biome.biomeID);
        return v == null ? null : ResourceLocation.withDefaultNamespace(v);
    }

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        Set<ResourceLocation> generated = new java.util.HashSet<>();
        for (BiomeGenBase b : LegacyBiomes.modBiomes()) generated.add(b.mcc$modernId());
        int n = 0;
        for (EntityRegistry.Spawn s : EntityRegistry.mcc$spawns()) {
            LegacyHosts.Info info = LegacyHosts.info(s.entityClass());
            if (info == null || s.weight() <= 0) continue;
            adoptCategory(info.type(), s.type().mcc$modern());
            Set<String> biomes = new LinkedHashSet<>();
            for (BiomeGenBase b : s.biomes()) {
                ResourceLocation id = modernBiome(b);
                if (id != null && (id.getNamespace().equals("minecraft") || generated.contains(id))) biomes.add(id.toString());
            }
            if (biomes.isEmpty()) continue;
            JsonObject json = new JsonObject();
            json.addProperty("type", "neoforge:add_spawns");
            JsonArray list = new JsonArray();
            biomes.forEach(list::add);
            json.add("biomes", list);
            JsonObject spawner = new JsonObject();
            spawner.addProperty("type", info.id().toString());
            spawner.addProperty("weight", s.weight());
            spawner.addProperty("minCount", Math.max(1, s.min()));
            spawner.addProperty("maxCount", Math.max(Math.max(1, s.min()), s.max()));
            json.add("spawners", spawner);
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/legacyconnector/neoforge/biome_modifier/spawns/" + info.id().getNamespace() + "_" + info.id().getPath() + "_" + (n++) + ".json", () -> bytes);
        }
    }

    /**
     * Entity types are built before a mod says what kind of creature it spawns (the category comes from the class
     * hierarchy), and the natural spawner only runs the categories it knows: a mob registered as MISC would never
     * spawn. The kind the mod passed to addSpawn is authoritative.
     */
    private static void adoptCategory(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.entity.MobCategory wanted) {
        if (type == null || wanted == null || type.getCategory() == wanted) return;
        try {
            java.lang.reflect.Field f = org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.getDeclaredField(net.minecraft.world.entity.EntityType.class, "category");
            org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.set(f, type, wanted);
        } catch (ReflectiveOperationException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Spawns").warn("Could not set the spawn category of {}: {}", type, e.toString());
        }
    }
}
