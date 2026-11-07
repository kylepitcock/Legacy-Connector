package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;

/** Forge 1.20.1's built-in capabilities, each tied to its NeoForge 1.21.1 counterpart. */
public final class ForgeCapabilities {
    private ForgeCapabilities() {
    }

    public static final Capability<IItemHandler> ITEM_HANDLER = CapabilityManager.builtin(IItemHandler.class.getName(),
            new Capability<>("item_handler", Capabilities.ItemHandler.BLOCK, Capabilities.ItemHandler.ENTITY, Capabilities.ItemHandler.ITEM));
    public static final Capability<IFluidHandler> FLUID_HANDLER = CapabilityManager.builtin(IFluidHandler.class.getName(),
            new Capability<>("fluid_handler", Capabilities.FluidHandler.BLOCK, Capabilities.FluidHandler.ENTITY, null));
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Capability<IFluidHandlerItem> FLUID_HANDLER_ITEM = CapabilityManager.builtin(IFluidHandlerItem.class.getName(),
            new Capability("fluid_handler_item", null, null, Capabilities.FluidHandler.ITEM));
    public static final Capability<IEnergyStorage> ENERGY = CapabilityManager.builtin(IEnergyStorage.class.getName(),
            new Capability<>("energy", Capabilities.EnergyStorage.BLOCK, Capabilities.EnergyStorage.ENTITY, Capabilities.EnergyStorage.ITEM));
}
