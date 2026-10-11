package org.kylepitcock.mccompat.v1_7_10.dimension;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import mccompat.v1_7_10.net.minecraft.world.WorldProvider;
import mccompat.v1_7_10.net.minecraftforge.common.DimensionManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod dimensions. A dimension a legacy mod registers with {@code DimensionManager.registerDimension(id, type)}
 * becomes a modern dimension {@code retrofit:dim_<id>} whose chunk generator ({@link LegacyChunkGenerator}) runs
 * the mod's {@code WorldProvider.createChunkGenerator()}, whose biomes come from the provider's chunk manager, and
 * whose world provider object is an instance of the mod's provider class.
 */
public final class LegacyDimensions {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Dimensions");
    private static final String NAMESPACE = "retrofit";
    private static final String PREFIX = "dim_";

    private LegacyDimensions() {}

    public static ResourceLocation id(int dimension) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, PREFIX + dimension);
    }

    public static ResourceKey<Level> levelKey(int dimension) {
        return ResourceKey.create(Registries.DIMENSION, id(dimension));
    }

    /** The legacy dimension id of a {@code retrofit:dim_<id>} location, or null. */
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
        return mccompat.v1_7_10.net.minecraft.world.World.mcc$get$provider(mccompat.v1_7_10.net.minecraft.world.World.mcc$wrap(level));
    }

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

    /** Dimension types and dimensions of every mod dimension registered by the end of legacy loading. */
    public static void contribute(Map<String, Supplier<byte[]>> out) {
        for (var e : DimensionManager.mcc$modDimensions().entrySet()) {
            int id = e.getKey();
            boolean sky = true;
            try {
                var ctor = e.getValue().getDeclaredConstructor();
                ctor.setAccessible(true);
                WorldProvider p = (WorldProvider) ctor.newInstance();
                sky = !p.hasNoSky;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                // keep the overworld-like defaults
            }
            JsonObject type = new JsonObject();
            type.addProperty("ultrawarm", false);
            type.addProperty("natural", true);
            type.addProperty("coordinate_scale", 1.0);
            type.addProperty("has_skylight", sky);
            type.addProperty("has_ceiling", false);
            type.addProperty("ambient_light", 0.0);
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
            type.addProperty("effects", "minecraft:overworld");
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
