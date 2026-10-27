package org.kylepitcock.legacyconnector.v1_7_10.dimension;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod dimensions. A dimension a legacy mod registers with {@code DimensionManager.registerDimension(id, type)}
 * becomes a modern dimension {@code legacyconnector:dim_<id>} whose chunk generator ({@link LegacyChunkGenerator}) runs
 * the mod's {@code WorldProvider.createChunkGenerator()}, whose biomes come from the provider's chunk manager, and
 * whose world provider object is an instance of the mod's provider class.
 */
public final class LegacyDimensions {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Dimensions");
    private static final String NAMESPACE = "legacyconnector";
    private static final String PREFIX = "dim_";
    /** The dimension special effects every mod dimension names (registered on the client). */
    public static final ResourceLocation EFFECTS = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "legacy_dimension");

    private LegacyDimensions() {}

    public static ResourceLocation id(int dimension) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, PREFIX + dimension);
    }

    public static ResourceKey<Level> levelKey(int dimension) {
        return ResourceKey.create(Registries.DIMENSION, id(dimension));
    }

    /** The legacy dimension id of a {@code legacyconnector:dim_<id>} location, or null. */
    public static Integer idOf(ResourceLocation location) {
        if (!NAMESPACE.equals(location.getNamespace()) || !location.getPath().startsWith(PREFIX)) return null;
        try {
            return Integer.parseInt(location.getPath().substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The provider of a mod dimension's level (null before the level exists). */
    public static WorldProvider provider(int dimension) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        var level = server.getLevel(levelKey(dimension));
        if (level == null) return null;
        if (BROKEN.contains(dimension)) return null;
        try {
            return legacyconnector.v1_7_10.net.minecraft.world.World.mcc$get$provider(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level));
        } catch (RuntimeException | LinkageError e) {
            // A mod's provider that cannot be built here (Mystcraft's needs the age data of a real age) must not take the
            // server down: the dimension generates with the fallback provider's defaults.
            if (BROKEN.add(dimension)) LOG.error("The world provider of dimension {} cannot be created; using defaults", dimension, e);
            return null;
        }
    }

    private static final java.util.Set<Integer> BROKEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** The provider object of a level: the mod's class for mod dimensions, the shim's plain one otherwise. */
    public static WorldProvider newProvider(Level level) {
        Integer id = idOf(level.dimension().location());
        if (id != null) {
            Class<?> cls = DimensionManager.mcc$modDimensions().get(id);
            if (cls != null) {
                try {
                    var ctor = cls.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    WorldProvider p = (WorldProvider) ctor.newInstance();
                    p.setDimension(id);
                    return p;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    LOG.error("Could not create the world provider {} of dimension {}", cls.getName(), id, e);
                }
            }
        }
        return new WorldProvider(level.dimension(), level.dimensionType().hasSkyLight(), level.dimensionType().ultraWarm());
    }

    /** The dimension type of a mod dimension: 1.7.10's 0-255 world with the provider's sky and ambient light. */
    static JsonObject dimensionType(boolean sky, float ambient) {
        JsonObject type = new JsonObject();
        type.addProperty("ultrawarm", false);
        type.addProperty("natural", true);
        type.addProperty("coordinate_scale", 1.0);
        type.addProperty("has_skylight", sky);
        type.addProperty("has_ceiling", false);
        type.addProperty("ambient_light", ambient);
        type.addProperty("monster_spawn_light_level", 7);
        type.addProperty("monster_spawn_block_light_limit", 0);
        type.addProperty("piglin_safe", false);
        type.addProperty("bed_works", true);
        type.addProperty("respawn_anchor_works", false);
        type.addProperty("has_raids", false);
        type.addProperty("logical_height", 256);
        type.addProperty("min_y", 0);
        type.addProperty("height", 256);
        type.addProperty("infiniburn", "#minecraft:infiniburn_overworld");
        type.addProperty("effects", EFFECTS.toString()); // client: LegacyDimensionEffects asks the provider
        return type;
    }

    /** Dimension types and dimensions of every mod dimension registered by the end of legacy loading. */
    public static void contribute(Map<String, Supplier<byte[]>> out) {
        RuntimeDimensions.contribute(out);
        for (var e : DimensionManager.mcc$modDimensions().entrySet()) {
            int id = e.getKey();
            boolean sky = true;
            float ambient = 0.0F;
            try {
                var ctor = e.getValue().getDeclaredConstructor();
                ctor.setAccessible(true);
                WorldProvider p = (WorldProvider) ctor.newInstance();
                sky = !p.hasNoSky;
                ambient = p.mcc$ambientLight();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                // keep the overworld-like defaults
            }
            JsonObject type = dimensionType(sky, ambient);
            byte[] typeBytes = type.toString().getBytes(StandardCharsets.UTF_8);
            String name = PREFIX + id;
            out.put("data/" + NAMESPACE + "/dimension_type/" + name + ".json", () -> typeBytes);

            JsonObject biomeSource = new JsonObject();
            biomeSource.addProperty("type", NAMESPACE + ":legacy");
            biomeSource.addProperty("dimension", id);
            JsonArray biomes = new JsonArray();
            LegacyBiomeSource.allBiomeIds().forEach(biomes::add);
            biomeSource.add("biomes", biomes);
            JsonObject generator = new JsonObject();
            generator.addProperty("type", NAMESPACE + ":legacy");
            generator.addProperty("dimension", id);
            generator.add("biome_source", biomeSource);
            JsonObject stem = new JsonObject();
            stem.addProperty("type", NAMESPACE + ":" + name);
            stem.add("generator", generator);
            byte[] stemBytes = stem.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/" + NAMESPACE + "/dimension/" + name + ".json", () -> stemBytes);
            LOG.info("Mod dimension {} ({}) is available as {}:{}", id, e.getValue().getName(), NAMESPACE, name);
        }
    }
}
