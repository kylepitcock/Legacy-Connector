package legacyconnector.v1_12_2.net.minecraft.util.datafix;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

public interface IDataWalker {
    NBTTagCompound process(IDataFixer fixer, NBTTagCompound compound, int versionIn);
}
