package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Modern block entities seen by 1.12.2 code: the NeoForge fluid handler of the position ({@code Capabilities.FluidHandler.BLOCK}) answers Forge's
 * {@code FLUID_HANDLER_CAPABILITY}, so pumps, pipes and tanks of legacy mods find cauldrons and the tanks of modern mods. Fluids with no
 * 1.12.2 counterpart travel as stand-in legacy fluids ({@link FluidBridge1122#fromModern}). Every call asks the level again.
 */
public final class ModernFluids1122 {
    private ModernFluids1122() {}

    public static net.neoforged.neoforge.fluids.capability.IFluidHandler modern(BlockEntity be, EnumFacing side) {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), side == null ? null : side.mcc$modern());
    }

    /** Whether the block entity has a fluid handler on some side (or sideless). */
    public static boolean offers(BlockEntity be) {
        if (modern(be, null) != null) return true;
        for (EnumFacing f : EnumFacing.values()) if (modern(be, f) != null) return true;
        return false;
    }

    /** The answer to {@code getCapability} for the fluid handler capability, null for any other capability or none offered. */
    @SuppressWarnings("unchecked")
    public static <T> T capability(BlockEntity be, Capability<T> capability, EnumFacing side) {
        if (capability == null || capability != CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return null;
        return modern(be, side) == null ? null : (T) new Legacy(be, side);
    }

    private static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action(boolean execute) {
        return execute ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;
    }

    private record Legacy(BlockEntity be, EnumFacing side) implements IFluidHandler {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            var h = modern(be, side);
            if (h == null) return new IFluidTankProperties[0];
            IFluidTankProperties[] out = new IFluidTankProperties[h.getTanks()];
            for (int i = 0; i < out.length; i++) {
                out[i] = new FluidTankProperties(FluidBridge1122.fromModern(h.getFluidInTank(i)), h.getTankCapacity(i), true, true);
            }
            return out;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            var h = modern(be, side);
            var in = FluidBridge1122.toModern(resource);
            return h == null || in.isEmpty() ? 0 : h.fill(in, action(doFill));
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            var h = modern(be, side);
            var in = FluidBridge1122.toModern(resource);
            return h == null || in.isEmpty() ? null : FluidBridge1122.fromModern(h.drain(in, action(doDrain)));
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            var h = modern(be, side);
            return h == null ? null : FluidBridge1122.fromModern(h.drain(maxDrain, action(doDrain)));
        }
    }
}
