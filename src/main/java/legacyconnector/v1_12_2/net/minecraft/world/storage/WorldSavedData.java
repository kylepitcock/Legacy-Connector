package legacyconnector.v1_12_2.net.minecraft.world.storage;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** A mod's per-world data; saved through the modern saved-data storage (see {@link MapStorage}). */
public abstract class WorldSavedData implements legacyconnector.v1_12_2.net.minecraftforge.common.util.INBTSerializable<NBTTagCompound> {
    public final String mapName;
    private boolean dirty;

    public WorldSavedData(String name) {
        this.mapName = name;
    }

    public abstract void readFromNBT(NBTTagCompound nbt);

    public abstract NBTTagCompound writeToNBT(NBTTagCompound compound);

    public void markDirty() {
        setDirty(true);
    }

    public void setDirty(boolean isDirty) {
        this.dirty = isDirty;
    }

    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return tag;
    }
}
