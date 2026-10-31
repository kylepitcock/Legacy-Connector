package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.cofh.api.energy.IEnergyConnection;
import legacyconnector.v1_7_10.cofh.api.energy.IEnergyContainerItem;
import legacyconnector.v1_7_10.cofh.api.energy.IEnergyProvider;
import legacyconnector.v1_7_10.cofh.api.energy.IEnergyReceiver;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * CoFH RF API -> NeoForge Energy capability (1 RF = 1 FE). Legacy RF tiles and items expose {@code IEnergyStorage},
 * so modern cables, machines and chargers can push and pull energy.
 */
public final class EnergyBridge {
    private EnergyBridge() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        var type = TileEntityBridge.type();
        if (type != null) {
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> forTile(be.legacy(), side));
        }
        for (Item item : ItemBridge.modItems()) {
            if (!(item instanceof IEnergyContainerItem container)) continue;
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new ItemStorage(container, stack),
                    item.mcc$variants.values().toArray(new net.minecraft.world.item.Item[0]));
        }
    }

    public static IEnergyStorage forTile(TileEntity legacy, Direction side) {
        if (!(legacy instanceof IEnergyReceiver) && !(legacy instanceof IEnergyProvider)) return null;
        ForgeDirection from = side == null ? ForgeDirection.UNKNOWN : ForgeDirection.getOrientation(side.get3DDataValue());
        if (legacy instanceof IEnergyConnection c && side != null && !c.canConnectEnergy(from)) return null;
        return new TileStorage(legacy, from);
    }

    private record TileStorage(TileEntity tile, ForgeDirection from) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return tile instanceof IEnergyReceiver r ? r.receiveEnergy(from, amount, simulate) : 0;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return tile instanceof IEnergyProvider p ? p.extractEnergy(from, amount, simulate) : 0;
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

    private record ItemStorage(IEnergyContainerItem item, net.minecraft.world.item.ItemStack stack) implements IEnergyStorage {
        private legacyconnector.v1_7_10.net.minecraft.item.ItemStack view() {
            return legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return item.receiveEnergy(view(), amount, simulate);
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return item.extractEnergy(view(), amount, simulate);
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
