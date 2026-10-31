package legacyconnector.v1_7_10.net.minecraft.world;

import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

/** Server-side world view. {@link World#mcc$wrap} returns instances of this class for server levels. */
public class WorldServer extends World {
    protected WorldServer(LevelAccessor level) {
        super(level);
    }

    /**
     * 1.7.10's {@code new WorldServer(server, saveHandler, name, dimension, settings, profiler)}. A legacy mod that extends
     * WorldServer (CoFH's WorldServerProxy, behind MineFactory Reloaded's grinder and slaughterhouse: it wraps a world to
     * intercept the entities spawned into it) cannot build a second level, so this world is another view of the live
     * level of the dimension: every query and change goes to the same blocks.
     */
    public WorldServer(MinecraftServer server, legacyconnector.v1_7_10.net.minecraft.world.storage.ISaveHandler saveHandler, String name,
            int dimension, WorldSettings settings, legacyconnector.v1_7_10.net.minecraft.profiler.Profiler profiler) {
        this(mcc$levelOf(dimension));
    }

    private static LevelAccessor mcc$levelOf(int dimension) {
        WorldServer live = legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.getWorld(dimension);
        return live == null ? null : live.mcc$fullLevel();
    }

    public legacyconnector.v1_7_10.net.minecraft.server.management.PlayerManager getPlayerManager() {
        return mcc$fullLevel() instanceof ServerLevel s ? new legacyconnector.v1_7_10.net.minecraft.server.management.PlayerManager(s) : null;
    }

    public MinecraftServer func_73046_m() {
        return mcc$fullLevel() instanceof ServerLevel s ? MinecraftServer.mcc$wrap(s.getServer()) : null;
    }

    /**
     * {@code world.theChunkProviderServer}: the world's chunk provider (nothing declared it, so it read as null: Mystcraft's
     * instability profiling generates through it).
     */
    public static legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderServer mcc$get$theChunkProviderServer(WorldServer w) {
        return (legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderServer) w.getChunkProvider();
    }

    public static void mcc$set$theChunkProviderServer(WorldServer w, legacyconnector.v1_7_10.net.minecraft.world.gen.ChunkProviderServer provider) {
        w.mcc$setChunkProvider(provider);
    }

    private legacyconnector.v1_7_10.net.minecraft.entity.EntityTracker mcc$tracker;

    public legacyconnector.v1_7_10.net.minecraft.entity.EntityTracker getEntityTracker() {
        if (mcc$tracker == null) mcc$tracker = new legacyconnector.v1_7_10.net.minecraft.entity.EntityTracker(this);
        return mcc$tracker;
    }

    public java.io.File getChunkSaveLocation() {
        return getSaveHandler().getWorldDirectory();
    }

    public boolean areAllPlayersAsleep() {
        return mcc$fullLevel() instanceof ServerLevel s && s.players().stream().allMatch(net.minecraft.world.entity.player.Player::isSleeping);
    }

    public void resetUpdateEntityTick() {
    }

    /** Forge: the teleporters of mods registered for this world (kept so their portal caches are cleaned with the world's). */
    public java.util.List<Teleporter> customTeleporters = new java.util.ArrayList<>();

    /** Where entities arrive from an end portal in this world (the provider's: the End's platform). */
    public legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates getEntrancePortalLocation() {
        return mcc$get$provider(this).getEntrancePortalLocation();
    }

    /**
     * A random spawn entry for the creature type at the position: the legacy biome's own list when it has one (mod biomes), else the
     * modern biome's spawns of that category, as the legacy class of each vanilla mob. Null when nothing can spawn there.
     */
    public legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry spawnRandomCreature(
            legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type, int x, int y, int z) {
        if (type == null) return null;
        var biome = getBiomeGenForCoords(x, z);
        java.util.List<legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry> list =
                biome == null ? java.util.List.of() : biome.getSpawnableList(type);
        if (list != null && !list.isEmpty()) {
            int total = 0;
            for (var e : list) total += Math.max(0, e.itemWeight);
            if (total > 0) {
                int pick = rand.nextInt(total);
                for (var e : list) {
                    pick -= Math.max(0, e.itemWeight);
                    if (pick < 0) return e;
                }
            }
        }
        if (!(mcc$fullLevel() instanceof ServerLevel level)) return null;
        var p = new net.minecraft.core.BlockPos(x, y, z);
        if (!level.hasChunkAt(p)) return null;
        var mobs = level.getBiome(p).value().getMobSettings().getMobs(type.mcc$modern());
        var picked = mobs.getRandom(level.random);
        if (picked.isEmpty()) return null;
        var data = picked.get();
        Class<?> legacy = mcc$legacyMobClass(data.type);
        if (legacy == null) return null;
        return new legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.SpawnListEntry(legacy, data.getWeight().asInt(), data.minCount, data.maxCount);
    }

    /** Modern vanilla mob ids whose 1.7.10 entity names differ from the id. */
    private static final java.util.Map<String, String> MCC_LEGACY_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("mooshroom", "MushroomCow"), java.util.Map.entry("ocelot", "Ozelot"), java.util.Map.entry("horse", "EntityHorse"),
            java.util.Map.entry("cave_spider", "CaveSpider"), java.util.Map.entry("zombified_piglin", "PigZombie"),
            java.util.Map.entry("magma_cube", "LavaSlime"), java.util.Map.entry("iron_golem", "VillagerGolem"), java.util.Map.entry("snow_golem", "SnowMan"));

    private static Class<?> mcc$legacyMobClass(net.minecraft.world.entity.EntityType<?> type) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (key == null || !"minecraft".equals(key.getNamespace())) return null;
        String path = key.getPath();
        String name = MCC_LEGACY_NAMES.get(path);
        if (name == null) name = Character.toUpperCase(path.charAt(0)) + path.substring(1);
        return legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.get(name);
    }

    public void func_147487_a(String particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double speed) {
    }
}
