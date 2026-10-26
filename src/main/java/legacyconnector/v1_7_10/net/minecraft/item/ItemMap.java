package legacyconnector.v1_7_10.net.minecraft.item;

import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import legacyconnector.v1_7_10.net.minecraft.world.storage.MapData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * The filled map. The vanilla map's data is the modern map ({@code map_<damage>}, the damage being the map id): mods get a
 * {@link MapData} view whose colour array is the modern map's own. A mod's map item that keeps its own {@link MapData} is drawn
 * by the modern map renderer's terrain scan ({@link #updateMapData}).
 */
public class ItemMap extends legacyconnector.v1_7_10.net.minecraft.item.ItemMapBase {
    public ItemMap(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemMap() {
        super();
        setHasSubtypes(true);
    }

    /** A legacy view of a modern map: same centre, scale and dimension, sharing its colour array. */
    private static final class ModernView extends MapData {
        final MapItemSavedData modern;

        ModernView(String name, MapItemSavedData modern) {
            super(name);
            this.modern = modern;
            this.xCenter = modern.centerX;
            this.zCenter = modern.centerZ;
            this.scale = modern.scale;
            this.dimension = WorldProvider.dimensionId(modern.dimension);
            this.colors = modern.colors;
        }

        @Override
        public void markDirty() {
            super.markDirty();
            modern.setDirty();
        }

        @Override
        public void setColumnDirty(int x, int minZ, int maxZ) {
            super.markDirty();
            for (int z = minZ; z <= maxZ && x >= 0 && x < 128; z++) {
                if (z >= 0 && z < 128) modern.setColor(x, z, colors[x + z * 128]);
            }
        }
    }

    private static final Map<MapItemSavedData, ModernView> VIEWS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    /** Modern maps standing in for mods' legacy map data while it is drawn (the scan keeps per-player progress in them). */
    private static final Map<MapData, MapItemSavedData> SCRATCH = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static MapData mcc$view(int id, MapItemSavedData modern) {
        if (modern == null) return null;
        synchronized (VIEWS) {
            ModernView view = VIEWS.get(modern);
            if (view == null || view.colors != modern.colors) {
                view = new ModernView("map_" + id, modern);
                VIEWS.put(modern, view);
            }
            return view;
        }
    }

    /** {@code func_150912_a}: the data of map {@code id} ("map_" + id), or null. */
    public static MapData loadMapData(int id, World world) {
        Level level = world == null ? null : world.mcc$fullLevel();
        if (level != null) {
            MapData view = mcc$view(id, level.getMapData(new MapId(id)));
            if (view != null) return view;
        }
        return world == null ? null : (MapData) world.loadItemData(MapData.class, "map_" + id);
    }

    /**
     * The data of the map on this stack. A vanilla map without data gets a new map on the server (scale 3, centred near the
     * spawn point, as 1.7.10 did); a mod's map keeps 1.7.10's own {@code map_<damage>} data.
     */
    public MapData getMapData(ItemStack stack, World world) {
        if (stack == null || world == null) return null;
        Level level = world.mcc$fullLevel();
        if (mcc$vanilla != null && level != null) {
            var modernStack = stack.mcc$modern();
            MapId id = modernStack.get(net.minecraft.core.component.DataComponents.MAP_ID);
            MapItemSavedData data = id == null ? null : level.getMapData(id);
            if (data == null && !world.isRemote && level instanceof net.minecraft.server.level.ServerLevel server) {
                byte scale = 3;
                int size = 128 * (1 << scale);
                var spawn = server.getSharedSpawnPos();
                int x = Math.round((float) spawn.getX() / size) * size;
                int z = Math.round((float) spawn.getZ() / size) * size;
                data = mcc$newModern(x, z, scale, server.dimension());
                id = server.getFreeMapId();
                server.setMapData(id, data);
                modernStack.set(net.minecraft.core.component.DataComponents.MAP_ID, id);
            }
            return id == null ? null : mcc$view(id.id(), data);
        }
        String name = "map_" + stack.getItemDamage();
        MapData data = (MapData) world.loadItemData(MapData.class, name);
        if (data == null && !world.isRemote) {
            stack.setItemDamage(world.getUniqueDataId("map"));
            name = "map_" + stack.getItemDamage();
            data = new MapData(name);
            data.scale = 3;
            int size = 128 * (1 << data.scale);
            data.xCenter = Math.round((float) world.getWorldInfo().getSpawnX() / size) * size;
            data.zCenter = Math.round((float) world.getWorldInfo().getSpawnZ() / size) * size;
            data.dimension = World.mcc$get$provider(world).dimensionId;
            data.markDirty();
            world.setItemData(name, data);
        }
        return data;
    }

    /** A modern map with exactly this centre (the public factory snaps centres to the modern grid). */
    private static MapItemSavedData mcc$newModern(int x, int z, byte scale, net.minecraft.resources.ResourceKey<Level> dimension) {
        try {
            var c = MapItemSavedData.class.getDeclaredConstructor(int.class, int.class, byte.class, boolean.class, boolean.class, boolean.class,
                    net.minecraft.resources.ResourceKey.class);
            c.setAccessible(true);
            return c.newInstance(x, z, scale, true, false, false, dimension);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return MapItemSavedData.createFresh(x, z, scale, true, false, dimension);
        }
    }

    /**
     * Draws the terrain around the entity into the map, a few columns per call, as a held map does every tick. The modern map's
     * scan does the work: on a vanilla map's data directly, on a mod's data through a modern map sharing its colours.
     */
    public void updateMapData(World world, Entity entity, MapData data) {
        if (world == null || entity == null || data == null || entity.mcc$entity == null) return;
        Level level = world.mcc$liveLevel();
        if (level == null || level.isClientSide) return;
        MapItemSavedData modern;
        if (data instanceof ModernView view) {
            modern = view.modern;
        } else {
            if (WorldProvider.dimensionId(level.dimension()) != data.dimension) return;
            synchronized (SCRATCH) {
                modern = SCRATCH.get(data);
                if (modern == null || modern.centerX != data.xCenter || modern.centerZ != data.zCenter || modern.scale != data.scale
                        || modern.dimension != level.dimension()) {
                    modern = mcc$newModern(data.xCenter, data.zCenter, (byte) Math.max(0, Math.min(4, data.scale)), level.dimension());
                    SCRATCH.put(data, modern);
                }
            }
            modern.colors = data.colors;
        }
        if (net.minecraft.world.item.Items.FILLED_MAP instanceof net.minecraft.world.item.MapItem map) {
            boolean dirty = modern.isDirty();
            map.update(level, entity.mcc$entity, modern);
            if (!(data instanceof ModernView) && modern.isDirty() && !dirty) data.markDirty();
        }
    }

    /** A mod's map draws itself while held, as 1.7.10's map did (the vanilla map is updated by the modern game). */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isCurrentItem) {
        super.onUpdate(stack, world, entity, slot, isCurrentItem);
        if (mcc$vanilla != null || world == null || world.isRemote || !isCurrentItem) return;
        MapData data = getMapData(stack, world);
        if (data != null) updateMapData(world, entity, data);
    }

    /** The packet that sends a vanilla map's contents to the player (null when the player is up to date or it is a mod's map). */
    @Override
    public Packet createMapDataPacket(ItemStack stack, World world, EntityPlayer player) {
        if (mcc$vanilla == null || stack == null || player == null || !(player.mcc$entity instanceof net.minecraft.world.entity.player.Player p)) return null;
        Level level = world == null ? null : world.mcc$fullLevel();
        if (level == null) return null;
        MapId id = stack.mcc$modern().get(net.minecraft.core.component.DataComponents.MAP_ID);
        MapItemSavedData data = id == null ? null : level.getMapData(id);
        if (data == null) return null;
        data.tickCarriedBy(p, stack.mcc$modern());
        net.minecraft.network.protocol.Packet<?> modern = data.getUpdatePacket(id, p);
        return modern == null ? null : new ModernPacket(modern);
    }

    /** A modern packet handed to legacy code, sent as it is. */
    private static final class ModernPacket extends Packet {
        private final net.minecraft.network.protocol.Packet<?> modern;

        ModernPacket(net.minecraft.network.protocol.Packet<?> modern) {
            this.modern = modern;
        }

        @Override
        public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
            player.connection.send(modern);
        }
    }
}
