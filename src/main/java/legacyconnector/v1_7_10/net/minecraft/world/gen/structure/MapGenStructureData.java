package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.WorldSavedData;

/** 1.7.10's saved structure starts of one structure generator: each start's NBT under its chunk key "[x,z]". */
public class MapGenStructureData extends WorldSavedData {
    private NBTTagCompound field_143044_a = new NBTTagCompound();

    public MapGenStructureData(String name) {
        super(name);
    }

    @Override
    public synchronized void readFromNBT(NBTTagCompound tag) {
        this.field_143044_a = tag.getCompoundTag("Features");
    }

    @Override
    public synchronized void writeToNBT(NBTTagCompound tag) {
        tag.setTag("Features", this.field_143044_a);
    }

    /** Stores (or replaces) the start of chunk (x, z). */
    public synchronized void func_143043_a(NBTTagCompound tag, int chunkX, int chunkZ) {
        this.field_143044_a.setTag(func_143042_b(chunkX, chunkZ), tag);
    }

    public static String func_143042_b(int chunkX, int chunkZ) {
        return "[" + chunkX + "," + chunkZ + "]";
    }

    /** All stored starts, by chunk key. */
    public synchronized NBTTagCompound func_143041_a() {
        return this.field_143044_a;
    }
}
