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

    /** A column of the map changed; it is saved with the map (the modern game syncs maps to players itself). */
    public void setColumnDirty(int x, int minZ, int maxZ) {
        markDirty();
    }
}
