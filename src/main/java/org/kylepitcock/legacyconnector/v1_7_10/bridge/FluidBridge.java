package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidTankInfo;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.IFluidContainerItem;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.IFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge 1.7.10 fluids -> NeoForge fluids. Each legacy fluid gets a modern fluid and fluid type (not placeable: the
 * legacy fluid block stays a legacy block), tanks of legacy tiles and fluid container items become
 * {@code Capabilities.FluidHandler}, so modern pipes, tanks and Jade/JEI see them.
 */
public final class FluidBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Fluids");
    private static final Map<String, Fluid> MODERN = new HashMap<>();

    private FluidBridge() {}

    /** Registers a modern fluid for every legacy fluid (call while registries are open). */
    public static synchronized void register() {
        for (var e : FluidRegistry.getRegisteredFluids().entrySet()) {
            String name = e.getKey();
            if (name.equals("water")) {
                MODERN.put(name, net.minecraft.world.level.material.Fluids.WATER);
                continue;
            }
            if (name.equals("lava")) {
                MODERN.put(name, net.minecraft.world.level.material.Fluids.LAVA);
                continue;
            }
            if (MODERN.containsKey(name)) continue;
            try {
                var legacy = e.getValue();
                String namespace = ownerNamespace(legacy);
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, Ids.normalizePath(name).replace('/', '_'));
                if (BuiltInRegistries.FLUID.containsKey(id)) id = ResourceLocation.fromNamespaceAndPath("legacyconnector", id.getNamespace() + "_" + id.getPath());
                LegacyFluidType type = new LegacyFluidType(name, legacy);
                LegacyRegistrar.register(net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES, id, type);
                Fluid modern = new LegacyModernFluid(type);
                LegacyRegistrar.register(BuiltInRegistries.FLUID, id, modern);
                MODERN.put(name, modern);
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("Could not register the modern fluid of {}", name, ex);
            }
        }
        LOG.info("Registered {} legacy fluid(s) as modern fluids", MODERN.size());
    }

    private static String ownerNamespace(legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid fluid) {
        var block = fluid.getBlock();
        if (block != null && !fluid.mcc$blockFromConstructor) {
            if (block.mcc$modern != null) return BuiltInRegistries.BLOCK.getKey(block.mcc$modern).getNamespace();
        }
        return "legacyconnector";
    }

    public static FluidStack toModern(legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack stack) {
        if (stack == null || stack.amount <= 0 || stack.getFluid() == null) return FluidStack.EMPTY;
        Fluid fluid = MODERN.get(stack.getFluid().getName());
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, stack.amount);
    }

    public static legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack fromModern(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (var e : MODERN.entrySet()) {
            if (e.getValue() == stack.getFluid()) {
                var legacy = FluidRegistry.getFluid(e.getKey());
                return legacy == null ? null : new legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack(legacy, stack.getAmount());
            }
        }
        return null;
    }

    private static legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid legacyOf(Fluid modern) {
        for (var e : MODERN.entrySet()) if (e.getValue() == modern) return FluidRegistry.getFluid(e.getKey());
        return null;
    }

    // ------------------------------------------------------------------ capabilities

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        var type = TileEntityBridge.type();
        if (type != null) {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> {
                TileEntity legacy = be.legacy();
                if (!(legacy instanceof IFluidHandler handler)) return null;
                ForgeDirection from = side == null ? ForgeDirection.UNKNOWN : ForgeDirection.getOrientation(side.get3DDataValue());
                return new TileHandler(handler, from);
            });
        }
        for (legacyconnector.v1_7_10.net.minecraft.item.Item item : ItemBridge.modItems()) {
            if (!(item instanceof IFluidContainerItem container)) continue;
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new ItemHandler(container, stack),
                    item.mcc$variants.values().toArray(new net.minecraft.world.item.Item[0]));
        }
    }

    private record TileHandler(IFluidHandler tile, ForgeDirection from) implements net.neoforged.neoforge.fluids.capability.IFluidHandler {
        @Override
        public int getTanks() {
            FluidTankInfo[] info = tile.getTankInfo(from);
            return info == null ? 0 : info.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidTankInfo[] info = tile.getTankInfo(from);
            return info == null || tank >= info.length ? FluidStack.EMPTY : toModern(info[tank].fluid);
        }

        @Override
        public int getTankCapacity(int tank) {
            FluidTankInfo[] info = tile.getTankInfo(from);
            return info == null || tank >= info.length ? 0 : info[tank].capacity;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            var legacy = legacyOf(stack.getFluid());
            return legacy != null && tile.canFill(from, legacy);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            var legacy = fromModern(resource);
            if (legacy == null || !tile.canFill(from, legacy.getFluid())) return 0;
            return tile.fill(from, legacy, action.execute());
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            var legacy = fromModern(resource);
            if (legacy == null || !tile.canDrain(from, legacy.getFluid())) return FluidStack.EMPTY;
            return toModern(tile.drain(from, legacy, action.execute()));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidTankInfo[] info = tile.getTankInfo(from);
            if (info != null) {
                for (FluidTankInfo i : info) {
                    if (i.fluid != null && i.fluid.amount > 0 && tile.canDrain(from, i.fluid.getFluid())) {
                        return toModern(tile.drain(from, maxDrain, action.execute()));
                    }
                }
            }
            return FluidStack.EMPTY;
        }
    }

    private record ItemHandler(IFluidContainerItem item, ItemStack stack) implements IFluidHandlerItem {
        private legacyconnector.v1_7_10.net.minecraft.item.ItemStack legacy() {
            return legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack);
        }

        @Override
        public ItemStack getContainer() {
            return stack;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return toModern(item.getFluid(legacy()));
        }

        @Override
        public int getTankCapacity(int tank) {
            return item.getCapacity(legacy());
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack s) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            var legacy = fromModern(resource);
            if (legacy == null) return 0;
            var l = legacy();
            int filled = item.fill(l, legacy, action.execute());
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack current = getFluidInTank(0);
            if (current.isEmpty() || !FluidStack.isSameFluidSameComponents(current, resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            var l = legacy();
            var drained = item.drain(l, maxDrain, action.execute());
            return toModern(drained);
        }
    }

    // ------------------------------------------------------------------ the modern fluid objects

    private static final class LegacyFluidType extends FluidType {
        private final String name;

        LegacyFluidType(String name, legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid legacy) {
            super(FluidType.Properties.create().descriptionId("fluid." + name.replace(':', '.'))
                    .density(legacy.getDensity()).temperature(legacy.getTemperature()).viscosity(legacy.getViscosity())
                    .lightLevel(legacy.getLuminosity()).canSwim(false).canDrown(false).supportsBoating(false));
            this.name = name;
        }

        @Override
        public net.minecraft.network.chat.Component getDescription(FluidStack stack) {
            var legacy = FluidRegistry.getFluid(name);
            String text = legacy == null ? name : legacy.getLocalizedName();
            return net.minecraft.network.chat.Component.literal(text);
        }

        @Override
        public net.minecraft.network.chat.Component getDescription() {
            return getDescription(FluidStack.EMPTY);
        }

        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                private ResourceLocation texture(boolean still) {
                    var legacy = FluidRegistry.getFluid(name);
                    var icon = legacy == null ? null : still ? legacy.getStillIcon() : legacy.getFlowingIcon();
                    if (icon == null && legacy != null) icon = legacy.getStillIcon();
                    String n = icon == null ? "minecraft:missingno" : icon.getIconName();
                    return ResourceLocation.parse(org.kylepitcock.legacyconnector.v1_7_10.Assets1710.texture(n, true));
                }

                @Override
                public ResourceLocation getStillTexture() {
                    return texture(true);
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return texture(false);
                }
            });
        }
    }

    /** A source-only fluid that exists for stacks and tanks; the world holds the legacy fluid block instead. */
    private static final class LegacyModernFluid extends Fluid {
        private final FluidType type;

        LegacyModernFluid(FluidType type) {
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
