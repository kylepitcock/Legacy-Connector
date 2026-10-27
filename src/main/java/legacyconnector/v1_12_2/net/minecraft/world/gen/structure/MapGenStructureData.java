package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSavedData;

/** 1.12.2 saved structure starts of one structure generator, keyed "[chunkX,chunkZ]". */
public class MapGenStructureData extends WorldSavedData {
    private NBTTagCompound tagCompound = new NBTTagCompound();

    public MapGenStructureData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.tagCompound = nbt.getCompoundTag("Features");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("Features", this.tagCompound);
        return compound;
    }

    public void writeInstance(NBTTagCompound tag, int chunkX, int chunkZ) {
        this.tagCompound.setTag(formatChunkCoords(chunkX, chunkZ), tag);
    }

    public static String formatChunkCoords(int chunkX, int chunkZ) {
        return "[" + chunkX + "," + chunkZ + "]";
    }

    public NBTTagCompound getTagCompound() {
        return this.tagCompound;
    }
}
