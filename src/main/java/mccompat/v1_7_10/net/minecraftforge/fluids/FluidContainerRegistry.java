package mccompat.v1_7_10.net.minecraftforge.fluids;

import java.util.ArrayList;
import java.util.List;
import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import mccompat.v1_7_10.net.minecraft.init.Items;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraftforge.common.MinecraftForge;

/** Forge 1.7.10 registry of filled/empty fluid container items (buckets, bottles, cells, ...). */
public abstract class FluidContainerRegistry {
    public static final int BUCKET_VOLUME = 1000;
    public static final ItemStack EMPTY_BUCKET = new ItemStack(Items.bucket);
    public static final ItemStack EMPTY_BOTTLE = new ItemStack(Items.glass_bottle);
    private static final List<FluidContainerData> DATA = new ArrayList<>();

    static {
        registerFluidContainer(FluidRegistry.WATER, new ItemStack(Items.water_bucket), EMPTY_BUCKET);
        registerFluidContainer(FluidRegistry.LAVA, new ItemStack(Items.lava_bucket), EMPTY_BUCKET);
        registerFluidContainer(FluidRegistry.WATER, new ItemStack(Items.potionitem), EMPTY_BOTTLE);
    }

    private FluidContainerRegistry() {}

    public static boolean registerFluidContainer(FluidStack stack, ItemStack filledContainer, ItemStack emptyContainer) {
        return registerFluidContainer(new FluidContainerData(stack, filledContainer, emptyContainer));
    }

    public static boolean registerFluidContainer(Fluid fluid, ItemStack filledContainer, ItemStack emptyContainer) {
        return fluid != null && registerFluidContainer(new FluidStack(fluid, BUCKET_VOLUME), filledContainer, emptyContainer);
    }

    public static boolean registerFluidContainer(FluidStack stack, ItemStack filledContainer) {
        return registerFluidContainer(new FluidContainerData(stack, filledContainer, null, true));
    }

    public static boolean registerFluidContainer(Fluid fluid, ItemStack filledContainer) {
        return fluid != null && registerFluidContainer(new FluidStack(fluid, BUCKET_VOLUME), filledContainer);
    }

    public static synchronized boolean registerFluidContainer(FluidContainerData data) {
        if (data == null || data.fluid == null || data.filledContainer == null || isFilledContainer(data.filledContainer)) return false;
        DATA.add(data.copy());
        MinecraftForge.EVENT_BUS.post(new FluidContainerRegisterEvent(data));
        return true;
    }

    public static synchronized FluidStack getFluidForFilledItem(ItemStack container) {
        if (container == null) return null;
        for (FluidContainerData d : DATA) {
            if (d.filledContainer.isItemEqual(container)) return d.fluid.copy();
        }
        return null;
    }

    public static synchronized ItemStack fillFluidContainer(FluidStack fluid, ItemStack container) {
        if (container == null || fluid == null) return null;
        for (FluidContainerData d : DATA) {
            if (d.emptyContainer != null && d.emptyContainer.isItemEqual(container) && fluid.containsFluid(d.fluid)) return d.filledContainer.copy();
        }
        return null;
    }

    public static synchronized ItemStack drainFluidContainer(ItemStack container) {
        if (container == null) return null;
        for (FluidContainerData d : DATA) {
            if (d.filledContainer.isItemEqual(container)) return d.emptyContainer == null ? null : d.emptyContainer.copy();
        }
        return null;
    }

    public static int getContainerCapacity(ItemStack container) {
        return getContainerCapacity(null, container);
    }

    public static synchronized int getContainerCapacity(FluidStack fluid, ItemStack container) {
        if (container == null) return 0;
        for (FluidContainerData d : DATA) {
            boolean matches = d.filledContainer.isItemEqual(container) || d.emptyContainer != null && d.emptyContainer.isItemEqual(container);
            if (matches && (fluid == null || fluid.isFluidEqual(d.fluid))) return d.fluid.amount;
        }
        return 0;
    }

    public static boolean containsFluid(ItemStack container, FluidStack fluid) {
        FluidStack f = getFluidForFilledItem(container);
        return f != null && f.containsFluid(fluid);
    }

    public static boolean isBucket(ItemStack container) {
        return container != null && (container.isItemEqual(EMPTY_BUCKET) || EMPTY_BUCKET.isItemEqual(drainFluidContainer(container)));
    }

    public static boolean isContainer(ItemStack container) {
        return isEmptyContainer(container) || isFilledContainer(container);
    }

    public static synchronized boolean isEmptyContainer(ItemStack container) {
        if (container == null) return false;
        for (FluidContainerData d : DATA) if (d.emptyContainer != null && d.emptyContainer.isItemEqual(container)) return true;
        return false;
    }

    public static boolean isFilledContainer(ItemStack container) {
        return getFluidForFilledItem(container) != null;
    }

    public static synchronized FluidContainerData[] getRegisteredFluidContainerData() {
        return DATA.toArray(new FluidContainerData[0]);
    }

    public static class FluidContainerData {
        public final FluidStack fluid;
        public final ItemStack filledContainer;
        public final ItemStack emptyContainer;

        public FluidContainerData(FluidStack stack, ItemStack filledContainer, ItemStack emptyContainer) {
            this(stack, filledContainer, emptyContainer, false);
        }

        public FluidContainerData(FluidStack stack, ItemStack filledContainer, ItemStack emptyContainer, boolean nullEmpty) {
            this.fluid = stack;
            this.filledContainer = filledContainer;
            this.emptyContainer = emptyContainer;
            if (stack == null || filledContainer == null || emptyContainer == null && !nullEmpty) {
                throw new RuntimeException("Invalid FluidContainerData - a parameter was null.");
            }
        }

        public FluidContainerData copy() {
            return new FluidContainerData(fluid.copy(), filledContainer.copy(), emptyContainer == null ? null : emptyContainer.copy(), true);
        }
    }

    public static class FluidContainerRegisterEvent extends Event {
        public final FluidContainerData data;

        public FluidContainerRegisterEvent(FluidContainerData data) {
            this.data = data.copy();
        }
    }
}
