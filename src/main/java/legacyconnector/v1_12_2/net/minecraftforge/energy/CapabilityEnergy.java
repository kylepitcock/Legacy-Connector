package legacyconnector.v1_12_2.net.minecraftforge.energy;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagInt;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityInject;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityManager;

public class CapabilityEnergy {
    @CapabilityInject(IEnergyStorage.class)
    public static Capability<IEnergyStorage> ENERGY = null;

    static {
        CapabilityManager.INSTANCE.register(IEnergyStorage.class, new Capability.IStorage<IEnergyStorage>() {
            @Override
            public NBTBase writeNBT(Capability<IEnergyStorage> capability, IEnergyStorage instance, EnumFacing side) {
                return new NBTTagInt(instance.getEnergyStored());
            }

            @Override
            public void readNBT(Capability<IEnergyStorage> capability, IEnergyStorage instance, EnumFacing side, NBTBase nbt) {
                if (!(instance instanceof EnergyStorage storage)) throw new IllegalArgumentException("Can not deserialize to an instance that isn't the default implementation");
                storage.mcc$set(((NBTTagInt) nbt).getInt());
            }
        }, () -> new EnergyStorage(1000));
        ENERGY = CapabilityManager.INSTANCE.mcc$capability(IEnergyStorage.class);
    }

    public static void register() {
    }
}
