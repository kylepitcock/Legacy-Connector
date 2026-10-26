package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyEntityBlockAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy tile entity id mappings and the single modern block entity type ({@code legacyconnector:legacy_tile_1_7_10}) that
 * hosts every legacy tile entity. One type suffices because the legacy class is recorded in the saved NBT.
 */
public final class TileEntityBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/TileEntities");
    private static final Map<String, Class<? extends TileEntity>> BY_ID = new HashMap<>();
    private static final Map<Class<?>, String> IDS = new HashMap<>();
    private static BlockEntityType<LegacyBlockEntity> type;

    private TileEntityBridge() {}

    public static synchronized void addMapping(Class<?> cls, String id) {
        if (BY_ID.containsKey(id)) {
            LOG.warn("Duplicate legacy tile entity id {} ({} vs {})", id, BY_ID.get(id).getName(), cls.getName());
            return;
        }
        @SuppressWarnings("unchecked")
        Class<? extends TileEntity> te = (Class<? extends TileEntity>) cls;
        BY_ID.put(id, te);
        IDS.put(cls, id);
    }

    public static synchronized void addAlias(String alias, String id) {
        Class<? extends TileEntity> cls = BY_ID.get(id);
        if (cls != null) BY_ID.putIfAbsent(alias, cls);
    }

    public static synchronized String idOf(Class<?> cls) {
        return IDS.get(cls);
    }

    public static TileEntity createAndLoad(NBTTagCompound tag) {
        Class<? extends TileEntity> cls;
        synchronized (TileEntityBridge.class) {
            cls = BY_ID.get(tag.getString("id"));
        }
        if (cls == null) {
            LOG.warn("Skipping legacy tile entity with unknown id {}", tag.getString("id"));
            return null;
        }
        try {
            var ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            TileEntity te = ctor.newInstance();
            te.readFromNBT(tag);
            return te;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.error("Failed to load legacy tile entity {}", cls.getName(), e);
            return null;
        }
    }

    public static BlockEntityType<LegacyBlockEntity> type() {
        return type;
    }

    /** Registers the host block entity type once all legacy blocks exist (end of legacy loading). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerType(List<LegacyBlockAdapter> adapters) {
        if (type != null) return;
        var blocks = adapters.stream().filter(a -> a instanceof LegacyEntityBlockAdapter).toArray(net.minecraft.world.level.block.Block[]::new);
        BlockEntityType<LegacyBlockEntity> built = BlockEntityType.Builder.of(LegacyBlockEntity::new, blocks).build(null);
        LegacyRegistrar.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_tile_1_7_10"),
                (BlockEntityType<?>) built);
        type = built;
    }

    /** Views of vanilla/modern block entities, kept per block entity so identity is stable for legacy code. */
    private static final Map<net.minecraft.world.level.block.entity.BlockEntity, TileEntity> VIEWS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    static {
        org.kylepitcock.legacyconnector.core.ViewSweeper.register("1.7.10 block entity", VIEWS, net.minecraft.world.level.block.entity.BlockEntity::isRemoved);
    }

    /**
     * The block entity at {@code pos}. Legacy generators probe for tile entities everywhere; a generation region warns
     * for every block that will have one but has none yet (hundreds of thousands of lines), so it asks the chunk and
     * only falls back to the region when the chunk holds pending data for the position.
     */
    static net.minecraft.world.level.block.entity.BlockEntity blockEntityAt(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.WorldGenRegion region) {
            var chunk = region.getChunk(pos);
            var be = chunk.getBlockEntity(pos);
            if (be != null) return be;
            var pending = chunk.getBlockEntityNbt(pos);
            if (pending == null) return null;
            // the placeholder queued when an entity block was placed: build it here, as the region does, but quietly when
            // the legacy block has no tile entity for this metadata (the region warns: Thaumcraft's hilltop stones, IE ores)
            if ("DUMMY".equals(pending.getString("id"))) {
                var state = chunk.getBlockState(pos);
                if (!(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb)) return null;
                be = eb.newBlockEntity(pos, state);
                if (be != null) chunk.setBlockEntity(be);
                return be;
            }
            return region.getBlockEntity(pos);
        }
        return level.getBlockEntity(pos);
    }

    public static TileEntity legacyAt(World world, BlockPos pos) {
        var be = blockEntityAt(world.mcc$level(), pos);
        if (be == null) return stateView(world, pos);
        if (be instanceof LegacyBlockEntity l) {
            TileEntity te = l.legacy(world);
            // A tile entity placed while a chunk is generated has no level yet: it lives in the world it was asked through.
            if (te != null && te.worldObj == null) {
                te.worldObj = world;
                te.xCoord = pos.getX();
                te.yCoord = pos.getY();
                te.zCoord = pos.getZ();
            }
            return te;
        }
        TileEntity view = VIEWS.computeIfAbsent(be, TileEntityBridge::view);
        if (view != null) {
            view.worldObj = world;
            view.xCoord = pos.getX();
            view.yCoord = pos.getY();
            view.zCoord = pos.getZ();
            if (view instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityChest chest) {
                chest.mcc$refresh();
                chest.numPlayersUsing = net.minecraft.world.level.block.entity.ChestBlockEntity.getOpenCount(world.mcc$level(), pos);
            } else if (view instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntitySign sign) {
                sign.mcc$refresh();
            }
        }
        return view;
    }

    /**
     * 1.7.10 note blocks and flower pots had tile entities; their modern blocks keep that state in the block itself. Legacy code
     * asking a live world for one gets a view over the block (a new one each time: it reads the block when made).
     */
    private static TileEntity stateView(World world, BlockPos pos) {
        var level = world.mcc$liveLevel();
        if (level == null) return null;
        var block = level.getBlockState(pos).getBlock();
        TileEntity view;
        if (block instanceof net.minecraft.world.level.block.NoteBlock) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityNote(level, pos);
        } else if (block instanceof net.minecraft.world.level.block.FlowerPotBlock) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFlowerPot(level, pos);
        } else {
            return null;
        }
        view.worldObj = world;
        view.xCoord = pos.getX();
        view.yCoord = pos.getY();
        view.zCoord = pos.getZ();
        return view;
    }

    private static TileEntity view(net.minecraft.world.level.block.entity.BlockEntity be) {
        TileEntity view = null;
        if (be instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityMobSpawner(spawner);
        } else if (be instanceof net.minecraft.world.level.block.piston.PistonMovingBlockEntity moving) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityPiston(moving);
        } else if (be instanceof net.minecraft.world.level.block.entity.HopperBlockEntity hopper) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityHopper(hopper);
        } else if (be instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity dispenser) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityDispenser(dispenser);
        } else if (be instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity beacon) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityBeacon(beacon);
        } else if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityChest(chest);
        } else if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntitySign(sign);
        } else if (be instanceof net.minecraft.world.level.block.entity.CommandBlockEntity command) {
            view = new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityCommandBlock(command);
        } else if (be instanceof net.minecraft.world.Container c) {
            view = new VanillaInventoryTile(c);
        }
        if (view != null) view.mcc$host = be;
        return view;
    }

    /** Called before legacy code changes the block at {@code pos}: writes back reflective edits to chest views. */
    public static void beforeBlockChange(World world, BlockPos pos) {
        var be = blockEntityAt(world.mcc$level(), pos);
        if (be == null) return;
        TileEntity view = VIEWS.get(be);
        if (view instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityChest chest) chest.mcc$flush();
        else if (view instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntitySign sign) sign.mcc$flush();
    }

    /** Legacy setBlock with the same block and another metadata: the tile entity refreshes as in 1.7.10 (setBlockMetadata does not). */
    public static void beforeSetBlock(World world, BlockPos pos, net.minecraft.world.level.block.state.BlockState next) {
        if (!(next.getBlock() instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter)) return;
        var current = world.mcc$level().getBlockState(pos);
        if (current.getBlock() != next.getBlock()) return;
        var be = blockEntityAt(world.mcc$level(), pos);
        if (be instanceof LegacyBlockEntity l) {
            l.refreshForMeta(current.getValue(org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter.META),
                    next.getValue(org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter.META));
        }
    }

    public static void setAt(World world, BlockPos pos, TileEntity te) {
        var be = blockEntityAt(world.mcc$level(), pos);
        if (be instanceof LegacyBlockEntity l) {
            l.replaceLegacy(te);
            if (te != null && te.worldObj == null) te.worldObj = world;
        } else if (te instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityPiston piston && world.mcc$level() != null
                && world.mcc$level().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.piston.MovingPistonBlock) {
            // a mod's piston moving a block: the legacy moving piston becomes the modern one
            var level = world.mcc$level();
            var modern = piston.mcc$toModern(pos, level.getBlockState(pos));
            if (modern != null && modern != be && level instanceof net.minecraft.world.level.Level live) live.setBlockEntity(modern);
        } else if (te != null) {
            LOG.debug("Legacy setTileEntity at {} ignored: block there does not host legacy tile entities", pos);
        }
    }
}
