package legacyconnector.v1_12_2.net.minecraft.util.datafix;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

public interface IFixableData {
    int getFixVersion();

    NBTTagCompound fixTagCompound(NBTTagCompound compound);
}
