package legacyconnector.v1_12_2.net.minecraft.util.datafix;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

public interface IDataFixer {
    NBTTagCompound process(IFixType type, NBTTagCompound compound, int versionIn);
}
