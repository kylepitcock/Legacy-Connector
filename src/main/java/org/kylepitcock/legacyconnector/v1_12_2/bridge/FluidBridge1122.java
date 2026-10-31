package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge 1.12.2 fluids -> NeoForge fluids, so tiles and items that answer {@code FLUID_HANDLER_CAPABILITY} work with modern pipes,
 * tanks and Create. Each legacy fluid gets a source-only modern fluid and fluid type (the legacy fluid block stays a legacy block).
 */
public final class FluidBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Fluids1122");
    private static final Map<String, Fluid> MODERN = new HashMap<>();
    private static final Map<Fluid, String> NAMES = new HashMap<>();

    private FluidBridge1122() {}

    /** Registers a modern fluid for every 1.12.2 fluid (call while registries are open, after the mods registered theirs). */
    public static synchronized void register() {
        for (var e : FluidRegistry.getRegisteredFluids().entrySet()) {
            String name = e.getKey();
            if (MODERN.containsKey(name)) continue;
            Fluid modern;
            if (name.equals("water")) modern = net.minecraft.world.level.material.Fluids.WATER;
            else if (name.equals("lava")) modern = net.minecraft.world.level.material.Fluids.LAVA;
            else {
                try {
                    var legacy = e.getValue();
                    String namespace = "legacyconnector";
                    var block = legacy.getBlock();
                    if (block != null && block.getRegistryName() != null) namespace = block.getRegistryName().getNamespace();
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, Ids.normalizePath(name).replace('/', '_'));
                    if (BuiltInRegistries.FLUID.containsKey(id)) id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "f1122_" + id.getNamespace() + "_" + id.getPath());
                    FluidType type = new FluidType(FluidType.Properties.create().descriptionId(legacy.getUnlocalizedName())
                            .density(legacy.getDensity()).temperature(legacy.getTemperature()).viscosity(legacy.getViscosity())
                            .lightLevel(legacy.getLuminosity()).canSwim(false).canDrown(false).supportsBoating(false));
                    LegacyRegistrar.register(net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES, id, type);
                    modern = new SourceFluid(type);
                    LegacyRegistrar.register(BuiltInRegistries.FLUID, id, modern);
                } catch (RuntimeException | LinkageError ex) {
                    LOG.warn("Could not register the modern fluid of {}", name, ex);
                    continue;
                }
            }
            MODERN.put(name, modern);
            NAMES.put(modern, name);
        }
        LOG.info("Registered {} 1.12.2 fluid(s) as modern fluids", MODERN.size());
    }

    public static synchronized FluidStack toModern(legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack stack) {
        if (stack == null || stack.amount <= 0 || stack.getFluid() == null) return FluidStack.EMPTY;
        Fluid fluid = MODERN.get(stack.getFluid().getName());
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, stack.amount);
    }

    public static synchronized legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack fromModern(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = NAMES.get(stack.getFluid());
        var legacy = name == null ? null : FluidRegistry.getFluid(name);
        return legacy == null ? null : new legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack(legacy, stack.getAmount());
    }

    /** A tile's Forge fluid handler capability seen from {@code side} as NeoForge's, or null. */
    public static IFluidHandler forTile(Object tile, Direction side) {
        if (!(tile instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity t)) return null;
        EnumFacing facing = side == null ? null : EnumFacing.mcc$of(side);
        var handler = t.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing);
        return handler == null ? null : new Handler(handler);
    }

    /** The fluid handler of an item stack, looked up on every call so it reads and writes the stack's live data. */
    public static net.neoforged.neoforge.fluids.capability.IFluidHandlerItem forStack(net.minecraft.world.item.ItemStack stack) {
        return new StackHandler(stack);
    }

    private static class Handler implements IFluidHandler {
        final legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler handler;

        Handler(legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler handler) {
            this.handler = handler;
        }

        legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler target() {
            return handler;
        }

        @Override
        public int getTanks() {
            IFluidTankProperties[] p = target().getTankProperties();
            return p == null ? 0 : p.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            IFluidTankProperties[] p = target().getTankProperties();
            return p == null || tank >= p.length ? FluidStack.EMPTY : toModern(p[tank].getContents());
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidTankProperties[] p = target().getTankProperties();
            return p == null || tank >= p.length ? 0 : p[tank].getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            IFluidTankProperties[] p = target().getTankProperties();
            var legacy = fromModern(stack);
            return legacy != null && p != null && tank < p.length && p[tank].canFillFluidType(legacy);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            var legacy = fromModern(resource);
            return legacy == null ? 0 : target().fill(legacy, action.execute());
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            var legacy = fromModern(resource);
            return legacy == null ? FluidStack.EMPTY : toModern(target().drain(legacy, action.execute()));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return toModern(target().drain(maxDrain, action.execute()));
        }
    }

    /** An item's handler: the legacy provider is asked again on each call (it keeps its state in the stack's NBT). */
    private static final class StackHandler extends Handler implements net.neoforged.neoforge.fluids.capability.IFluidHandlerItem {
        private final net.minecraft.world.item.ItemStack stack;

        StackHandler(net.minecraft.world.item.ItemStack stack) {
            super(null);
            this.stack = stack;
        }

        @Override
        legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler target() {
            var view = Stacks1122.fromModern(stack);
            var h = view.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
            return h == null ? EMPTY : h;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int r = super.fill(resource, action);
            if (action.execute()) Stacks1122.flushViews();
            return r;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack r = super.drain(resource, action);
            if (action.execute()) Stacks1122.flushViews();
            return r;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack r = super.drain(maxDrain, action);
            if (action.execute()) Stacks1122.flushViews();
            return r;
        }

        @Override
        public net.minecraft.world.item.ItemStack getContainer() {
            return stack;
        }
    }

    private static final legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler EMPTY =
            legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.templates.EmptyFluidHandler.INSTANCE;

    /** A source-only fluid that exists for stacks and tanks; the world holds the legacy fluid block instead. */
    private static final class SourceFluid extends Fluid {
        private final FluidType type;

        SourceFluid(FluidType type) {
            this.type = type;
        }

        @Override
        public FluidType getFluidType() {
            return type;
        }

        @Override
        public net.minecraft.world.item.Item getBucket() {
            return Items.AIR;
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
            return true;
        }

        @Override
        protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
            return Vec3.ZERO;
        }

        @Override
        public int getTickDelay(net.minecraft.world.level.LevelReader level) {
            return 0;
        }

        @Override
        protected float getExplosionResistance() {
            return 0;
        }

        @Override
        public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
            return 1;
        }

        @Override
        public float getOwnHeight(FluidState state) {
            return 1;
        }

        @Override
        protected BlockState createLegacyBlock(FluidState state) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
            return Shapes.empty();
        }
    }
}
