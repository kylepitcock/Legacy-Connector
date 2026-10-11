package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.kylepitcock.mccompat.core.registry.LegacyRegistrar;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockEntity;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyEntityBlockAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy tile entity id mappings and the single modern block entity type ({@code retrofit:legacy_tile_1_7_10}) that
 * hosts every legacy tile entity. One type suffices because the legacy class is recorded in the saved NBT.
 */
public final class TileEntityBridge {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/TileEntities");
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
        LegacyRegistrar.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("retrofit", "legacy_tile_1_7_10"),
                (BlockEntityType<?>) built);
        type = built;
    }

    /** Views of vanilla/modern block entities, kept per block entity so identity is stable for legacy code. */
    private static final Map<net.minecraft.world.level.block.entity.BlockEntity, TileEntity> VIEWS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static TileEntity legacyAt(World world, BlockPos pos) {
        var be = world.mcc$level().getBlockEntity(pos);
        if (be == null) return null;
        if (be instanceof LegacyBlockEntity l) return l.legacy();
        TileEntity view = VIEWS.computeIfAbsent(be, TileEntityBridge::view);
        if (view != null) {
            view.worldObj = world;
            view.xCoord = pos.getX();
            view.yCoord = pos.getY();
            view.zCoord = pos.getZ();
            if (view instanceof mccompat.v1_7_10.net.minecraft.tileentity.TileEntityChest chest) {
                chest.mcc$refresh();
                chest.numPlayersUsing = net.minecraft.world.level.block.entity.ChestBlockEntity.getOpenCount(world.mcc$level(), pos);
            }
        }
        return view;
    }

    private static TileEntity view(net.minecraft.world.level.block.entity.BlockEntity be) {
        TileEntity view = null;
        if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            view = new mccompat.v1_7_10.net.minecraft.tileentity.TileEntityChest(chest);
        } else if (be instanceof net.minecraft.world.Container c) {
            view = new VanillaInventoryTile(c);
        }
        if (view != null) view.mcc$host = be;
        return view;
    }

    /** Called before legacy code changes the block at {@code pos}: writes back reflective edits to chest views. */
    public static void beforeBlockChange(World world, BlockPos pos) {
        var be = world.mcc$level().getBlockEntity(pos);
        if (be == null) return;
        TileEntity view = VIEWS.get(be);
        if (view instanceof mccompat.v1_7_10.net.minecraft.tileentity.TileEntityChest chest) chest.mcc$flush();
    }

    public static void setAt(World world, BlockPos pos, TileEntity te) {
        var be = world.mcc$level().getBlockEntity(pos);
        if (be instanceof LegacyBlockEntity l) {
            l.replaceLegacy(te);
        } else if (te != null) {
            LOG.debug("Legacy setTileEntity at {} ignored: block there does not host legacy tile entities", pos);
        }
    }
}
