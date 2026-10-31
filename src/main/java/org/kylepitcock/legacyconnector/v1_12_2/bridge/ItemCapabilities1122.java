package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraftforge.energy.CapabilityEnergy;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.kylepitcock.legacyconnector.api.internal.BuiltinAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Items of 1.12.2 mods that attach capabilities in {@code Item.initCapabilities} (fluid containers, battery items, backpacks) as NeoForge
 * item capabilities. Modern stacks are copied and re-created all the time, so nothing is cached: every call looks the legacy provider
 * up again on a view of the stack (the state lives in the stack's data) and writes changes back to it.
 */
public final class ItemCapabilities1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/ItemCaps1122");

    private ItemCapabilities1122() {}

    public static void register(RegisterCapabilitiesEvent event) {
        int count = 0;
        for (Item item : ItemBridge1122.modItems()) {
            if (!declaresCapabilities(item.getClass())) continue;
            var variants = ItemBridge1122.variants(item).values().toArray(new net.minecraft.world.item.Item[0]);
            if (variants.length == 0) continue;
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> has(stack, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) ? FluidBridge1122.forStack(stack) : null, variants);
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> has(stack, CapabilityEnergy.ENERGY) ? new Energy(stack) : null, variants);
            event.registerItem(Capabilities.ItemHandler.ITEM, (stack, ctx) -> has(stack, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) ? new Items(stack) : null, variants);
            count++;
        }
        LOG.info("{} 1.12.2 item(s) attach capabilities", count);
    }

    private static boolean declaresCapabilities(Class<?> type) {
        try {
            return type.getMethod("initCapabilities", legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class,
                    legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound.class).getDeclaringClass() != Item.class;
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    private static boolean has(ItemStack stack, legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability) {
        try {
            return Stacks1122.fromModern(stack).getCapability(capability, null) != null;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private record Energy(ItemStack stack) implements IEnergyStorage {
        private legacyconnector.v1_12_2.net.minecraftforge.energy.IEnergyStorage live() {
            var s = Stacks1122.fromModern(stack).getCapability(CapabilityEnergy.ENERGY, null);
            return s == null ? EMPTY : s;
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int r = live().receiveEnergy(amount, simulate);
            if (!simulate) Stacks1122.flushViews();
            return r;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int r = live().extractEnergy(amount, simulate);
            if (!simulate) Stacks1122.flushViews();
            return r;
        }

        @Override
        public int getEnergyStored() {
            return live().getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return live().getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return live().canExtract();
        }

        @Override
        public boolean canReceive() {
            return live().canReceive();
        }
    }

    private static final legacyconnector.v1_12_2.net.minecraftforge.energy.IEnergyStorage EMPTY = new legacyconnector.v1_12_2.net.minecraftforge.energy.EnergyStorage(0);

    private record Items(ItemStack stack) implements IItemHandler {
        private IItemHandler live() {
            var h = Stacks1122.fromModern(stack).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            return h == null ? new net.neoforged.neoforge.items.ItemStackHandler(0) : BuiltinAdapters.wrapItems1122(h);
        }

        @Override
        public int getSlots() {
            return live().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return live().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack s, boolean simulate) {
            ItemStack r = live().insertItem(slot, s, simulate);
            if (!simulate) Stacks1122.flushViews();
            return r;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack r = live().extractItem(slot, amount, simulate);
            if (!simulate) Stacks1122.flushViews();
            return r;
        }

        @Override
        public int getSlotLimit(int slot) {
            return live().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack s) {
            return live().isItemValid(slot, s);
        }
    }
}
