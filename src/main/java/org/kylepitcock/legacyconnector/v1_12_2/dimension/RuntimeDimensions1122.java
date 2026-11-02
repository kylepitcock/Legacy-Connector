package org.kylepitcock.legacyconnector.v1_12_2.dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager;
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
 * Levels for 1.12.2 mod dimensions registered once the server runs. Galacticraft registers its planets in
 * {@code FMLServerStartingEvent} (the ids come from its config) and Forge made their worlds on first use
 * ({@code DimensionManager.getWorld}, a teleport); the data pack of the dimensions registered while loading cannot hold them.
 * They get the same {@code legacyconnector:dim_<id>} level, built like the server builds its own, with the generic dimension type
 * with or without a sky ({@code legacyconnector:runtime_sky}), because dimension types cannot be added to a running server.
 */
public final class RuntimeDimensions1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Dimensions");
    private static final ResourceLocation TYPE_SKY = ResourceLocation.fromNamespaceAndPath("legacyconnector", "runtime_sky");
    private static final ResourceLocation TYPE_NO_SKY = ResourceLocation.fromNamespaceAndPath("legacyconnector", "runtime_no_sky");
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

    private RuntimeDimensions1122() {}

    /** The level of mod dimension {@code id}: created when the dimension is registered but has none. Null for other ids, off the server thread or on failure. */
    public static ServerLevel getOrCreate(MinecraftServer server, int id) {
        ResourceKey<net.minecraft.world.level.Level> key = Dimensions1122.levelKey(id);
        ServerLevel existing = server.getLevel(key);
        if (existing != null) return existing;
        if (id >= -1 && id <= 1 || !server.isSameThread() || FAILED.contains(id)) return null;
        if (!DimensionManager.isDimensionRegistered(id)) return null;
        var type = DimensionManager.getProviderType(id);
        if (type == null || type.mcc$providerClass() == null) return null;
        try {
            ServerLevel level = create(server, id, key, type.mcc$providerClass());
            LOG.info("Created the level of 1.12.2 mod dimension {} ({}) while the server runs", id, type.mcc$providerClass().getName());
            return level;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            FAILED.add(id);
            LOG.error("Could not create the level of 1.12.2 mod dimension {} ({})", id, type.mcc$providerClass().getName(), e);
            return null;
        }
    }

    private static ServerLevel create(MinecraftServer server, int id, ResourceKey<net.minecraft.world.level.Level> key, Class<?> providerClass)
            throws ReflectiveOperationException {
        boolean sky = true;
        try {
            var ctor = providerClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            sky = ((legacyconnector.v1_12_2.net.minecraft.world.WorldProvider) ctor.newInstance()).hasSkyLight();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // a provider that needs its world: an overworld-like sky
        }
        var registries = server.registryAccess();
        Holder<DimensionType> type = registries.registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, sky ? TYPE_SKY : TYPE_NO_SKY));
        var biomeRegistry = registries.registryOrThrow(Registries.BIOME);
        List<Holder<Biome>> biomes = new ArrayList<>();
        for (String biome : Dimensions1122.providerBiomeIds()) {
            biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biome))).ifPresent(biomes::add);
        }
        var generator = new LegacyChunkGenerator1122(id, new LegacyBiomeSource1122(id, HolderSet.direct(biomes)));
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
}
