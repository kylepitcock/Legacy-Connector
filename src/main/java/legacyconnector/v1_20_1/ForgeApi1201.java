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

    /** Forge's {@code Event.isCancelable/setCanceled}: in NeoForge only events implementing {@code ICancellableEvent} cancel. */
    public static boolean eventIsCancelable(net.neoforged.bus.api.Event event) {
        return event instanceof net.neoforged.bus.api.ICancellableEvent;
    }

    public static void eventSetCanceled(net.neoforged.bus.api.Event event, boolean canceled) {
        if (event instanceof net.neoforged.bus.api.ICancellableEvent c) c.setCanceled(canceled);
        else if (canceled) throw new UnsupportedOperationException("Attempted to cancel a non-cancellable event: " + event.getClass().getName());
    }

    /** {@code Event.hasResult()}: results are gone from the NeoForge event bus, no event has one. */
    public static boolean eventHasResult(net.neoforged.bus.api.Event event) {
        return false;
    }

    public static void eventSetResult(net.neoforged.bus.api.Event event, legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result result) {
        // no NeoForge 1.21.1 event reads it
    }

    /** {@code RegisterHelper.register(String, value)}: the name is in the namespace of the mod being loaded. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerHelper(RegisterEvent.RegisterHelper helper, String name, Object value) {
        ResourceLocation id = name.contains(":") ? ResourceLocation.parse(name) : ResourceLocation.fromNamespaceAndPath(net.neoforged.fml.ModLoadingContext.get().getActiveNamespace(), name);
        helper.register(id, value);
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
