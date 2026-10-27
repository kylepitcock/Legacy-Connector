package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.WorldServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/** Legacy worlds of modern levels: one per level, plus a per-thread view of the generation region a legacy generator runs in. */
public final class WorldBridge1122 {
    private static final Map<Level, World> WORLDS = new WeakHashMap<>();

    private WorldBridge1122() {}

    public static synchronized World wrap(Level level) {
        if (level == null) return null;
        return WORLDS.computeIfAbsent(level, l -> l instanceof net.minecraft.server.level.ServerLevel ? new WorldServer(l) : new WorldView(l));
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
