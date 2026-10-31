package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.cofh.api.energy.IEnergyHandler;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidTankInfo;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.IFluidHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Legacy view of a modern block entity (a machine of a NeoForge mod) that offers NeoForge energy or fluid capabilities, so legacy
 * code that pushes into its neighbours finds an {@code IEnergyHandler}/{@code IFluidHandler} there. Many 1.7.10 generators and pumps
 * only push (RotaryCraft's Dynamo has no buffer: its {@code extractEnergy} returns 0 and its tick calls {@code receiveEnergy} on the
 * adjacent {@code IEnergyHandler}), so a modern machine that only pulls through the capability would never get anything from them.
 * Every call goes to the live capability, so the view follows sides and state changes of the modern block.
 */
public final class ModernCapabilityTile extends TileEntity implements IEnergyHandler, IFluidHandler {
    private final BlockEntity host;

    public ModernCapabilityTile(BlockEntity host) {
        this.host = host;
    }

    /** Whether the block entity offers an energy or fluid capability on any side (or sideless). */
    public static boolean offers(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) return false;
        var pos = be.getBlockPos();
        if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null || level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null) return true;
        for (Direction d : Direction.values()) {
            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d) != null || level.getCapability(Capabilities.FluidHandler.BLOCK, pos, d) != null) return true;
        }
        return false;
    }

    private IEnergyStorage energy(ForgeDirection from) {
        Level level = host.getLevel();
        return level == null || host.isRemoved() ? null : level.getCapability(Capabilities.EnergyStorage.BLOCK, host.getBlockPos(), from == null ? null : from.mcc$modern());
    }

    private net.neoforged.neoforge.fluids.capability.IFluidHandler fluids(ForgeDirection from) {
        Level level = host.getLevel();
        return level == null || host.isRemoved() ? null : level.getCapability(Capabilities.FluidHandler.BLOCK, host.getBlockPos(), from == null ? null : from.mcc$modern());
    }

    // ------------------------------------------------------------------ RF

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return energy(from) != null;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        IEnergyStorage e = energy(from);
        return e != null && e.canReceive() ? e.receiveEnergy(maxReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        IEnergyStorage e = energy(from);
        return e != null && e.canExtract() ? e.extractEnergy(maxExtract, simulate) : 0;
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        IEnergyStorage e = energy(from);
        return e == null ? 0 : e.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        IEnergyStorage e = energy(from);
        return e == null ? 0 : e.getMaxEnergyStored();
    }

    // ------------------------------------------------------------------ fluids

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        var handler = fluids(from);
        var stack = FluidBridge.toModern(resource);
        return handler == null || stack.isEmpty() ? 0 : handler.fill(stack, doFill ? FluidAction.EXECUTE : FluidAction.SIMULATE);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        var handler = fluids(from);
        var stack = FluidBridge.toModern(resource);
        return handler == null || stack.isEmpty() ? null : FluidBridge.fromModern(handler.drain(stack, doDrain ? FluidAction.EXECUTE : FluidAction.SIMULATE));
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        var handler = fluids(from);
        return handler == null ? null : FluidBridge.fromModern(handler.drain(maxDrain, doDrain ? FluidAction.EXECUTE : FluidAction.SIMULATE));
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        var handler = fluids(from);
        if (handler == null || fluid == null) return false;
        var probe = FluidBridge.toModern(new FluidStack(fluid, 1));
        return !probe.isEmpty() && handler.fill(probe, FluidAction.SIMULATE) > 0;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        var handler = fluids(from);
        if (handler == null || fluid == null) return false;
        var probe = FluidBridge.toModern(new FluidStack(fluid, 1));
        return !probe.isEmpty() && !handler.drain(probe, FluidAction.SIMULATE).isEmpty();
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        var handler = fluids(from);
        if (handler == null) return new FluidTankInfo[0];
        FluidTankInfo[] out = new FluidTankInfo[handler.getTanks()];
        for (int i = 0; i < out.length; i++) out[i] = new FluidTankInfo(FluidBridge.fromModern(handler.getFluidInTank(i)), handler.getTankCapacity(i));
        return out;
    }
}
