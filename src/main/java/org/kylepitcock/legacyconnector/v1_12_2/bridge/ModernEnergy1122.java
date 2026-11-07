package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyProvider;
import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyReceiver;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.energy.CapabilityEnergy;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The other direction of the 1.12.2 energy bridge: a block entity of a NeoForge mod (a modern battery, generator or machine) that offers
 * the energy capability, seen by 1.12.2 code as a tile entity. Legacy generators and cables look at the neighbour with
 * {@code world.getTileEntity(pos)} and then ask it for {@code CapabilityEnergy.ENERGY} (Forge Energy) or test it for
 * {@code IEnergyReceiver}/{@code IEnergyProvider} (Redstone Flux). Every call goes to the live capability, so the view follows sides and
 * state changes of the modern block.
 */
public final class ModernEnergy1122 {
    private ModernEnergy1122() {}

    /** Whether the block entity offers an energy capability on some side (or sideless). */
    public static boolean offers(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) return false;
        // capabilities are registered per block entity type: ask once per type (the views are looked up on every getTileEntity)
        return OFFERS.computeIfAbsent(be.getType(), t -> probe(be, level));
    }

    private static final java.util.Map<net.minecraft.world.level.block.entity.BlockEntityType<?>, Boolean> OFFERS = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean probe(BlockEntity be, Level level) {
        var pos = be.getBlockPos();
        if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null) return true;
        for (Direction d : Direction.values()) {
            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d) != null) return true;
        }
        return false;
    }

    /** The block entity's live energy capability for a side, or null. */
    public static IEnergyStorage modern(BlockEntity be, EnumFacing side) {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) return null;
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), side == null ? null : side.mcc$modern());
    }

    /** NeoForge energy as the Forge 1.12 interface (null when the block entity has none on that side). */
    public static legacyconnector.v1_12_2.net.minecraftforge.energy.IEnergyStorage legacy(BlockEntity be, EnumFacing side) {
        return modern(be, side) == null ? null : new Legacy(be, side);
    }

    /** The answer to {@code getCapability} on a view: Forge Energy for the energy capability, null for any other. */
    @SuppressWarnings("unchecked")
    public static <T> T capability(BlockEntity be, Capability<T> capability, EnumFacing side) {
        return capability == CapabilityEnergy.ENERGY ? (T) legacy(be, side) : null;
    }

    private record Legacy(BlockEntity be, EnumFacing side) implements legacyconnector.v1_12_2.net.minecraftforge.energy.IEnergyStorage {
        private IEnergyStorage live() {
            return modern(be, side);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            var e = live();
            return e != null && maxReceive > 0 && e.canReceive() ? e.receiveEnergy(maxReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            var e = live();
            return e != null && maxExtract > 0 && e.canExtract() ? e.extractEnergy(maxExtract, simulate) : 0;
        }

        @Override
        public int getEnergyStored() {
            var e = live();
            return e == null ? 0 : e.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            var e = live();
            return e == null ? 0 : e.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            var e = live();
            return e != null && e.canExtract();
        }

        @Override
        public boolean canReceive() {
            var e = live();
            return e != null && e.canReceive();
        }
    }

    /** A modern energy block entity as a 1.12.2 tile entity: Forge Energy capability plus the RF interfaces. */
    public static final class EnergyTile extends TileEntity implements IEnergyReceiver, IEnergyProvider {
        private final BlockEntity host;

        public EnergyTile(BlockEntity host) {
            this.host = host;
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY && modern(host, facing) != null;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability(host, capability, facing);
        }

        @Override
        public boolean canConnectEnergy(EnumFacing from) {
            return modern(host, from) != null;
        }

        @Override
        public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
            var e = modern(host, from);
            return e != null && maxReceive > 0 && e.canReceive() ? e.receiveEnergy(maxReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) {
            var e = modern(host, from);
            return e != null && maxExtract > 0 && e.canExtract() ? e.extractEnergy(maxExtract, simulate) : 0;
        }

        @Override
        public int getEnergyStored(EnumFacing from) {
            var e = modern(host, from);
            return e == null ? 0 : e.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored(EnumFacing from) {
            var e = modern(host, from);
            return e == null ? 0 : e.getMaxEnergyStored();
        }
    }
}
