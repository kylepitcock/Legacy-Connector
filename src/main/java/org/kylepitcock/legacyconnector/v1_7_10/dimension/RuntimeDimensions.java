package org.kylepitcock.legacyconnector.v1_7_10.dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Levels for mod dimensions registered while the server runs. The dimensions a mod registers during loading are part of
 * the generated data pack ({@link LegacyDimensions#contribute}); Mystcraft ages, Galacticraft space stations and
 * Mystcraft's instability profiling dimension are registered later, and 1.7.10 created their worlds on first use
 * ({@code worldServerForDimension}, {@code initDimension}). They get the same {@code legacyconnector:dim_<id>} level
 * here, built like the server builds its own levels, with a generic dimension type (with or without a sky) because
 * dimension types cannot be registered once the server runs.
 */
public final class RuntimeDimensions {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Dimensions");
    public static final ResourceLocation TYPE_SKY = ResourceLocation.fromNamespaceAndPath("legacyconnector", "runtime_sky");
    public static final ResourceLocation TYPE_NO_SKY = ResourceLocation.fromNamespaceAndPath("legacyconnector", "runtime_no_sky");
    private static final Set<Integer> FAILED = ConcurrentHashMap.newKeySet();

    private static final ChunkProgressListener NO_PROGRESS = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(ChunkPos center) {
        }

        @Override
        public void onStatusChange(ChunkPos pos, ChunkStatus status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    private RuntimeDimensions() {}

    /**
     * The level of mod dimension {@code id}, created when the dimension is registered but has none yet. Null for ids no
     * mod registered, off the server thread (levels are only created where the server adds its own) or when creating fails.
     */
    public static ServerLevel getOrCreate(MinecraftServer server, int id) {
        ResourceKey<net.minecraft.world.level.Level> key = LegacyDimensions.levelKey(id);
        ServerLevel existing = server.getLevel(key);
        if (existing != null) return existing;
        if (!server.isSameThread() || FAILED.contains(id)) return null;
        Class<?> providerClass = DimensionManager.mcc$modDimensions().get(id);
        if (providerClass == null) return null;
        try {
            ServerLevel level = create(server, id, key, providerClass);
            LOG.info("Created the level of mod dimension {} ({}) while the server runs", id, providerClass.getName());
            return level;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FAILED.add(id);
            LOG.error("Could not create the level of mod dimension {} ({})", id, providerClass.getName(), e);
            return null;
        }
    }

    private static ServerLevel create(MinecraftServer server, int id, ResourceKey<net.minecraft.world.level.Level> key, Class<?> providerClass)
            throws ReflectiveOperationException {
        boolean sky = true;
        try {
            var ctor = providerClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            sky = !((WorldProvider) ctor.newInstance()).hasNoSky;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // a provider that needs its dimension's data to be built (Mystcraft's): an overworld-like sky
        }
        var registries = server.registryAccess();
        Holder<DimensionType> type = registries.registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, sky ? TYPE_SKY : TYPE_NO_SKY));
        var biomeRegistry = registries.registryOrThrow(Registries.BIOME);
        List<Holder<Biome>> biomes = new ArrayList<>();
        for (String biome : LegacyBiomeSource.allBiomeIds()) {
            biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biome))).ifPresent(biomes::add);
        }
        var generator = new LegacyChunkGenerator(id, new LegacyBiomeSource(id, HolderSet.direct(biomes)));
        WorldData worldData = server.getWorldData();
        ServerLevel overworld = server.overworld();
        ServerLevel level = new ServerLevel(server, (Executor) field(server, "executor"),
                (LevelStorageSource.LevelStorageAccess) field(server, "storageSource"), new DerivedLevelData(worldData, worldData.overworldData()),
                key, new LevelStem(type, generator), NO_PROGRESS, worldData.isDebugWorld(),
                BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()), List.of(), false, overworld.getRandomSequences());
        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        server.forgeGetWorldMap().put(key, level);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }

    private static Object field(MinecraftServer server, String name) throws ReflectiveOperationException {
        var f = MinecraftServer.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(server);
    }

    /** The generic dimension types (data pack entries) runtime levels use. */
    public static void contribute(java.util.Map<String, java.util.function.Supplier<byte[]>> out) {
        for (boolean sky : new boolean[] {true, false}) {
            com.google.gson.JsonObject t = LegacyDimensions.dimensionType(sky, 0.0F);
            byte[] bytes = t.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.put("data/legacyconnector/dimension_type/" + (sky ? TYPE_SKY : TYPE_NO_SKY).getPath() + ".json", () -> bytes);
        }
    }
}
