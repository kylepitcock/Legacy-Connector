package legacyconnector.v1_7_10.net.minecraft.world.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.WorldSavedData;

/**
 * A map's saved data in the 1.7.10 format (centre, dimension, scale, 128x128 colour ids), as mods' own map items store it
 * (they subclass it and call {@code super.readFromNBT/writeToNBT}). Vanilla maps are the modern ones.
 */
public class MapData extends WorldSavedData {
    public int xCenter;
    public int zCenter;
    /** Forge widened it to an int (dimension ids above 127). */
    public int dimension;
    public byte scale;
    public byte[] colors = new byte[16384];
    public List playersArrayList = new ArrayList();
    public Map playersHashMap = new HashMap();
    public Map playersVisibleOnMap = new LinkedHashMap();

    public MapData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        // saved by vanilla as a byte, by Forge as an int
        dimension = tag.getTagId("dimension") == 1 ? tag.getByte("dimension") : tag.getInteger("dimension");
        xCenter = tag.getInteger("xCenter");
        zCenter = tag.getInteger("zCenter");
        scale = (byte) Math.max(0, Math.min(4, tag.getByte("scale")));
        int width = tag.getShort("width");
        int height = tag.getShort("height");
        byte[] saved = tag.getByteArray("colors");
        if (width == 128 && height == 128 && saved.length == 16384) {
            colors = saved;
            return;
        }
        colors = new byte[16384];
        int offX = (128 - width) / 2;
        int offZ = (128 - height) / 2;
        for (int z = 0; z < height; z++) {
            int mz = z + offZ;
            if (mz < 0 || mz >= 128) continue;
            for (int x = 0; x < width; x++) {
                int mx = x + offX;
                int from = x + z * width;
                if (mx >= 0 && mx < 128 && from < saved.length) colors[mx + mz * 128] = saved[from];
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("dimension", dimension);
        tag.setInteger("xCenter", xCenter);
        tag.setInteger("zCenter", zCenter);
        tag.setByte("scale", scale);
        tag.setShort("width", (short) 128);
        tag.setShort("height", (short) 128);
        tag.setByteArray("colors", colors);
    }

    /** A column of the map changed: saved with the map, and queued for every player's next update packet. */
    public void setColumnDirty(int x, int minZ, int maxZ) {
        markDirty();
        for (Object o : playersArrayList) {
            MapInfo info = (MapInfo) o;
            if (info.field_76209_b[x] < 0 || info.field_76209_b[x] > minZ) info.field_76209_b[x] = minZ;
            if (info.field_76210_c[x] < 0 || info.field_76210_c[x] < maxZ) info.field_76210_c[x] = maxZ;
        }
    }

    /** The tracking record of a player holding this map (created on first use). */
    @SuppressWarnings("unchecked")
    public MapInfo func_82568_a(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        MapInfo info = (MapInfo) playersHashMap.get(player);
        if (info == null) {
            info = new MapInfo(player);
            playersHashMap.put(player, info);
            playersArrayList.add(info);
        }
        return info;
    }

    /** Updates the player markers: every player carrying the map in this dimension, players who dropped it leave. */
    @SuppressWarnings("unchecked")
    public void updateVisiblePlayers(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        func_82568_a(player);
        if (!player.inventory.hasItemStack(stack)) playersVisibleOnMap.remove(player.getCommandSenderName());
        for (int i = 0; i < playersArrayList.size(); i++) {
            MapInfo info = (MapInfo) playersArrayList.get(i);
            var holder = info.entityplayerObj;
            boolean onFrame = stack != null && stack.isOnItemFrame();
            if (!legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$isDead(holder) && (holder.inventory.hasItemStack(stack) || onFrame)) {
                if (!onFrame && legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$dimension(holder) == dimension) {
                    mcc$marker(0, legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$worldObj(holder), holder.getCommandSenderName(), holder.mcc$x(), holder.mcc$z(),
                            legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationYaw(holder));
                }
            } else {
                playersHashMap.remove(holder);
                playersArrayList.remove(info);
                i--;
            }
        }
    }

    /** Places (or moves) a marker; off the map it becomes a small edge marker, far off it is dropped. In the nether it spins. */
    @SuppressWarnings("unchecked")
    private void mcc$marker(int type, legacyconnector.v1_7_10.net.minecraft.world.World world, String name, double x, double z, double rotation) {
        int step = 1 << scale;
        float fx = (float) (x - xCenter) / step;
        float fz = (float) (z - zCenter) / step;
        byte px = (byte) (int) (fx * 2.0F + 0.5D);
        byte pz = (byte) (int) (fz * 2.0F + 0.5D);
        final byte edge = 63;
        byte rot;
        if (fx >= -edge && fz >= -edge && fx <= edge && fz <= edge) {
            rotation += rotation < 0.0D ? -8.0D : 8.0D;
            rot = (byte) (int) (rotation * 16.0D / 360.0D);
            if (dimension < 0 && world != null) {
                int k = (int) (world.getWorldInfo().getWorldTime() / 10L);
                rot = (byte) (k * k * 34187121 + k * 121 >> 15 & 15);
            }
        } else {
            if (Math.abs(fx) >= 320.0F || Math.abs(fz) >= 320.0F) {
                playersVisibleOnMap.remove(name);
                return;
            }
            type = 6;
            rot = 0;
            if (fx <= -edge) px = (byte) (edge * 2 + 2.5D);
            if (fz <= -edge) pz = (byte) (edge * 2 + 2.5D);
            if (fx >= edge) px = (byte) (edge * 2 + 1);
            if (fz >= edge) pz = (byte) (edge * 2 + 1);
        }
        playersVisibleOnMap.put(name, new MapCoord((byte) type, px, pz, rot));
    }

    /** The next update packet for the player (scale, markers or a changed colour column), or null when nothing changed. */
    public byte[] getUpdatePacketData(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, legacyconnector.v1_7_10.net.minecraft.world.World world,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        MapInfo info = (MapInfo) playersHashMap.get(player);
        return info == null ? null : info.getPlayersOnMap(stack);
    }

    /** Applies an update packet on the client: 0 a colour column, 1 the markers, 2 the scale. */
    @SuppressWarnings("unchecked")
    public void updateMPMapData(byte[] data) {
        if (data == null || data.length == 0) return;
        if (data[0] == 0 && data.length >= 3) {
            int x = data[1] & 255;
            int z = data[2] & 255;
            for (int k = 0; k < data.length - 3; k++) {
                int at = (k + z) * 128 + x;
                if (at >= 0 && at < colors.length) colors[at] = data[k + 3];
            }
            markDirty();
        } else if (data[0] == 1) {
            playersVisibleOnMap.clear();
            for (int i = 0; i < (data.length - 1) / 3; i++) {
                byte size = (byte) (data[i * 3 + 1] >> 4);
                byte cx = data[i * 3 + 2];
                byte cz = data[i * 3 + 3];
                byte rot = (byte) (data[i * 3 + 1] & 15);
                playersVisibleOnMap.put("icon-" + i, new MapCoord(size, cx, cz, rot));
            }
        } else if (data[0] == 2 && data.length >= 2) {
            scale = data[1];
        }
    }

    /** A marker on the map: icon type, position (-128..127 from the centre in half pixels) and rotation (0..15). */
    public class MapCoord {
        public byte iconSize;
        public byte centerX;
        public byte centerZ;
        public byte iconRotation;

        public MapCoord(byte iconSize, byte centerX, byte centerZ, byte iconRotation) {
            this.iconSize = iconSize;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.iconRotation = iconRotation;
        }
    }

    /** What one player was last sent of this map: the dirty column ranges and the markers. */
    public class MapInfo {
        public final legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer entityplayerObj;
        public int[] field_76209_b = new int[128];
        public int[] field_76210_c = new int[128];
        private int field_76208_e;
        private int field_76205_f;
        private byte[] lastPlayerLocationOnMap;
        /** How many ticks the player has held the map (updates are sent every few). */
        public int field_82569_d;
        private boolean field_82570_i;

        public MapInfo(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
            this.entityplayerObj = player;
            for (int i = 0; i < 128; i++) {
                field_76209_b[i] = 0;
                field_76210_c[i] = 127;
            }
        }

        public byte[] getPlayersOnMap(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
            if (!field_82570_i) {
                field_82570_i = true;
                return new byte[] {2, scale};
            }
            if (--field_76205_f < 0) {
                field_76205_f = 4;
                byte[] markers = new byte[playersVisibleOnMap.size() * 3 + 1];
                markers[0] = 1;
                int i = 0;
                for (Object o : playersVisibleOnMap.values()) {
                    MapCoord c = (MapCoord) o;
                    markers[i * 3 + 1] = (byte) (c.iconSize << 4 | c.iconRotation & 15);
                    markers[i * 3 + 2] = c.centerX;
                    markers[i * 3 + 3] = c.centerZ;
                    i++;
                }
                boolean same = stack == null || !stack.isOnItemFrame();
                if (lastPlayerLocationOnMap != null && lastPlayerLocationOnMap.length == markers.length) {
                    for (int j = 0; j < markers.length; j++) {
                        if (markers[j] != lastPlayerLocationOnMap[j]) {
                            same = false;
                            break;
                        }
                    }
                } else {
                    same = false;
                }
                if (!same) {
                    lastPlayerLocationOnMap = markers;
                    return markers;
                }
            }
            int x = field_76208_e++ * 11 % 128;
            if (field_76209_b[x] >= 0) {
                int length = field_76210_c[x] - field_76209_b[x] + 1;
                int z = field_76209_b[x];
                byte[] column = new byte[length + 3];
                column[0] = 0;
                column[1] = (byte) x;
                column[2] = (byte) z;
                for (int k = 0; k < column.length - 3; k++) column[k + 3] = colors[(k + z) * 128 + x];
                field_76210_c[x] = -1;
                field_76209_b[x] = -1;
                return column;
            }
            return null;
        }
    }
}
