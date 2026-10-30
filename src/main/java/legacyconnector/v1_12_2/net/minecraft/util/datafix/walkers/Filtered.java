package legacyconnector.v1_12_2.net.minecraft.util.datafix.walkers;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IDataFixer;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IDataWalker;

/**
 * A walker for the data of one entity or tile entity class: it runs on tags whose {@code id} is that class's registry name. The name is
 * looked up when data is processed (mods register their walkers before or after the class).
 */
public abstract class Filtered implements IDataWalker {
    private final Class<?> mcc$type;
    ResourceLocation key;

    public Filtered(Class<?> type) {
        this.mcc$type = type;
    }

    @SuppressWarnings("unchecked")
    private ResourceLocation key() {
        if (key == null && mcc$type != null) {
            if (legacyconnector.v1_12_2.net.minecraft.entity.Entity.class.isAssignableFrom(mcc$type)) {
                key = legacyconnector.v1_12_2.net.minecraft.entity.EntityList.getKey(
                        (Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.Entity>) mcc$type);
            } else if (legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity.class.isAssignableFrom(mcc$type)) {
                key = legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity.getKey(
                        (Class<? extends legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity>) mcc$type);
            }
        }
        return key;
    }

    @Override
    public NBTTagCompound process(IDataFixer fixer, NBTTagCompound compound, int versionIn) {
        ResourceLocation k = key();
        if (k != null && compound != null && compound.hasKey("id", 8) && k.equals(new ResourceLocation(compound.getString("id")))) {
            compound = filteredProcess(fixer, compound, versionIn);
        }
        return compound;
    }

    abstract NBTTagCompound filteredProcess(IDataFixer fixer, NBTTagCompound compound, int versionIn);
}
