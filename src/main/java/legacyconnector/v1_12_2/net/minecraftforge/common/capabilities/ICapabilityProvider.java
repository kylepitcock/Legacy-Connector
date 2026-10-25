package legacyconnector.v1_12_2.net.minecraftforge.common.capabilities;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

public interface ICapabilityProvider {
    boolean hasCapability(Capability<?> capability, EnumFacing facing);

    <T> T getCapability(Capability<T> capability, EnumFacing facing);
}
