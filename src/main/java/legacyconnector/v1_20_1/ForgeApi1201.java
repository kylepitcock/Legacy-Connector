package legacyconnector.v1_20_1;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import legacyconnector.v1_20_1.net.minecraftforge.registries.ForgeRegistryWrapper;
import legacyconnector.v1_20_1.net.minecraftforge.registries.IForgeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Static replacements for Forge 1.20.1 methods that NeoForge 1.21.1's classes do not have. The translator
 * ({@code Forge1201ApiRewrites}) rewrites a call {@code recv.name(args)} to {@code ForgeApi1201.<helper>(recv, args)}; the table is
 * {@code forward/1.20.1/forge-call-rewrites.tsv}. Only Forge API goes here; Minecraft signature changes are in {@code Compat1201}.
 */
public final class ForgeApi1201 {
    private ForgeApi1201() {
    }

    // ---- mod loading ---------------------------------------------------------------------------------------------------------

    /** {@code ModLoadingContext.get().registerConfig(type, spec)}: NeoForge registers configs on the mod container. */
    public static void registerConfig(net.neoforged.fml.ModLoadingContext ctx, net.neoforged.fml.config.ModConfig.Type type, net.neoforged.fml.config.IConfigSpec spec) {
        ctx.getActiveContainer().registerConfig(type, spec);
    }

    public static void registerConfig(net.neoforged.fml.ModLoadingContext ctx, net.neoforged.fml.config.ModConfig.Type type, net.neoforged.fml.config.IConfigSpec spec, String fileName) {
        ctx.getActiveContainer().registerConfig(type, spec, fileName);
    }

    // ---- entities ------------------------------------------------------------------------------------------------------------

    /** {@code EntityType.Builder.setCustomClientFactory(..)}: vanilla spawning with the type's own factory replaces it. */
    public static <T extends net.minecraft.world.entity.Entity> net.minecraft.world.entity.EntityType.Builder<T> builderCustomClientFactory(
            net.minecraft.world.entity.EntityType.Builder<T> builder, java.util.function.BiFunction<?, ?, ?> factory) {
        return builder;
    }

    // ---- event bus ----------------------------------------------------------------------------------------------------------

