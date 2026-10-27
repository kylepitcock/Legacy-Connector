package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.WorldServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/** Legacy views of modern levels (server levels are {@code WorldServer}s) and 1.12's numeric dimension ids. */
public final class WorldBridge1122 {
    private static final Map<Level, World> WORLDS = new WeakHashMap<>();
    private static final Map<net.minecraft.resources.ResourceKey<Level>, Integer> IDS = new java.util.concurrent.ConcurrentHashMap<>();

    private WorldBridge1122() {}

    public static synchronized World wrap(Level level) {
        if (level == null) return null;
        return WORLDS.computeIfAbsent(level, l -> l instanceof net.minecraft.server.level.ServerLevel ? new WorldServer(l)
                // the client's level is a WorldClient (Minecraft.world)
                : l.isClientSide() ? new legacyconnector.v1_12_2.net.minecraft.client.multiplayer.WorldClient(l) : new WorldView(l));
    }

    /**
     * The view of a generation region, per thread. Regions live for one generation step; kept in {@link #WORLDS} they would never
     * be released (the view holds its region and all its proto chunks).
     */
    private static final ThreadLocal<World> REGION_VIEW = new ThreadLocal<>();

    /** A level, or a generation region (its own view: blocks read and written through the region). */
    public static World wrap(LevelAccessor access) {
        if (access == null) return null;
        if (access instanceof Level level) return wrap(level);
        World cached = REGION_VIEW.get();
        if (cached != null && cached.mcc$access() == access) return cached;
        World view = new WorldServer(access);
        REGION_VIEW.set(view);
        return view;
    }

    /** Drops this thread's region view (called when a generation step is done). */
    public static void mcc$releaseRegion() {
        REGION_VIEW.remove();
    }

    /** A block view of a modern block getter for legacy code: the world when it is a level or region, else a read-only view. */
    public static legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess access(net.minecraft.world.level.BlockGetter getter) {
        if (getter instanceof LevelAccessor a) return wrap(a);
        return new BlockGetterView(getter);
    }

    /**
     * The modern entity that hosts a legacy entity with none yet. The entity bridge (area E) creates hosts for mod entities; until
     * one exists for an entity class, it is not spawned.
     */
    public static net.minecraft.world.entity.Entity hostEntity(World world, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return entity.mcc$modern();
    }

    /** The modern structure tag of a 1.12 structure name ({@code World.findNearestStructure}), or null. */
    public static net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> structureTag(String name) {
        if (name == null) return null;
        return switch (name) {
            case "Stronghold" -> net.minecraft.tags.StructureTags.EYE_OF_ENDER_LOCATED;
            case "Village" -> net.minecraft.tags.StructureTags.VILLAGE;
            case "Mansion" -> net.minecraft.tags.StructureTags.ON_WOODLAND_EXPLORER_MAPS;
            case "Monument" -> net.minecraft.tags.StructureTags.ON_OCEAN_EXPLORER_MAPS;
            case "Mineshaft" -> net.minecraft.tags.StructureTags.MINESHAFT;
            case "Temple" -> net.minecraft.tags.StructureTags.ON_TREASURE_MAPS;
            default -> null;
        };
    }

    /**
     * The 1.12 dimension id of a level: 0, -1 and 1 for the vanilla three, the id a mod registered for a dimension of that name,
     * else a stable id of the level's name above 1000 (mods compare against 0 for the overworld, so a new dimension must not be 0).
     */
    public static int dimensionId(Level level) {
        var key = level.dimension();
        Integer known = IDS.get(key);
        if (known != null) return known;
        int id;
        if (key == Level.OVERWORLD) id = 0;
        else if (key == Level.NETHER) id = -1;
        else if (key == Level.END) id = 1;
        else {
            id = 1000 + Math.floorMod(key.location().toString().hashCode(), 1_000_000);
            for (var type : legacyconnector.v1_12_2.net.minecraft.world.DimensionType.values()) {
                if (type.getName().equals(key.location().getPath())) id = type.getId();
            }
        }
        IDS.put(key, id);
        return id;
    }

    /** The server level of a 1.12 dimension id, or null. */
    public static net.minecraft.server.level.ServerLevel levelOf(net.minecraft.server.MinecraftServer server, int id) {
        if (server == null) return null;
        if (id == 0) return server.overworld();
        if (id == -1) return server.getLevel(Level.NETHER);
        if (id == 1) return server.getLevel(Level.END);
        for (var level : server.getAllLevels()) if (dimensionId(level) == id) return level;
        return null;
    }

    private static final class WorldView extends World {
        WorldView(Level level) {
            super(level);
        }
    }

    /** Read access to a modern block getter that is not a level (a chunk section cache, a render region). */
    private static final class BlockGetterView implements legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess {
        private final net.minecraft.world.level.BlockGetter getter;

        BlockGetterView(net.minecraft.world.level.BlockGetter getter) {
            this.getter = getter;
        }

        private static net.minecraft.core.BlockPos modern(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos p) {
            return new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity getTileEntity(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
            var be = getter.getBlockEntity(modern(pos));
            return be instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity l ? l.legacy() : null;
        }

        @Override
        public int getCombinedLight(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, int lightValue) {
            if (getter instanceof net.minecraft.world.level.BlockAndTintGetter t) {
                var p = modern(pos);
                int sky = t.getBrightness(net.minecraft.world.level.LightLayer.SKY, p);
                int block = Math.max(lightValue, t.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p));
                return sky << 20 | block << 4;
            }
            return 15 << 20 | lightValue << 4;
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getBlockState(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
            var s = StatesBridge1122.toLegacy(getter.getBlockState(modern(pos)));
            return s != null ? s : Vanilla1122.airState();
        }

        @Override
        public boolean isAirBlock(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
            return getter.getBlockState(modern(pos)).isAir();
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.world.biome.Biome getBiome(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
            return legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS;
        }

        @Override
        public int getStrongPower(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing direction) {
            var p = modern(pos);
            return getter.getBlockState(p).getDirectSignal(getter, p, direction.mcc$modern());
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.world.WorldType getWorldType() {
            return legacyconnector.v1_12_2.net.minecraft.world.WorldType.DEFAULT;
        }

        @Override
        public boolean isSideSolid(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side,
                boolean _default) {
            var p = modern(pos);
            return getter.getBlockState(p).isFaceSturdy(getter, p, side.mcc$modern());
        }
    }
}
