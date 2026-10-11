package mccompat.v1_7_10.net.minecraft.world;

import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/** Legacy per-world mod data; persisted through the modern saved-data storage (see MapStorage). */
public abstract class WorldSavedData {
    public final String mapName;
    private boolean dirty;

    public WorldSavedData(String name) {
        this.mapName = name;
    }

    public abstract void readFromNBT(NBTTagCompound tag);

    public abstract void writeToNBT(NBTTagCompound tag);

    public void markDirty() {
        setDirty(true);
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }
}