    /** Forge accepted {@code bus.register(obj)} for an object without subscriber methods (a mod registering its main class "just in case"); NeoForge throws. */
    public static void busRegister(net.neoforged.bus.api.IEventBus bus, Object target) {
        try {
            bus.register(target);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() == null || !e.getMessage().contains("no @SubscribeEvent methods")) throw e;
        }
    }

    /**
     * {@code bus.addGenericListener(Entity.class, handler)}: Forge 1.20.1's generic events (the only one mods used is {@code AttachCapabilitiesEvent<T>}) are
     * delivered when the event's object is an instance of the filter class.
     */
    public static <T extends net.neoforged.bus.api.Event> void busAddGenericListener(net.neoforged.bus.api.IEventBus bus, Class<?> filter, Consumer<T> handler) {
        busAddGenericListener(bus, filter, net.neoforged.bus.api.EventPriority.NORMAL, false, null, handler);
    }

    public static <T extends net.neoforged.bus.api.Event> void busAddGenericListener(net.neoforged.bus.api.IEventBus bus, Class<?> filter,
            net.neoforged.bus.api.EventPriority priority, Consumer<T> handler) {
        busAddGenericListener(bus, filter, priority, false, null, handler);
    }

    public static <T extends net.neoforged.bus.api.Event> void busAddGenericListener(net.neoforged.bus.api.IEventBus bus, Class<?> filter,
            net.neoforged.bus.api.EventPriority priority, boolean receiveCancelled, Consumer<T> handler) {
        busAddGenericListener(bus, filter, priority, receiveCancelled, null, handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends net.neoforged.bus.api.Event> void busAddGenericListener(net.neoforged.bus.api.IEventBus bus, Class<?> filter,
            net.neoforged.bus.api.EventPriority priority, boolean receiveCancelled, Class<T> eventClass, Consumer<T> handler) {
        Class type = eventClass != null ? eventClass : legacyconnector.v1_20_1.net.minecraftforge.event.AttachCapabilitiesEvent.class;
        bus.addListener(priority, receiveCancelled, type, (Consumer) (Object event) -> {
            if (event instanceof legacyconnector.v1_20_1.net.minecraftforge.event.AttachCapabilitiesEvent<?> attach && !filter.isInstance(attach.getObject())) return;
            handler.accept((T) event);
        });
    }

    // ---- items --------------------------------------------------------------------------------------------------------------

    /** {@code item.initCapabilities(stack, nbt)} called from outside the item class. */
    public static legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(Object item,
            net.minecraft.world.item.ItemStack stack, net.minecraft.nbt.CompoundTag nbt) {
        return item instanceof Caps1201.InitCapabilities1201 init ? init.initCapabilities(stack, nbt) : null;
    }

    /** {@code super.initCapabilities(stack, nbt)} of an item whose parent is a Minecraft class: no capabilities. */
    public static legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.ICapabilityProvider superInitCapabilities(Object item,
            net.minecraft.world.item.ItemStack stack, net.minecraft.nbt.CompoundTag nbt) {
        return null;
    }

    // ---- events -------------------------------------------------------------------------------------------------------------

    public static void eventSetCanceled(Object event, boolean canceled) {
        if (event instanceof net.neoforged.bus.api.ICancellableEvent c) c.setCanceled(canceled);
    }

    public static boolean eventIsCanceled(Object event) {
        return event instanceof net.neoforged.bus.api.ICancellableEvent c && c.isCanceled();
    }

    private static final java.util.Map<Object, Object> RESULTS = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    /** Forge's {@code Event.setResult}: kept per event object; the events the layer posts itself read it, NeoForge's own events have their own result types. */
    public static void eventSetResult(Object event, legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result result) {
        RESULTS.put(event, result);
    }

    public static legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result eventGetResult(Object event) {
        Object r = RESULTS.get(event);
        return r == null ? legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.DEFAULT : (legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result) r;
    }

    public static boolean eventHasResult(Object event) {
        return true;
    }

    public static boolean eventIsCancelable(Object event) {
        return event instanceof net.neoforged.bus.api.ICancellableEvent;
    }

    // ---- registries ---------------------------------------------------------------------------------------------------------

    /** {@code DeferredRegister.create(ForgeRegistries.BLOCKS, modid)}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DeferredRegister createDeferred(IForgeRegistry registry, String modid) {
        // the key is enough for DeferredRegister (data driven registries have no object before a world loads)
        return DeferredRegister.create((net.minecraft.resources.ResourceKey) registry.getRegistryKey(), modid);
    }

    /** {@code RegistryObject.create(name, registry)}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DeferredHolder holderCreate(ResourceLocation name, IForgeRegistry registry) {
        return DeferredHolder.create((net.minecraft.resources.ResourceKey) registry.getRegistryKey(), name);
    }

    public static boolean holderIsPresent(DeferredHolder<?, ?> holder) {
        return holder.isBound();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void holderIfPresent(DeferredHolder holder, Consumer consumer) {
        if (holder.isBound()) consumer.accept(holder.get());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional holderGetHolder(DeferredHolder holder) {
        return holder.isBound() ? Optional.of(holder.getDelegate()) : Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional holderMap(DeferredHolder holder, Function mapper) {
        return holder.isBound() ? Optional.ofNullable(mapper.apply(holder.get())) : Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional holderFlatMap(DeferredHolder holder, Function mapper) {
        return holder.isBound() ? (Optional) mapper.apply(holder.get()) : Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional holderFilter(DeferredHolder holder, Predicate predicate) {
        return holder.isBound() && predicate.test(holder.get()) ? Optional.of(holder.get()) : Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object holderOrElse(DeferredHolder holder, Object other) {
        return holder.isBound() ? holder.get() : other;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object holderOrElseGet(DeferredHolder holder, Supplier other) {
        return holder.isBound() ? holder.get() : other.get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object holderOrElseThrow(DeferredHolder holder, Supplier exception) throws Throwable {
        if (holder.isBound()) return holder.get();
        throw (Throwable) exception.get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Stream holderStream(DeferredHolder holder) {
        return holder.isBound() ? Stream.of(holder.get()) : Stream.empty();
    }

    /** {@code RegisterEvent.getForgeRegistry()}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static IForgeRegistry registerEventForgeRegistry(RegisterEvent event) {
        Registry<?> registry = event.getRegistry();
        return registry == null ? null : wrap(registry);
    }

    /** A view of a vanilla registry as Forge's handle. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static IForgeRegistry wrap(Registry<?> registry) {
        return new ForgeRegistryWrapper((Registry) registry);
    }
}
