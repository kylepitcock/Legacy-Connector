package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidTank;

/** Forge: a tile entity holding one bucket-sized tank, saved with it and offered as its fluid handler on every side. */
public class TileFluidHandler extends TileEntity {
    protected FluidTank tank = new FluidTank(Fluid.BUCKET_VOLUME);

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        tank.readFromNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag = super.writeToNBT(tag);
        tank.writeToNBT(tag);
        return tag;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability != null && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability != null && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return (T) tank;
        return super.getCapability(capability, facing);
    }
}
