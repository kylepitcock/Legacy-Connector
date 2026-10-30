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
}
