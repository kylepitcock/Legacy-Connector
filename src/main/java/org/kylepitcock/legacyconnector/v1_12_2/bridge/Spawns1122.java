package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122;

/**
 * Natural spawning of 1.12.2 mod mobs: what mods put in the legacy biomes' spawn lists ({@code EntityRegistry.addSpawn}, or
 * adding to {@code Biome.getSpawnableList} directly) becomes NeoForge {@code add_spawns} biome modifiers in the generated data
 * pack, one per mob and spawn settings, on the modern biomes each legacy vanilla biome stands for. Mod biomes the modern game
 * does not have are left out (a modifier naming an unknown biome would fail to load).
 */
public final class Spawns1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Spawns1122");

    private Spawns1122() {}

    private record Key(Hosts1122.Info info, EnumCreatureType type, int weight, int min, int max) {}

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        try {
            legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS.getBiomeName(); // the vanilla biome views exist
        } catch (RuntimeException | LinkageError e) {
            // only mod biomes then
        }
        Map<Key, Set<String>> spawns = new LinkedHashMap<>();
        for (Biome biome : Registries1122.BIOMES.getValues()) {
            List<ResourceLocation> modern = BiomeBridge1122.modernIds(biome);
            if (modern.isEmpty()) continue;
            for (EnumCreatureType type : EnumCreatureType.values()) {
                List<Biome.SpawnListEntry> list;
                try {
                    list = biome.getSpawnableList(type);
                } catch (RuntimeException | LinkageError e) {
                    continue;
                }
                if (list == null) continue;
                for (Biome.SpawnListEntry e : List.copyOf(list)) {
                    if (e == null || e.entityClass == null || e.itemWeight <= 0) continue;
                    Hosts1122.Info info = Hosts1122.info(e.entityClass);
                    if (info == null || info.type() == null) continue;
                    int min = Math.max(1, e.minGroupCount);
                    Key key = new Key(info, type, e.itemWeight, min, Math.max(min, e.maxGroupCount));
                    Set<String> biomes = spawns.computeIfAbsent(key, k -> new LinkedHashSet<>());
                    for (ResourceLocation id : modern) biomes.add(id.toString());
                }
            }
        }
        int n = 0;
        for (var entry : spawns.entrySet()) {
            Key k = entry.getKey();
            adoptCategory(k.info().type(), mobCategory(k.type()));
            JsonObject json = new JsonObject();
            json.addProperty("type", "neoforge:add_spawns");
            JsonArray biomes = new JsonArray();
            entry.getValue().forEach(biomes::add);
            json.add("biomes", biomes);
            JsonObject spawner = new JsonObject();
            spawner.addProperty("type", k.info().id().toString());
            spawner.addProperty("weight", k.weight());
            spawner.addProperty("minCount", k.min());
            spawner.addProperty("maxCount", k.max());
            json.add("spawners", spawner);
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            ResourceLocation id = k.info().id();
            out.put("data/legacyconnector/neoforge/biome_modifier/spawns1122/" + id.getNamespace() + "_" + id.getPath() + "_" + (n++) + ".json", () -> bytes);
        }
        if (n > 0) LOG.info("{} 1.12.2 mob spawn entries added to the modern biomes", n);
    }

    public static net.minecraft.world.entity.MobCategory mobCategory(EnumCreatureType type) {
        return switch (type) {
            case MONSTER -> net.minecraft.world.entity.MobCategory.MONSTER;
            case CREATURE -> net.minecraft.world.entity.MobCategory.CREATURE;
            case AMBIENT -> net.minecraft.world.entity.MobCategory.AMBIENT;
            case WATER_CREATURE -> net.minecraft.world.entity.MobCategory.WATER_CREATURE;
        };
    }

    /**
     * The natural spawner only spawns a mob in the category of its type, and the host type's category comes from the class
     * hierarchy: the kind the mod gave its spawn entry is authoritative (as in the 1.7.10 bridge).
     */
    private static void adoptCategory(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.entity.MobCategory wanted) {
        if (type == null || wanted == null || type.getCategory() == wanted) return;
        try {
            java.lang.reflect.Field f = org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.getDeclaredField(net.minecraft.world.entity.EntityType.class, "category");
            org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.set(f, type, wanted);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Could not set the spawn category of {}: {}", type, e.toString());
        }
    }
}
