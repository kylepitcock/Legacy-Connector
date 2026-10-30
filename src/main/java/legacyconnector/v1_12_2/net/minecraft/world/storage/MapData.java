package legacyconnector.v1_12_2.net.minecraft.world.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * 1.12.2 map data (a map item's centre, scale, dimension, colours and icons), saved like other world data. A view of a modern map
 * ({@link #mcc$of}) starts with the modern map's values. Target icons added to a map stack ({@link #addTargetDecoration}) go into
 * the stack's legacy NBT and its modern map decorations.
 */
public class MapData extends WorldSavedData {
    public int xCenter;
    public int zCenter;
    /** The map's dimension id (Forge widened 1.12's byte to an int). */
    public int dimension;
    public boolean trackingPosition;
    public boolean unlimitedTracking;
    public byte scale;
    public byte[] colors = new byte[16384];
    public Map<String, MapDecoration> mapDecorations = new LinkedHashMap<>();
    public java.util.List<Object> playersArrayList = new java.util.ArrayList<>();
    private final Map<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer, MapInfo> playersHashMap = new java.util.HashMap<>();

    public MapData(String mapName) {
        super(mapName);
    }

    /** A legacy view of a modern map's state (copied when made). */
    public static MapData mcc$of(String name, net.minecraft.world.level.saveddata.maps.MapItemSavedData modern) {
        MapData d = new MapData(name);
        if (modern == null) return d;
        d.xCenter = modern.centerX;
        d.zCenter = modern.centerZ;
        d.scale = modern.scale;
        d.trackingPosition = true;
        d.unlimitedTracking = false;
        System.arraycopy(modern.colors, 0, d.colors, 0, Math.min(d.colors.length, modern.colors.length));
        d.dimension = legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$idOf(modern.dimension);
        for (var dec : modern.getDecorations()) {
            MapDecoration.Type type = MapDecoration.Type.PLAYER;
            for (MapDecoration.Type t : MapDecoration.Type.values()) if (t.mcc$modern().equals(dec.type())) type = t;
            d.mapDecorations.put(dec.name().map(c -> c.getString()).orElse(String.valueOf(d.mapDecorations.size())),
                    new MapDecoration(type, dec.x(), dec.y(), dec.rot()));
        }
        return d;
    }

    public void calculateMapCenter(double x, double z, int mapScale) {
        int i = 128 * (1 << mapScale);
        int j = (int) Math.floor((x + 64.0D) / (double) i);
        int k = (int) Math.floor((z + 64.0D) / (double) i);
        this.xCenter = j * i + i / 2 - 64;
        this.zCenter = k * i + i / 2 - 64;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.dimension = nbt.getInteger("dimension");
        this.xCenter = nbt.getInteger("xCenter");
        this.zCenter = nbt.getInteger("zCenter");
        this.scale = (byte) Math.max(0, Math.min(4, nbt.getByte("scale")));
        this.trackingPosition = !nbt.hasKey("trackingPosition", 1) || nbt.getBoolean("trackingPosition");
        this.unlimitedTracking = nbt.getBoolean("unlimitedTracking");
        byte[] c = nbt.getByteArray("colors");
        if (c.length == 16384) this.colors = c;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("dimension", this.dimension);
        compound.setInteger("xCenter", this.xCenter);
        compound.setInteger("zCenter", this.zCenter);
        compound.setByte("scale", this.scale);
        compound.setShort("width", (short) 128);
        compound.setShort("height", (short) 128);
        compound.setByteArray("colors", this.colors);
        compound.setBoolean("trackingPosition", this.trackingPosition);
        compound.setBoolean("unlimitedTracking", this.unlimitedTracking);
        return compound;
    }

    public void updateMapData(int x, int y) {
        markDirty();
        for (Object o : playersArrayList) if (o instanceof MapInfo info) info.update(x, y);
    }

    /** Adds a target icon (treasure X, monument, mansion...) at a world position to a map stack. */
    public static void addTargetDecoration(ItemStack map, BlockPos target, String decorationName, MapDecoration.Type type) {
        if (map == null || map.isEmpty() || target == null || type == null) return;
        NBTTagList list = map.hasTagCompound() && map.getTagCompound().hasKey("Decorations", 9) ? map.getTagCompound().getTagList("Decorations", 10) : new NBTTagList();
        NBTTagCompound c = new NBTTagCompound();
        c.setByte("type", type.getIcon());
        c.setString("id", decorationName);
        c.setDouble("x", (double) target.getX());
        c.setDouble("z", (double) target.getZ());
        c.setDouble("rot", 180.0D);
        list.appendTag(c);
        map.setTagInfo("Decorations", list);
        try {
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModernLive(map);
            if (!modern.isEmpty()) {
                net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(modern, target.mcc$modern(), decorationName, type.mcc$modern());
            }
        } catch (RuntimeException | LinkageError e) {
            // the legacy NBT keeps the icon
        }
    }

    /** The map number of this data ({@code map_N}), or 0. */
    private int mcc$mapId() {
        try {
            return mapName != null && mapName.startsWith("map_") ? Integer.parseInt(mapName.substring(4)) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** The tracking state of a player holding this map (made on first use). */
    public MapInfo getMapInfo(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        MapInfo info = playersHashMap.get(player);
        if (info == null) {
            info = new MapInfo(player);
            playersHashMap.put(player, info);
            playersArrayList.add(info);
        }
        return info;
    }

    /**
     * Updates the player icons: players that left or no longer carry the map are dropped, and the holder's icon (with its heading) is
     * placed where it stands, or at the map's edge when it is outside and the map tracks without limit.
     */
    public void updateVisiblePlayers(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, ItemStack mapStack) {
        if (player == null) return;
        if (!playersHashMap.containsKey(player)) getMapInfo(player);
        for (var it = playersArrayList.iterator(); it.hasNext(); ) {
            if (!(it.next() instanceof MapInfo info)) continue;
            var p = info.player;
            String name = p.getName();
            boolean holds = p.mcc$player() != null && !p.mcc$player().isRemoved() && (mapStack == null || legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.mcc$get$inventory(p).hasItemStack(mapStack) || mapStack.isOnItemFrame());
            if (!holds) {
                playersHashMap.remove(p);
                it.remove();
                mapDecorations.remove(name);
            } else if (trackingPosition && p.mcc$player().level() != null
                    && legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$idOf(p.mcc$player().level().dimension()) == dimension) {
                updateDecorations(MapDecoration.Type.PLAYER, name, p.mcc$player().getX(), p.mcc$player().getZ(), p.mcc$player().getYRot());
            }
        }
    }

    private void updateDecorations(MapDecoration.Type type, String name, double worldX, double worldZ, double rotation) {
        int i = 1 << scale;
        float f = (float) (worldX - xCenter) / i;
        float f1 = (float) (worldZ - zCenter) / i;
        byte b0 = (byte) ((int) (f * 2.0F + 0.5D));
        byte b1 = (byte) ((int) (f1 * 2.0F + 0.5D));
        byte b2;
        if (f >= -63.0F && f1 >= -63.0F && f <= 63.0F && f1 <= 63.0F) {
            rotation = rotation + (rotation < 0.0D ? -8.0D : 8.0D);
            b2 = (byte) ((int) (rotation * 16.0D / 360.0D));
        } else {
            if (type != MapDecoration.Type.PLAYER || !unlimitedTracking && Math.abs(f) >= 320.0F && Math.abs(f1) >= 320.0F) {
                mapDecorations.remove(name);
                return;
            }
            type = Math.abs(f) < 320.0F && Math.abs(f1) < 320.0F ? MapDecoration.Type.PLAYER_OFF_MAP : MapDecoration.Type.PLAYER_OFF_LIMITS;
            b2 = 0;
            if (f <= -63.0F) b0 = -128;
            if (f1 <= -63.0F) b1 = -128;
            if (f >= 63.0F) b0 = 127;
            if (f1 >= 63.0F) b1 = 127;
        }
        mapDecorations.put(name, new MapDecoration(type, b0, b1, b2));
    }

    /** The update packet for a player holding the map (null when nothing changed for it). */
    public legacyconnector.v1_12_2.net.minecraft.network.Packet<?> getMapPacket(ItemStack mapStack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        MapInfo info = playersHashMap.get(player);
        return info == null ? null : info.getPacket(mapStack);
    }

    /** One player's view of the map: which rectangle of colours changed since it was last sent. */
    public class MapInfo {
        public final legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player;
        private boolean isDirty = true;
        private int minX;
        private int minY;
        private int maxX = 127;
        private int maxY = 127;
        private int tick;
        public int step;

        public MapInfo(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
            this.player = player;
        }

        public legacyconnector.v1_12_2.net.minecraft.network.Packet<?> getPacket(ItemStack stack) {
            if (isDirty) {
                isDirty = false;
                return new legacyconnector.v1_12_2.net.minecraft.network.play.server.SPacketMaps(mcc$mapId(), scale, trackingPosition, mapDecorations.values(),
                        colors, minX, minY, maxX + 1 - minX, maxY + 1 - minY);
            }
            return tick++ % 5 == 0 ? new legacyconnector.v1_12_2.net.minecraft.network.play.server.SPacketMaps(mcc$mapId(), scale, trackingPosition,
                    mapDecorations.values(), colors, 0, 0, 0, 0) : null;
        }

        public void update(int x, int y) {
            if (isDirty) {
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            } else {
                isDirty = true;
                minX = x;
                minY = y;
                maxX = x;
                maxY = y;
            }
        }
    }
}
