package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122;
import org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaEntities1122;

/**
 * Natural spawning as 1.12.2 mods change it.
 *
 * <p>The spawn lists of the 1.12 vanilla biomes start as the modern biome's own mob spawns (as views of the vanilla mob classes:
 * EntityZombie, EntityCow...), read from the vanilla registries the first time a list is asked for. What mods change in the
 * lists ({@code EntityRegistry.addSpawn}/{@code removeSpawn}, or editing {@code Biome.getSpawnableList} directly) becomes NeoForge
 * biome modifiers in the generated data pack: added entries {@code add_spawns}, removed vanilla entries {@code remove_spawns}, on
 * the modern biomes each legacy vanilla biome stands for. Mod biomes the modern game does not have are left out (a modifier naming
 * an unknown biome would fail to load).
 */
public final class Spawns1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Spawns1122");

    private Spawns1122() {}

    /** One spawn entry by value. */
    private record Entry(Class<?> cls, int weight, int min, int max) {
        static Entry of(Biome.SpawnListEntry e) {
            return new Entry(e.entityClass, e.itemWeight, e.minGroupCount, e.maxGroupCount);
        }
    }

    /** What each filled vanilla biome's lists held when they were filled. */
    private static final Map<Biome, Map<EnumCreatureType, List<Entry>>> BASELINE = new IdentityHashMap<>();
    private static volatile net.minecraft.core.HolderLookup.RegistryLookup<net.minecraft.world.level.biome.Biome> vanillaBiomes;
    private static final ThreadLocal<Boolean> FILLING = new ThreadLocal<>();

    // ------------------------------------------------------------------ vanilla biome lists

    /** Called by {@code Biome.getSpawnableList}: fills a 1.12 vanilla biome's lists from its modern biome the first time. */
    public static void ensureVanilla(Biome biome) {
        if (biome == null || FILLING.get() != null) return;
        var name = biome.getRegistryName();
        if (name == null || !"minecraft".equals(name.getNamespace())) return;
        synchronized (BASELINE) {
            if (BASELINE.containsKey(biome)) return;
            Map<EnumCreatureType, List<Entry>> baseline = new java.util.EnumMap<>(EnumCreatureType.class);
            BASELINE.put(biome, baseline);
            FILLING.set(Boolean.TRUE);
            try {
                List<ResourceLocation> modern = BiomeBridge1122.modernIds(biome);
                var settings = modern.isEmpty() ? null : mobSettings(modern.get(0));
                for (EnumCreatureType type : EnumCreatureType.values()) {
                    List<Biome.SpawnListEntry> list = biome.getSpawnableList(type);
                    if (settings != null) {
                        for (var data : settings.getMobs(mobCategory(type)).unwrap()) {
                            Class<?> cls = legacyClass(data.type);
                            if (cls == null) continue;
                            @SuppressWarnings("unchecked")
                            var entry = new Biome.SpawnListEntry((Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.Entity>) cls,
                                    data.getWeight().asInt(), data.minCount, data.maxCount);
                            list.add(entry);
                        }
                    }
                    List<Entry> copy = new ArrayList<>();
                    for (var e : list) copy.add(Entry.of(e));
                    baseline.put(type, copy);
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.debug("Could not read the vanilla spawns of {}: {}", name, e.toString());
            } finally {
                FILLING.remove();
            }
        }
    }

    private static Class<?> legacyClass(EntityType<?> type) {
        String legacy = VanillaEntities1122.mcc$legacyNameOf(type);
        return legacy == null ? null : VanillaEntities1122.mcc$legacyClass(legacy);
    }

    /** A vanilla biome's spawn settings as the game's own registries define them (built once). */
    private static net.minecraft.world.level.biome.MobSpawnSettings mobSettings(ResourceLocation id) {
        var biomes = vanillaBiomes;
        if (biomes == null) {
            synchronized (Spawns1122.class) {
                if (vanillaBiomes == null) {
                    vanillaBiomes = net.minecraft.data.registries.VanillaRegistries.createLookup().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
                }
                biomes = vanillaBiomes;
            }
        }
        return biomes.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, id))
                .map(h -> h.value().getMobSettings()).orElse(null);
    }

    // ------------------------------------------------------------------ biome modifiers

    private record AddKey(EntityType<?> type, boolean modEntity, EnumCreatureType creature, int weight, int min, int max) {}

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        try {
            legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS.getBiomeName(); // the vanilla biome views exist
        } catch (RuntimeException | LinkageError e) {
            // only mod biomes then
        }
        Map<AddKey, Set<String>> adds = new LinkedHashMap<>();
        Map<EntityType<?>, Set<String>> removes = new LinkedHashMap<>();
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
                List<Entry> baseline;
                synchronized (BASELINE) {
                    var b = BASELINE.get(biome);
                    baseline = b == null ? List.of() : b.getOrDefault(type, List.of());
                }
                List<Entry> now = new ArrayList<>();
                for (Biome.SpawnListEntry e : List.copyOf(list)) if (e != null && e.entityClass != null) now.add(Entry.of(e));
                // added (or changed) entries
                for (Entry e : now) {
                    if (e.weight() <= 0 || baseline.contains(e)) continue;
                    Hosts1122.Info info = Hosts1122.info(e.cls());
                    EntityType<?> et = info != null ? info.type() : VanillaEntities1122.mcc$modernTypeOf(e.cls());
                    if (et == null) continue;
                    int min = Math.max(1, e.min());
                    AddKey key = new AddKey(et, info != null, type, e.weight(), min, Math.max(min, e.max()));
                    Set<String> biomes = adds.computeIfAbsent(key, k -> new LinkedHashSet<>());
                    for (ResourceLocation id : modern) biomes.add(id.toString());
                }
                // vanilla entries a mod removed (a class no longer listed at all)
                for (Entry e : baseline) {
                    boolean still = false;
                    for (Entry n : now) if (n.cls() == e.cls()) still = true;
                    if (still) continue;
                    EntityType<?> et = VanillaEntities1122.mcc$modernTypeOf(e.cls());
                    if (et == null) continue;
                    Set<String> biomes = removes.computeIfAbsent(et, k -> new LinkedHashSet<>());
                    for (ResourceLocation id : modern) biomes.add(id.toString());
                }
            }
        }
        int n = 0;
        for (var entry : adds.entrySet()) {
            AddKey k = entry.getKey();
            if (k.modEntity()) adoptCategory(k.type(), mobCategory(k.creature()));
            ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(k.type());
            JsonObject json = new JsonObject();
            json.addProperty("type", "neoforge:add_spawns");
            json.add("biomes", array(entry.getValue()));
            JsonObject spawner = new JsonObject();
            spawner.addProperty("type", typeId.toString());
            spawner.addProperty("weight", k.weight());
            spawner.addProperty("minCount", k.min());
            spawner.addProperty("maxCount", k.max());
            json.add("spawners", spawner);
            put(out, "add_" + typeId.getNamespace() + "_" + typeId.getPath() + "_" + (n++), json);
        }
        int r = 0;
        for (var entry : removes.entrySet()) {
            ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey());
            JsonObject json = new JsonObject();
            json.addProperty("type", "neoforge:remove_spawns");
            json.add("biomes", array(entry.getValue()));
            json.addProperty("entity_types", typeId.toString());
            put(out, "remove_" + typeId.getNamespace() + "_" + typeId.getPath() + "_" + (r++), json);
        }
        if (n + r > 0) LOG.info("1.12.2 mob spawns: {} additions and {} removals applied to the modern biomes", n, r);
    }

    private static JsonArray array(Set<String> values) {
        JsonArray a = new JsonArray();
        values.forEach(a::add);
        return a;
    }

    private static void put(Map<String, Supplier<byte[]>> out, String name, JsonObject json) {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        out.put("data/legacyconnector/neoforge/biome_modifier/spawns1122/" + name + ".json", () -> bytes);
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
     * The natural spawner only spawns a mob in the category of its type, and a host type's category comes from the class
     * hierarchy: the kind the mod gave its spawn entry is authoritative (as in the 1.7.10 bridge). Vanilla types are left alone.
     */
    private static void adoptCategory(EntityType<?> type, net.minecraft.world.entity.MobCategory wanted) {
        if (type == null || wanted == null || type.getCategory() == wanted) return;
        try {
            java.lang.reflect.Field f = org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.getDeclaredField(EntityType.class, "category");
            org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.set(f, type, wanted);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Could not set the spawn category of {}: {}", type, e.toString());
        }
    }
}
