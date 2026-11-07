package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_6_4.net.minecraftforge.liquids.ILiquidTank;
import legacyconnector.v1_6_4.net.minecraftforge.liquids.ITankContainer;
import legacyconnector.v1_6_4.net.minecraftforge.liquids.LiquidContainerData;
import legacyconnector.v1_6_4.net.minecraftforge.liquids.LiquidDictionary;
import legacyconnector.v1_6_4.net.minecraftforge.liquids.LiquidStack;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidContainerRegistry;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;
import net.minecraft.core.Direction;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects the 1.5.2/1.6.4 liquid API ({@code net.minecraftforge.liquids}) to the 1.7.10 fluid layer and so to NeoForge's fluid capabilities: a
 * {@link LiquidStack} (a block id standing for a liquid) maps to a registered {@link Fluid} (by the {@code LiquidDictionary} name, else by the block),
 * a tile implementing {@link ITankContainer} is exposed as an {@code IFluidHandler} ({@link #forTile}), a liquid in the dictionary gets a fluid of
 * its own when no mod made one, and a registered liquid container is registered with the fluid container registry. Everything the shim's
 * {@code FluidBridge} and the capability rules do for 1.7.10 tanks then works for 1.6.4 ones: pipes of other mods, fluid items, JEI.
 */
public final class Liquids164 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Liquids164");
    /** Containers registered before their liquid had a fluid (Forestry registers its containers before its liquids). */
    private static final List<LiquidContainerData> PENDING = new ArrayList<>();

    private Liquids164() {}

    // ------------------------------------------------------------------ conversion

    /** The 1.7.10 fluid a liquid stands for, or null when none is registered. */
    public static Fluid fluidOf(LiquidStack liquid) {
        if (liquid == null) return null;
        String name = LiquidDictionary.liquidName(liquid);
        if (name != null) {
            Fluid f = FluidRegistry.getFluid(name.toLowerCase(Locale.ROOT));
            if (f != null) return f;
        }
        Block block = Ids.block(liquid.itemID);
        if (block != null) {
            Fluid f = FluidRegistry.lookupFluidForBlock(block);
            if (f != null) return f;
        }
        return switch (liquid.itemID) {
            case 8, 9 -> FluidRegistry.WATER;
            case 10, 11 -> FluidRegistry.LAVA;
            default -> null;
        };
    }

    public static FluidStack toFluid(LiquidStack liquid) {
        Fluid fluid = fluidOf(liquid);
        return fluid == null ? null : new FluidStack(fluid, liquid.amount);
    }

    /** The liquid of a fluid: the dictionary's canonical stack for the fluid's name, else the stack of its block. */
    public static LiquidStack toLiquid(FluidStack stack) {
        if (stack == null || stack.getFluid() == null) return null;
        Fluid fluid = stack.getFluid();
        for (var e : LiquidDictionary.getLiquids().entrySet()) {
            if (e.getKey().equalsIgnoreCase(fluid.getName())) {
                LiquidStack copy = e.getValue().copy();
                copy.amount = stack.amount;
                return copy;
            }
        }
        Block block = fluid.getBlock();
        if (block != null) return new LiquidStack(Ids.blockId(block), stack.amount, 0);
        if (fluid == FluidRegistry.WATER) return new LiquidStack(9, stack.amount, 0);
        if (fluid == FluidRegistry.LAVA) return new LiquidStack(11, stack.amount, 0);
        return null;
    }

    // ------------------------------------------------------------------ dictionary and containers

    /** A liquid named in the dictionary that no fluid stands for yet gets one, so it can travel through tanks, pipes and containers of the modern side. */
    public static void liquidNamed(String name, LiquidStack liquid) {
        if (name == null || liquid == null) return;
        String key = name.toLowerCase(Locale.ROOT);
        if (FluidRegistry.getFluid(key) != null) return;
        try {
            Fluid fluid = new Fluid(key);
            Block block = Ids.block(liquid.itemID);
            if (block != null && FluidRegistry.lookupFluidForBlock(block) == null) fluid.setBlock(block);
            FluidRegistry.registerFluid(fluid);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("No fluid for the liquid {}", name, e);
        }
        flush();
    }

    /** A container item pair registered with the liquid API is registered with the fluid containers as well. */
    public static synchronized void containerRegistered(LiquidContainerData data) {
        if (!register(data)) PENDING.add(data);
    }

    private static boolean register(LiquidContainerData data) {
        FluidStack fluid = toFluid(data.stillLiquid);
        if (fluid == null) return false;
        try {
            FluidContainerRegistry.registerFluidContainer(fluid, data.filled.copy(), data.container.copy());
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Liquid container {} could not be registered as a fluid container", data.filled, e);
        }
        return true;
    }

    private static synchronized void flush() {
        PENDING.removeIf(Liquids164::register);
    }

    // ------------------------------------------------------------------ tanks as NeoForge fluid handlers

    /** The NeoForge fluid handler of a tile implementing the liquid API's {@code ITankContainer}, or null for other tiles. */
    public static net.neoforged.neoforge.fluids.capability.IFluidHandler forTile(Object legacy, Direction side) {
        if (!(legacy instanceof ITankContainer container)) return null;
        flush();
        ForgeDirection from = side == null ? ForgeDirection.UNKNOWN : ForgeDirection.getOrientation(side.get3DDataValue());
        return new Handler(container, from);
    }

    private record Handler(ITankContainer tile, ForgeDirection from) implements net.neoforged.neoforge.fluids.capability.IFluidHandler {
        private ILiquidTank[] tanks() {
            ILiquidTank[] t = tile.getTanks(from);
            return t == null ? new ILiquidTank[0] : t;
        }

        @Override
        public int getTanks() {
            return Math.max(1, tanks().length);
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int i) {
            ILiquidTank[] t = tanks();
            if (i < 0 || i >= t.length || t[i] == null) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            LiquidStack l = t[i].getLiquid();
            return l == null ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : FluidBridge.toModern(toFluid(l));
        }

        @Override
        public int getTankCapacity(int i) {
            ILiquidTank[] t = tanks();
            return i < 0 || i >= t.length || t[i] == null ? 0 : t[i].getCapacity();
        }

        @Override
        public boolean isFluidValid(int i, net.neoforged.neoforge.fluids.FluidStack stack) {
            ILiquidTank[] t = tanks();
            if (i < 0 || i >= t.length || t[i] == null) return false;
            LiquidStack have = t[i].getLiquid();
            LiquidStack want = toLiquid(FluidBridge.fromModern(stack));
            return want != null && (have == null || have.amount <= 0 || have.isLiquidEqual(want));
        }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            LiquidStack liquid = toLiquid(FluidBridge.fromModern(resource));
            return liquid == null ? 0 : tile.fill(from, liquid, action.execute());
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            LiquidStack want = toLiquid(FluidBridge.fromModern(resource));
            if (want == null) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            LiquidStack probe = tile.drain(from, resource.getAmount(), false);
            if (probe == null || !probe.isLiquidEqual(want)) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            LiquidStack out = action.execute() ? tile.drain(from, resource.getAmount(), true) : probe;
            return out == null ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : FluidBridge.toModern(toFluid(out));
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            LiquidStack out = tile.drain(from, maxDrain, action.execute());
            return out == null || out.amount <= 0 ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : FluidBridge.toModern(toFluid(out));
        }
    }
}
