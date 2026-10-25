package legacyconnector.v1_12_2.net.minecraftforge.common.capabilities;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraftforge.common.util.INBTSerializable;

public interface ICapabilitySerializable<T extends NBTBase> extends ICapabilityProvider, INBTSerializable<T> {
}
