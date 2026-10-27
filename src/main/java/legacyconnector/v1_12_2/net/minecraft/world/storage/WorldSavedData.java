package legacyconnector.v1_12_2.net.minecraft.world.storage;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2 per-world mod data; persisted through the modern saved-data storage (see {@link MapStorage}). */
public abstract class WorldSavedData {
    public final String mapName;
    private boolean dirty;

    public WorldSavedData(String name) {
        this.mapName = name;
    }

    public abstract void readFromNBT(NBTTagCompound tag);

    public abstract NBTTagCompound writeToNBT(NBTTagCompound tag);

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
