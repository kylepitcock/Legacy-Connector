package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;

public interface INBTSerializable<T extends NBTBase> {
    T serializeNBT();

    void deserializeNBT(T nbt);
}
