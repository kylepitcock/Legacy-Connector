package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyConnection;
import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyContainerItem;
import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyProvider;
import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyReceiver;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Redstone Flux (the 1.12.2 {@code cofh.redstoneflux.api}: the {@code IEnergyReceiver}/{@code IEnergyProvider} tiles and
 * {@code IEnergyContainerItem} items of Thermal Expansion, Thermal Dynamics, Mekanism's RF support, ...) as NeoForge's energy
 * capability, 1 RF = 1 FE. Forge Energy (a tile answering {@code CapabilityEnergy.ENERGY}) is handled before this by
 * {@code BuiltinAdapters.energy1122}; this is the fallback for tiles that only implement the RF interfaces.
 */
public final class EnergyBridge1122 {
    private EnergyBridge1122() {}

    /** The RF view of a tile for a side, or null when it is not an RF tile or does not connect on that side. */
    public static IEnergyStorage forTile(TileEntity tile, Direction side) {
        if (!(tile instanceof IEnergyReceiver) && !(tile instanceof IEnergyProvider)) return null;
        EnumFacing from = side == null ? null : EnumFacing.mcc$of(side);
        if (side != null && tile instanceof IEnergyConnection c && !c.canConnectEnergy(from)) return null;
        return new TileStorage(tile, from);
    }

    /** Registers the energy item capability of every 1.12.2 item implementing Redstone Flux's {@code IEnergyContainerItem}. */
    public static void registerItems(RegisterCapabilitiesEvent event) {
        for (Item item : ItemBridge1122.modItems()) {
            if (!(item instanceof IEnergyContainerItem container)) continue;
            var variants = ItemBridge1122.variants(item).values().toArray(new net.minecraft.world.item.Item[0]);
            if (variants.length == 0) continue;
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new ItemStorage(container, stack), variants);
        }
    }

    /** An RF tile as energy storage; every call goes to the live tile. */
    public record TileStorage(TileEntity tile, EnumFacing from) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return amount > 0 && tile instanceof IEnergyReceiver r ? Math.max(0, r.receiveEnergy(from, amount, simulate)) : 0;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return amount > 0 && tile instanceof IEnergyProvider p ? Math.max(0, p.extractEnergy(from, amount, simulate)) : 0;
        }

        @Override
        public int getEnergyStored() {
            if (tile instanceof IEnergyReceiver r) return r.getEnergyStored(from);
            return tile instanceof IEnergyProvider p ? p.getEnergyStored(from) : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            if (tile instanceof IEnergyReceiver r) return r.getMaxEnergyStored(from);
            return tile instanceof IEnergyProvider p ? p.getMaxEnergyStored(from) : 0;
        }

        @Override
        public boolean canExtract() {
            return tile instanceof IEnergyProvider;
        }

        @Override
        public boolean canReceive() {
            return tile instanceof IEnergyReceiver;
        }
    }

    /** An RF item stack as energy storage. The stack's data is the legacy item's state, so changes are flushed to it. */
    public record ItemStorage(IEnergyContainerItem item, net.minecraft.world.item.ItemStack stack) implements IEnergyStorage {
        private legacyconnector.v1_12_2.net.minecraft.item.ItemStack view() {
            return Stacks1122.fromModern(stack);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (amount <= 0) return 0;
            int r = Math.max(0, item.receiveEnergy(view(), amount, simulate));
            if (!simulate && r > 0) Stacks1122.flushViews();
            return r;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            if (amount <= 0) return 0;
            int r = Math.max(0, item.extractEnergy(view(), amount, simulate));
            if (!simulate && r > 0) Stacks1122.flushViews();
            return r;
        }

        @Override
        public int getEnergyStored() {
            return item.getEnergyStored(view());
        }

        @Override
        public int getMaxEnergyStored() {
            return item.getMaxEnergyStored(view());
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
