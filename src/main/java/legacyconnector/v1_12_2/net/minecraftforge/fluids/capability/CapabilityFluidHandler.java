package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityInject;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityManager;

/** The fluid handler capability. */
public class CapabilityFluidHandler {
    @CapabilityInject(IFluidHandler.class)
    public static Capability<IFluidHandler> FLUID_HANDLER_CAPABILITY = null;
    @CapabilityInject(IFluidHandlerItem.class)
    public static Capability<IFluidHandlerItem> FLUID_HANDLER_ITEM_CAPABILITY = null;

    static {
        CapabilityManager.INSTANCE.register(IFluidHandler.class, new Capability.IStorage<IFluidHandler>() {
            @Override
            public NBTBase writeNBT(Capability<IFluidHandler> capability, IFluidHandler instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IFluidHandler> capability, IFluidHandler instance, EnumFacing side, NBTBase nbt) {
            }
        }, () -> new legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidTank(1000));
        FLUID_HANDLER_CAPABILITY = CapabilityManager.INSTANCE.mcc$capability(IFluidHandler.class);
        // the item capability was never registered, so it stayed null and every mod asking an item stack for it threw (JEI's fluid subtype
        // interpreter on the Aether's buckets)
        CapabilityManager.INSTANCE.register(IFluidHandlerItem.class, new Capability.IStorage<IFluidHandlerItem>() {
            @Override
            public NBTBase writeNBT(Capability<IFluidHandlerItem> capability, IFluidHandlerItem instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IFluidHandlerItem> capability, IFluidHandlerItem instance, EnumFacing side, NBTBase nbt) {
            }
        }, () -> null);
        FLUID_HANDLER_ITEM_CAPABILITY = CapabilityManager.INSTANCE.mcc$capability(IFluidHandlerItem.class);
    }

    public static void register() {
    }
}
