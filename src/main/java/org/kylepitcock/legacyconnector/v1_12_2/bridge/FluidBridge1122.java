package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.BlockFluidBase;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.BlockFluidFinite;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BucketItem;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122FluidBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge 1.12.2 fluids -> NeoForge fluids, the way a 1.21.1 mod adds a fluid:
 * <ul>
 *     <li>every legacy fluid gets a {@link FluidType} (density, viscosity, temperature, luminosity, gaseousness, sounds, name, and on the client
 *     its still/flowing textures and colour: {@code client.Fluids1122});</li>
 *     <li>a fluid whose block is a fluid block ({@code BlockFluidBase} or a mod's {@code BlockLiquid}) gets a source and a flowing
 *     {@link BaseFlowingFluid}, and its block ({@link Legacy1122FluidBlock}) holds their states, so rendering, swimming, pushing and buckets go
 *     through vanilla code; the legacy block keeps flowing by its own ticks (the modern fluid never ticks), so mods' flow rules, displacement
 *     and {@code onEntityCollision} effects still run. Fluids without a block get a source-only fluid for tanks and pipes;</li>
 *     <li>a fluid with a block or a universal bucket ({@code FluidRegistry.addBucketForFluid}) gets a bucket: the mod's own filled bucket
 *     ({@code new ItemBucket(fluidBlock)}) when it has one, else a modern {@link Legacy1122BucketItem} ({@code <namespace>:<fluid>_bucket}).
 *     Legacy code sees that bucket as Forge's universal bucket with the fluid in its tag ({@link #legacyFilledBucket}).</li>
 * </ul>
 * Tiles and items that answer {@code FLUID_HANDLER_CAPABILITY} work with modern pipes, tanks and Create through {@link #forTile} and
 * {@link #forStack}.
 */
public final class FluidBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Fluids1122");
    /** Legacy fluid name -> its modern (source) fluid. */
    private static final Map<String, Fluid> MODERN = new HashMap<>();
    /** Modern fluid (source and flowing) -> legacy fluid name. */
    private static final Map<Fluid, String> NAMES = new IdentityHashMap<>();
    /** Legacy fluid name -> its bucket item (generated or the mod's own). */
    private static final Map<String, net.minecraft.world.item.Item> BUCKETS = new HashMap<>();
    /** Bucket item -> legacy fluid name (generated buckets and the mods' own). */
    private static final Map<net.minecraft.world.item.Item, String> BUCKET_FLUIDS = new IdentityHashMap<>();
    /** The generated buckets, in registration order. */
    private static final Map<Legacy1122BucketItem, String> GENERATED = new LinkedHashMap<>();
    /** Every fluid type made here, with its legacy fluid name. */
    private static final Map<FluidType, String> TYPES = new LinkedHashMap<>();
    /** Fluids whose block holds their states: legacy name -> {source, flowing}. */
    private static final Map<String, Fluid[]> FLOWING = new LinkedHashMap<>();
    private static volatile boolean registered;

    private FluidBridge1122() {}

    /** Registers a modern fluid for every 1.12.2 fluid (call while registries are open, after the mods registered theirs). */
    public static synchronized void register() {
        int flowing = 0, buckets = 0;
        for (var e : FluidRegistry.getRegisteredFluids().entrySet()) {
            String name = e.getKey();
            if (MODERN.containsKey(name)) continue;
            var legacy = e.getValue();
            Fluid modern;
            if (name.equals("water")) modern = net.minecraft.world.level.material.Fluids.WATER;
            else if (name.equals("lava")) modern = net.minecraft.world.level.material.Fluids.LAVA;
            else {
                try {
                    String namespace = "legacyconnector";
                    var block = legacy.getBlock();
                    if (block != null && block.getRegistryName() != null) namespace = Ids.modernModId(block.getRegistryName().getNamespace());
                    String path = Ids.normalizePath(name).replace('/', '_');
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
                    if (BuiltInRegistries.FLUID.containsKey(id)) id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "f1122_" + id.getNamespace() + "_" + id.getPath());
                    LegacyFluidType type = new LegacyFluidType(name, legacy);
                    LegacyRegistrar.register(net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES, id, type);
                    TYPES.put(type, name);
                    var modernBlock = block == null ? null : BlockBridge1122.modern(block);
                    if (modernBlock instanceof Legacy1122FluidBlock fluidBlock && fluidBlock.mcc$fluid() == null) {
                        Binding binding = new Binding(fluidBlock);
                        BaseFlowingFluid.Properties props = new BaseFlowingFluid.Properties(() -> type, () -> binding.source, () -> binding.flowing)
                                .bucket(() -> bucketOrAir(name));
                        binding.source = new LegacySource(props, binding);
                        binding.flowing = new LegacyFlowing(props, binding);
                        LegacyRegistrar.register(BuiltInRegistries.FLUID, id, binding.source);
                        ResourceLocation flowingId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "flowing_" + id.getPath());
                        if (BuiltInRegistries.FLUID.containsKey(flowingId)) flowingId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_flowing");
                        LegacyRegistrar.register(BuiltInRegistries.FLUID, flowingId, binding.flowing);
                        binding.bind(legacy);
                        NAMES.put(binding.flowing, name);
                        FLOWING.put(name, new Fluid[] {binding.source, binding.flowing});
                        modern = binding.source;
                        flowing++;
                    } else {
                        modern = new SourceFluid(type, name);
                        LegacyRegistrar.register(BuiltInRegistries.FLUID, id, modern);
                    }
                    MODERN.put(name, modern);
                    NAMES.put(modern, name);
                    if (registerBucket(name, legacy, modern, id)) buckets++;
                } catch (RuntimeException | LinkageError ex) {
                    LOG.warn("Could not register the modern fluid of {}", name, ex);
                    continue;
                }
            }
            MODERN.put(name, modern);
            NAMES.put(modern, name);
        }
        registered = true;
        LOG.info("Registered {} 1.12.2 fluid(s) as modern fluids ({} placed in the world as fluids, {} with buckets)", MODERN.size(), flowing, buckets);
    }

    private static boolean registerBucket(String name, legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid legacy, Fluid modern, ResourceLocation fluidId) {
        if (legacy.getBlock() == null && !FluidRegistry.hasBucket(legacy)) return false;
        var own = ownBucket(legacy);
        net.minecraft.world.item.Item ownModern = own == null ? null : ItemBridge1122.modern(own, 0);
        if (ownModern != null) {
            BUCKETS.put(name, ownModern);
            BUCKET_FLUIDS.put(ownModern, name);
            return true;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fluidId.getNamespace(), fluidId.getPath() + "_bucket");
        if (BuiltInRegistries.ITEM.containsKey(id)) id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "b1122_" + fluidId.getNamespace() + "_" + fluidId.getPath() + "_bucket");
        var bucket = new Legacy1122BucketItem(modern, new net.minecraft.world.item.Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));
        LegacyRegistrar.register(BuiltInRegistries.ITEM, id, bucket);
        BUCKETS.put(name, bucket);
        BUCKET_FLUIDS.put(bucket, name);
        GENERATED.put(bucket, name);
        return true;
    }

    private static net.minecraft.world.item.Item bucketOrAir(String name) {
        var item = BUCKETS.get(name);
        return item == null ? Items.AIR : item;
    }

    // ------------------------------------------------------------------ fluid and stack conversion

    public static synchronized FluidStack toModern(legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack stack) {
        if (stack == null || stack.amount <= 0 || stack.getFluid() == null) return FluidStack.EMPTY;
        Fluid fluid = MODERN.get(stack.getFluid().getName());
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, stack.amount);
    }

    public static synchronized legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack fromModern(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var legacy = legacyOf(stack.getFluid());
        return legacy == null ? null : new legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack(legacy, stack.getAmount());
    }

    /** The legacy fluid of a modern one (its source or flowing form), or null. */
    public static synchronized legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid legacyOf(Fluid modern) {
        String name = modern == null ? null : NAMES.get(modern);
        return name == null ? null : FluidRegistry.getFluid(name);
    }

    /** The modern source fluid of a legacy one, or null. */
    public static synchronized Fluid modernOf(legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid legacy) {
        return legacy == null ? null : MODERN.get(legacy.getName());
    }

    // ------------------------------------------------------------------ buckets

    /** A mod's own filled bucket of the fluid ({@code new ItemBucket(fluidBlock)}), or null. Cached once the fluids are registered. */
    private static final Map<String, Optional<legacyconnector.v1_12_2.net.minecraft.item.Item>> OWN = new HashMap<>();

    public static synchronized legacyconnector.v1_12_2.net.minecraft.item.Item ownBucket(legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid fluid) {
        if (fluid == null || fluid.getBlock() == null) return null;
        var cached = OWN.get(fluid.getName());
        if (cached != null) return cached.orElse(null);
        legacyconnector.v1_12_2.net.minecraft.item.Item found = null;
        var block = fluid.getBlock();
        try {
            for (var item : Registries1122.ITEMS.getValues()) {
                if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBucket bucket && bucket.mcc$containedBlock() == block) {
                    found = item;
                    break;
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // no own bucket
        }
        if (registered || found != null) OWN.put(fluid.getName(), Optional.ofNullable(found));
        return found;
    }

    /** The modern filled bucket of a legacy fluid (vanilla's for water and lava), or empty when it has none. */
    public static net.minecraft.world.item.ItemStack modernBucket(legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid fluid) {
        if (fluid == null) return net.minecraft.world.item.ItemStack.EMPTY;
        if (fluid == FluidRegistry.WATER || "water".equals(fluid.getName())) return new net.minecraft.world.item.ItemStack(Items.WATER_BUCKET);
        if (fluid == FluidRegistry.LAVA || "lava".equals(fluid.getName())) return new net.minecraft.world.item.ItemStack(Items.LAVA_BUCKET);
        net.minecraft.world.item.Item item;
        synchronized (FluidBridge1122.class) {
            item = BUCKETS.get(fluid.getName());
        }
        return item == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(item);
    }

    /**
     * The legacy filled bucket of a fluid for {@code FluidUtil.getFilledBucket} (it may run in preInit, before the modern buckets exist): the
     * mod's own bucket item, else Forge's universal bucket with the fluid in its tag (Forge's own format), which turns into the modern bucket
     * when the stack is converted. Empty for a fluid that gets no bucket (no block and no universal bucket).
     */
    public static legacyconnector.v1_12_2.net.minecraft.item.ItemStack legacyFilledBucket(legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid fluid) {
        if (fluid == null) return legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY;
        var own = ownBucket(fluid);
        if (own != null) return new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(own);
        if (fluid.getBlock() == null && !FluidRegistry.hasBucket(fluid)) return legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY;
        return universalStack(fluid, 1);
    }

    private static legacyconnector.v1_12_2.net.minecraft.item.ItemStack universalStack(legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid fluid, int count) {
        var universal = legacyconnector.v1_12_2.net.minecraftforge.common.ForgeModContainer.mcc$get$universalBucket(null);
        var stack = new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(universal, count);
        var tag = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound();
        new legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack(fluid, legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid.BUCKET_VOLUME).writeToNBT(tag);
        stack.setTagCompound(tag);
        return stack;
    }

    /** A stack of Forge's (unregistered) universal bucket as the modern bucket of the fluid in its tag (the empty bucket without one). */
    public static net.minecraft.world.item.ItemStack modernUniversalBucket(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        var contained = legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack.loadFluidStackFromNBT(stack.getTagCompound());
        if (contained == null || contained.getFluid() == null) return new net.minecraft.world.item.ItemStack(Items.BUCKET, stack.getCount());
        var bucket = modernBucket(contained.getFluid());
        if (!bucket.isEmpty()) bucket.setCount(stack.getCount());
        return bucket;
    }

    /** A generated modern bucket as the legacy universal bucket stack (null for any other item). */
    public static legacyconnector.v1_12_2.net.minecraft.item.ItemStack legacyBucketStack(net.minecraft.world.item.ItemStack stack) {
        if (!(stack.getItem() instanceof Legacy1122BucketItem bucket)) return null;
        String name;
        synchronized (FluidBridge1122.class) {
            name = GENERATED.get(bucket);
        }
        var fluid = name == null ? null : FluidRegistry.getFluid(name);
        return fluid == null ? null : universalStack(fluid, stack.getCount());
    }

    /** The legacy fluid a bucket item holds (a generated bucket or a mod's own mapped one), or null. */
    public static synchronized legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid fluidOfBucket(net.minecraft.world.item.Item item) {
        String name = item == null ? null : BUCKET_FLUIDS.get(item);
        return name == null ? null : FluidRegistry.getFluid(name);
    }

    /** The generated buckets and their modern source fluids (item models and colours, creative tab). */
    public static synchronized Map<net.minecraft.world.item.Item, Fluid> generatedBuckets() {
        Map<net.minecraft.world.item.Item, Fluid> out = new LinkedHashMap<>();
        GENERATED.forEach((item, name) -> out.put(item, item.content));
        return out;
    }

    /** Every fluid type made for a 1.12.2 fluid, with its legacy fluid (client extensions). */
    public static synchronized Map<FluidType, legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid> fluidTypes() {
        Map<FluidType, legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid> out = new LinkedHashMap<>();
        TYPES.forEach((type, name) -> {
            var legacy = FluidRegistry.getFluid(name);
            if (legacy != null) out.put(type, legacy);
        });
        return out;
    }

    /** The source and flowing fluids of fluids placed in the world, with the legacy fluid (client render layers). */
    public static synchronized List<Object[]> placedFluids() {
        List<Object[]> out = new ArrayList<>();
        FLOWING.forEach((name, pair) -> out.add(new Object[] {FluidRegistry.getFluid(name), pair[0], pair[1]}));
        return Collections.unmodifiableList(out);
    }

    // ------------------------------------------------------------------ capabilities

    /**
     * Fluid handlers of the buckets: a generated bucket is a {@link net.minecraft.world.item.BucketItem} (NeoForge's own bucket wrapper);
     * a mod's own bucket answers through its legacy capability when it has one, else holds a bucket of its fluid.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        List<net.minecraft.world.item.Item> generated;
        Map<net.minecraft.world.item.Item, String> own = new IdentityHashMap<>();
        synchronized (FluidBridge1122.class) {
            generated = new ArrayList<>(GENERATED.keySet());
            BUCKET_FLUIDS.forEach((item, name) -> {
                if (!(item instanceof Legacy1122BucketItem)) own.put(item, name);
            });
        }
        if (!generated.isEmpty()) {
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper(stack),
                    generated.toArray(new net.minecraft.world.item.Item[0]));
        }
        own.forEach((item, name) -> event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new OwnBucket(stack, name), item));
    }

    /** A mod's own filled bucket without a legacy fluid capability: a bucket of its fluid, emptied into the vanilla bucket. */
    private static final class OwnBucket implements net.neoforged.neoforge.fluids.capability.IFluidHandlerItem {
        private net.minecraft.world.item.ItemStack container;
        private final String name;

        OwnBucket(net.minecraft.world.item.ItemStack container, String name) {
            this.container = container;
            this.name = name;
        }

        private FluidStack contents() {
            if (container.getCount() != 1 || BUCKET_FLUIDS.get(container.getItem()) == null) return FluidStack.EMPTY;
            Fluid fluid = MODERN.get(name);
            return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, FluidType.BUCKET_VOLUME);
        }

        @Override
        public net.minecraft.world.item.ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return contents();
        }

        @Override
        public int getTankCapacity(int tank) {
            return FluidType.BUCKET_VOLUME;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack contents = contents();
            if (contents.isEmpty() || resource.getAmount() < FluidType.BUCKET_VOLUME || !FluidStack.isSameFluid(contents, resource)) return FluidStack.EMPTY;
            return drain(FluidType.BUCKET_VOLUME, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack contents = contents();
            if (contents.isEmpty() || maxDrain < FluidType.BUCKET_VOLUME) return FluidStack.EMPTY;
            if (action.execute()) container = new net.minecraft.world.item.ItemStack(Items.BUCKET);
            return contents;
        }
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

    // ------------------------------------------------------------------ the modern fluid objects

    /** The fluid type of a legacy fluid: its physical properties, name and sounds. */
    private static final class LegacyFluidType extends FluidType {
        private final String name;

        LegacyFluidType(String name, legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid legacy) {
            super(properties(legacy));
            this.name = name;
        }

        private static Properties properties(legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid legacy) {
            boolean gas = legacy.isGaseous();
            int viscosity = Math.max(1, legacy.getViscosity());
            String key = legacy.getUnlocalizedName();
            return Properties.create().descriptionId(key == null ? "fluid." + legacy.getName() : key)
                    .density(legacy.getDensity()).temperature(legacy.getTemperature()).viscosity(viscosity)
                    .lightLevel(Math.max(0, Math.min(15, legacy.getLuminosity())))
                    // water moves entities at 0.014 with viscosity 1000; lava (6000) at a sixth of it
                    .motionScale(0.014D * Math.min(1.0D, Math.max(0.1D, 1000.0D / viscosity)))
                    .canSwim(!gas).canDrown(!gas).canPushEntity(!gas).fallDistanceModifier(gas ? 1.0F : 0.0F)
                    .canExtinguish(!gas && legacy.getTemperature() < 373).canHydrate(false).canConvertToSource(false).supportsBoating(false);
        }

        private legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid legacy() {
            return FluidRegistry.getFluid(name);
        }

        @Override
        public net.minecraft.network.chat.Component getDescription(FluidStack stack) {
            var legacy = legacy();
            if (legacy == null) return net.minecraft.network.chat.Component.literal(name);
            try {
                return net.minecraft.network.chat.Component.literal(legacy.getLocalizedName(
                        new legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack(legacy, Math.max(1, stack.getAmount()))));
            } catch (RuntimeException | LinkageError e) {
                return net.minecraft.network.chat.Component.literal(name);
            }
        }

        @Override
        public net.minecraft.network.chat.Component getDescription() {
            return getDescription(new FluidStack(MODERN.getOrDefault(name, net.minecraft.world.level.material.Fluids.WATER), 1000));
        }

        /** The legacy fluid's fill and empty sounds (vanilla's bucket sounds by default; lava's for hot fluids). */
        @Override
        public SoundEvent getSound(SoundAction action) {
            var legacy = legacy();
            try {
                if (legacy != null && action == SoundActions.BUCKET_FILL && legacy.getFillSound() != null) return legacy.getFillSound().mcc$event();
                if (legacy != null && action == SoundActions.BUCKET_EMPTY && legacy.getEmptySound() != null) return legacy.getEmptySound().mcc$event();
            } catch (RuntimeException | LinkageError e) {
                // the default sound
            }
            return super.getSound(action);
        }

        /** Forge's {@code Fluid.doesVaporize}: placed where water evaporates (the nether), the fluid vaporizes when it says so. */
        @Override
        public boolean isVaporizedOnPlacement(Level level, BlockPos pos, FluidStack stack) {
            var legacy = legacy();
            if (legacy == null || !level.dimensionType().ultraWarm()) return false;
            try {
                return legacy.doesVaporize(new legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack(legacy, Math.max(1, stack.getAmount())));
            } catch (RuntimeException | LinkageError e) {
                return false;
            }
        }
    }

    /**
     * The link between a legacy fluid block and its source/flowing fluids: the fluid state of each metadata value and the metadata of each
     * fluid state (for {@code createLegacyBlock}: a bucket placing the source places the legacy block at level 0).
     */
    private static final class Binding {
        final Legacy1122FluidBlock block;
        BaseFlowingFluid source;
        BaseFlowingFluid flowing;
        final FluidState[] byMeta = new FluidState[16];
        int sourceMeta;

        Binding(Legacy1122FluidBlock block) {
            this.block = block;
        }

        void bind(legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid fluid) {
            var legacy = block.legacy();
            int[] light = new int[16];
            int quanta = legacy instanceof BlockFluidBase base ? Math.max(1, base.getQuantaPerBlock()) : 8;
            int maxLight = Math.max(0, Math.min(15, fluid.getLuminosity()));
            sourceMeta = -1;
            for (int meta = 0; meta < 16; meta++) {
                int level = level(legacy, meta);
                FluidState state;
                int remaining;
                if (legacy instanceof BlockFluidFinite) {
                    remaining = level + 1;
                    state = remaining >= quanta ? source.getSource(false) : flowing.getFlowing(amount(remaining, quanta), false);
                } else if (legacy instanceof BlockFluidBase) {
                    remaining = Math.max(1, quanta - level);
                    state = level == 0 ? source.getSource(false) : flowing.getFlowing(amount(remaining, quanta), false);
                } else {
                    // a BlockLiquid: 0 source, 1-7 flowing, 8 and up falling
                    remaining = level == 0 ? 8 : level < 8 ? 8 - level : 8;
                    state = level == 0 ? source.getSource(false) : level < 8 ? flowing.getFlowing(8 - level, false) : flowing.getFlowing(8, true);
                }
                if (state.isSource() && sourceMeta < 0) sourceMeta = meta;
                byMeta[meta] = state;
                // Forge's BlockFluidBase.getLightValue: the luminosity scaled by the quanta of the block
                light[meta] = maxLight == 0 ? 0 : Math.max(0, Math.min(15, (int) ((remaining - 1) / (float) quanta * maxLight)));
            }
            if (sourceMeta < 0) sourceMeta = 0;
            block.mcc$bind(fluid, byMeta, light);
        }

        private static int amount(int remaining, int quanta) {
            return Math.max(1, Math.min(8, Math.round(8.0F * remaining / quanta)));
        }

        private static int level(legacyconnector.v1_12_2.net.minecraft.block.Block legacy, int meta) {
            try {
                var state = legacy.getStateFromMeta(meta);
                if (state != null) {
                    var keys = state.getPropertyKeys();
                    if (keys.contains(BlockFluidBase.LEVEL)) return state.getValue(BlockFluidBase.LEVEL);
                    if (keys.contains(legacyconnector.v1_12_2.net.minecraft.block.BlockLiquid.LEVEL)) {
                        return state.getValue(legacyconnector.v1_12_2.net.minecraft.block.BlockLiquid.LEVEL);
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                // the metadata is the level
            }
            return meta;
        }

        /** The legacy block state holding a fluid state: the source at its metadata, a flowing state at the metadata closest in amount. */
        BlockState legacyBlock(FluidState state) {
            int meta = sourceMeta;
            if (!state.isSource()) {
                int best = Integer.MAX_VALUE;
                int amount = state.getAmount();
                boolean falling = state.hasProperty(net.minecraft.world.level.material.FlowingFluid.FALLING)
                        && state.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING);
                for (int m = 0; m < 16; m++) {
                    FluidState s = byMeta[m];
                    if (s == null || s.isSource()) continue;
                    boolean f = s.hasProperty(net.minecraft.world.level.material.FlowingFluid.FALLING) && s.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING);
                    int score = Math.abs(s.getAmount() - amount) * 2 + (f == falling ? 0 : 1);
                    if (score < best) {
                        best = score;
                        meta = m;
                    }
                }
            }
            return block.defaultBlockState().setValue(Legacy1122Block.META, meta);
        }
    }

    /*
     * The source and flowing fluid of a legacy fluid block: the legacy block flows by its own ticks, so the modern fluid never ticks or
     * spreads, and no modern fluid replaces it (1.12's liquids did not flow into each other's blocks either).
     */
    private static final class LegacySource extends BaseFlowingFluid.Source {
        private final Binding binding;

        LegacySource(Properties properties, Binding binding) {
            super(properties);
            this.binding = binding;
        }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
            return false;
        }

        @Override
        protected BlockState createLegacyBlock(FluidState state) {
            return binding.legacyBlock(state);
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return 5;
        }

        @Override
        public Optional<SoundEvent> getPickupSound() {
            return Optional.ofNullable(getFluidType().getSound(SoundActions.BUCKET_FILL));
        }
    }

    private static final class LegacyFlowing extends BaseFlowingFluid.Flowing {
        private final Binding binding;

        LegacyFlowing(Properties properties, Binding binding) {
            super(properties);
            this.binding = binding;
        }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
            return false;
        }

        @Override
        protected BlockState createLegacyBlock(FluidState state) {
            return binding.legacyBlock(state);
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return 5;
        }

        @Override
        public Optional<SoundEvent> getPickupSound() {
            return Optional.ofNullable(getFluidType().getSound(SoundActions.BUCKET_FILL));
        }
    }

    /** A source-only fluid of a fluid with no fluid block: it exists for stacks, tanks and buckets, never in the world. */
    private static final class SourceFluid extends Fluid {
        private final FluidType type;
        private final String name;

        SourceFluid(FluidType type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public FluidType getFluidType() {
            return type;
        }

        @Override
        public net.minecraft.world.item.Item getBucket() {
            return bucketOrAir(name);
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
        public int getTickDelay(LevelReader level) {
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
