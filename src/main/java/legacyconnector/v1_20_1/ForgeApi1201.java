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

    // ---- listeners on the wrong bus: Forge accepted them and never delivered the event; NeoForge throws ----

    private static boolean wrongBus(IllegalArgumentException e) {
        String m = String.valueOf(e.getMessage());
        return m.contains("not valid for this bus") || m.contains("not allowed on the common");
    }

    private static void ignoredWrongBus(IllegalArgumentException e) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/Events").debug("Listener ignored (its event belongs to the other bus): {}", e.getMessage());
    }

    public static <T extends net.neoforged.bus.api.Event> void addListenerSafe(net.neoforged.bus.api.IEventBus bus, java.util.function.Consumer<T> listener) {
        try {
            bus.addListener(listener);
        } catch (IllegalArgumentException e) {
            if (!wrongBus(e)) throw e;
            ignoredWrongBus(e);
        }
    }

    public static <T extends net.neoforged.bus.api.Event> void addListenerSafe(net.neoforged.bus.api.IEventBus bus, net.neoforged.bus.api.EventPriority priority,
            java.util.function.Consumer<T> listener) {
        try {
            bus.addListener(priority, listener);
        } catch (IllegalArgumentException e) {
            if (!wrongBus(e)) throw e;
            ignoredWrongBus(e);
        }
    }

    public static <T extends net.neoforged.bus.api.Event> void addListenerSafe(net.neoforged.bus.api.IEventBus bus, net.neoforged.bus.api.EventPriority priority,
            boolean receiveCanceled, java.util.function.Consumer<T> listener) {
        try {
            bus.addListener(priority, receiveCanceled, listener);
        } catch (IllegalArgumentException e) {
            if (!wrongBus(e)) throw e;
            ignoredWrongBus(e);
        }
    }

    public static <T extends net.neoforged.bus.api.Event> void addListenerSafe(net.neoforged.bus.api.IEventBus bus, net.neoforged.bus.api.EventPriority priority,
            boolean receiveCanceled, Class<T> type, java.util.function.Consumer<T> listener) {
        try {
            bus.addListener(priority, receiveCanceled, type, listener);
        } catch (IllegalArgumentException e) {
            if (!wrongBus(e)) throw e;
            ignoredWrongBus(e);
        }
    }

    // ---- reflection on fields whose type changed ----

    /**
     * {@code Field.get} in mod code: the three lists of a {@code LootPool} (entries, conditions, functions) were arrays in 1.20.1 and mods add loot to a
     * table by reading them reflectively; a list is handed out as the array they cast it to.
     */
    public static Object fieldGet(java.lang.reflect.Field field, Object target) throws IllegalAccessException {
        Object value = field.get(target);
        if (value instanceof java.util.List<?> list && field.getDeclaringClass() == net.minecraft.world.level.storage.loot.LootPool.class) {
            return switch (field.getName()) {
                case "entries" -> list.toArray(new net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[0]);
                case "conditions" -> list.toArray(new net.minecraft.world.level.storage.loot.predicates.LootItemCondition[0]);
                case "functions" -> list.toArray(new net.minecraft.world.level.storage.loot.functions.LootItemFunction[0]);
                default -> value;
            };
        }
        return value;
    }

    /** {@code Field.set} in mod code: the array a mod built for a {@code LootPool} list goes back as a list. */
    public static void fieldSet(java.lang.reflect.Field field, Object target, Object value) throws IllegalAccessException {
        if (value instanceof Object[] array && field.getType() == java.util.List.class && field.getDeclaringClass() == net.minecraft.world.level.storage.loot.LootPool.class) {
            value = java.util.List.of(array);
        }
        field.set(target, value);
    }

    private static final java.util.Set<String> SKIPPED_REGISTERS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * {@code DeferredRegister.register(bus)} of a library that runs next to its native 1.21.1 build ("side-by-side"): both copies register the library's own
     * ids ({@code citadel:lectern}), which the registry refuses twice, and the native copy already provides them, so the isolated copy's register is not attached.
     */
    public static void skipIsolatedRegister(net.neoforged.neoforge.registries.DeferredRegister<?> register, net.neoforged.bus.api.IEventBus bus) {
        if (SKIPPED_REGISTERS.add(String.valueOf(register.getNamespace()) + "/" + register.getRegistryKey().location()))
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/SideBySide").info("Deferred register {} of an isolated library is not attached: the native build registers the same ids",
                    register.getRegistryKey().location());
    }

    /** Forge accepted {@code bus.register(obj)} for an object without subscriber methods (a mod registering its main class "just in case"); NeoForge throws. */
    public static void busRegister(net.neoforged.bus.api.IEventBus bus, Object target) {
        // a config event handler goes through the per-method wrapper (it tolerates the config read at the unload, see registerEachMethod)
        for (java.lang.reflect.Method m : target.getClass().getMethods()) {
            if (m.getParameterCount() == 1 && m.getAnnotation(net.neoforged.bus.api.SubscribeEvent.class) != null
                    && m.getParameterTypes()[0] == net.neoforged.fml.event.config.ModConfigEvent.class) {
                registerEachMethod(bus, target);
                return;
            }
        }
        try {
            bus.register(target);
        } catch (IllegalArgumentException e) {
            String message = e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
            if (message != null && message.contains("IModBusEvent events are not allowed")) {
                registerSplit(bus, target);
                return;
            }
            if (message != null && message.contains("Cannot register listeners for abstract class")) {
                registerEachMethod(bus, target);
                return;
            }
            // Forge also registered the @SubscribeEvent methods a superclass declares (Cyclops Core mods extend ModBase); NeoForge refuses the object
            if (message != null && message.contains("however its supertype")) {
                registerEachMethod(bus, target);
                return;
            }
            if (e.getMessage() == null || !e.getMessage().contains("no @SubscribeEvent methods")) throw e;
            if (e.getMessage() == null) throw e;
            if (e.getMessage().contains("no @SubscribeEvent methods")) return;
            // Forge ignored a subscriber method whose event belongs to the other bus (Citadel registers itself on the game bus with a ModConfigEvent
            // handler); NeoForge rejects the whole object. The other methods are registered one by one, the foreign one is dropped.
            if (!e.getMessage().contains("not valid for this bus")) throw e;
            registerEachMethod(bus, target);
        }
    }

    /**
     * Automatic subscription of a {@code @Mod.EventBusSubscriber} class whose hierarchy has instance {@code @SubscribeEvent} methods (Citadel's proxies):
     * Forge registered the class for its static methods and ignored the others; NeoForge rejects the class. The loader removes the annotation and has the
     * mod's main class constructor call this instead.
     *
     * @param owner a class of the mod (its class loader loads the subscriber)
     * @param bus {@code MOD} or {@code GAME}/{@code FORGE}
     * @param dists comma separated {@code CLIENT}/{@code DEDICATED_SERVER}, empty for both
     */
    public static void subscribeStatics(Class<?> owner, String bus, String dists, String className) {
        net.neoforged.api.distmarker.Dist dist = net.neoforged.fml.loading.FMLEnvironment.dist;
        if (!dists.isEmpty() && !java.util.Arrays.asList(dists.split(",")).contains(dist.name())) return;
        try {
            Class<?> type = Class.forName(className, true, owner.getClassLoader());
            net.neoforged.bus.api.IEventBus target = bus.equals("MOD") ? net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus() : net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
            registerEachMethod(target, type);
        } catch (ClassNotFoundException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Subscribers").warn("Subscriber class {} could not be loaded: {}", className, e.toString());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerEachMethod(net.neoforged.bus.api.IEventBus bus, Object target) {
        boolean isClass = target instanceof Class<?>;
        Class<?> type = isClass ? (Class<?>) target : target.getClass();
        java.util.List<java.lang.reflect.Method> all = new java.util.ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) all.addAll(java.util.Arrays.asList(c.getDeclaredMethods()));
        for (java.lang.reflect.Method m : all) {
            net.neoforged.bus.api.SubscribeEvent sub = m.getAnnotation(net.neoforged.bus.api.SubscribeEvent.class);
            if (sub == null || m.getParameterCount() != 1 || java.lang.reflect.Modifier.isStatic(m.getModifiers()) == isClass == false && isClass) continue;
            if (!net.neoforged.bus.api.Event.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
            Class<? extends net.neoforged.bus.api.Event> eventType = (Class<? extends net.neoforged.bus.api.Event>) m.getParameterTypes()[0];
            m.setAccessible(true);
            java.util.List<Class<? extends net.neoforged.bus.api.Event>> types = java.lang.reflect.Modifier.isAbstract(eventType.getModifiers())
                    ? concreteEvents(eventType) : java.util.List.of(eventType);
            for (Class<? extends net.neoforged.bus.api.Event> t : types) {
                try {
                    bus.addListener(sub.priority(), sub.receiveCanceled(), (Class) t, (java.util.function.Consumer) event -> {
                        try {
                            m.invoke(java.lang.reflect.Modifier.isStatic(m.getModifiers()) ? null : target, event);
                        } catch (java.lang.reflect.InvocationTargetException ex) {
                            // Forge 1.20.1 kept the spec's values readable after the unload (CoFH Core refreshes its settings from the Unloading event at server stop);
                            // NeoForge clears the loaded config first, so a ConfigValue read fails with "Cannot get config value before config is loaded"
                            if (event instanceof net.neoforged.fml.event.config.ModConfigEvent.Unloading && ex.getCause() instanceof IllegalStateException) return;
                            throw ex.getCause() instanceof RuntimeException r ? r : new RuntimeException(ex.getCause());
                        } catch (IllegalAccessException ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
                } catch (IllegalArgumentException ignored) {
                    // an event of the other bus: Forge never delivered it here either
                }
            }
        }
    }

    private static final java.util.Map<Class<?>, java.util.List<Class<? extends net.neoforged.bus.api.Event>>> CONCRETE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Forge let a handler listen to an abstract event class (every subclass is delivered: {@code ExplosionEvent}, {@code PlayerEvent}, {@code AdvancementEvent});
     * NeoForge refuses it. The concrete subclasses in the jar the class comes from are found once and the handler is registered for each.
     */
    @SuppressWarnings("unchecked")
    public static java.util.List<Class<? extends net.neoforged.bus.api.Event>> concreteEvents(Class<? extends net.neoforged.bus.api.Event> abstractType) {
        return CONCRETE.computeIfAbsent(abstractType, base -> {
            java.util.List<Class<? extends net.neoforged.bus.api.Event>> out = new java.util.ArrayList<>();
            try {
                java.net.URL where = base.getProtectionDomain().getCodeSource() == null ? null : base.getProtectionDomain().getCodeSource().getLocation();
                String pkg = base.getName().substring(0, base.getName().lastIndexOf('.', base.getName().indexOf('$') > 0 ? base.getName().indexOf('$') : base.getName().length())).replace('.', '/');
                if (where != null) {
                    java.nio.file.Path path = java.nio.file.Path.of(where.toURI());
                    try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(path.toFile())) {
                        var entries = zip.entries();
                        while (entries.hasMoreElements()) {
                            String n = entries.nextElement().getName();
                            if (!n.endsWith(".class") || !n.startsWith("net/neoforged/neoforge/event/") && !n.startsWith("net/neoforged/neoforge/client/event/")) continue;
                            String cn = n.substring(0, n.length() - 6).replace('/', '.');
                            try {
                                Class<?> c = Class.forName(cn, false, base.getClassLoader());
                                if (base.isAssignableFrom(c) && c != base && !java.lang.reflect.Modifier.isAbstract(c.getModifiers()) && !c.isInterface()) out.add((Class<? extends net.neoforged.bus.api.Event>) c);
                            } catch (Throwable ignored) {
                                // a client class on a server and the like: not an event this side can get
                            }
                        }
                    }
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Events").warn("Could not list the subclasses of {}: {}", base.getName(), e.toString());
            }
            return out;
        });
    }

    /**
     * Forge 1.20.1 let an object on the Forge bus hold handlers of mod bus events (Twigs' {@code BuildCreativeModeTabContentsEvent} listener); NeoForge
     * refuses the whole object. Each {@code @SubscribeEvent} method is registered on its own bus instead: mod bus events on the mod's bus, the rest on {@code bus}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerSplit(net.neoforged.bus.api.IEventBus bus, Object target) {
        net.neoforged.bus.api.IEventBus modBus = net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus();
        boolean isClass = target instanceof Class<?>;
        Class<?> type = isClass ? (Class<?>) target : target.getClass();
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
            net.neoforged.bus.api.SubscribeEvent sub = method.getAnnotation(net.neoforged.bus.api.SubscribeEvent.class);
            if (sub == null || method.getParameterCount() != 1 || !net.neoforged.bus.api.Event.class.isAssignableFrom(method.getParameterTypes()[0])) continue;
            boolean isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());
            if (!isStatic && isClass) continue;
            Class<? extends net.neoforged.bus.api.Event> eventType = (Class<? extends net.neoforged.bus.api.Event>) method.getParameterTypes()[0];
            method.setAccessible(true);
            net.neoforged.bus.api.IEventBus where = net.neoforged.fml.event.IModBusEvent.class.isAssignableFrom(eventType) && modBus != null ? modBus : bus;
            where.addListener(sub.priority(), sub.receiveCanceled(), (Class) eventType, (java.util.function.Consumer) event -> {
                try {
                    method.invoke(isStatic ? null : target, event);
                } catch (java.lang.reflect.InvocationTargetException ex) {
                    throw new RuntimeException(ex.getCause());
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
    }

    /**
     * {@code DeferredRegister.register(name, supplier)}: a value that is a {@code Codec} made by {@code RecordCodecBuilder.create} is registered as the
     * {@code MapCodec} inside it (registries of codecs such as the global loot modifier serializers hold map codecs in 1.21.1).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DeferredHolder drRegister(DeferredRegister register, String name, Supplier supplier) {
        return register.register(name, () -> {
            Object value = supplier.get();
            return value instanceof com.mojang.serialization.MapCodec.MapCodecCodec<?> m ? m.codec() : value;
        });
    }

    /** {@code IConfigSpec.self()}: the spec itself (Forge's generic self-type accessor). */
    public static net.neoforged.fml.config.IConfigSpec configSelf(net.neoforged.fml.config.IConfigSpec spec) {
        return spec;
    }

    /** {@code RegisterHelper.register(String, value)}: the name is in the namespace of the mod being loaded. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerHelper(RegisterEvent.RegisterHelper helper, String name, Object value) {
        ResourceLocation id = name.contains(":") ? ResourceLocation.parse(name) : ResourceLocation.fromNamespaceAndPath(net.neoforged.fml.ModLoadingContext.get().getActiveNamespace(), name);
        helper.register(id, value);
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

    /** {@code DeferredRegister.makeRegistry(() -> new RegistryBuilder<T>()...)}: NeoForge's builder is filled from the Forge one when the registry is created. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Supplier makeRegistry(DeferredRegister register, Supplier builder) {
        net.minecraft.core.Registry reg = register.makeRegistry(b -> {
            legacyconnector.v1_20_1.net.minecraftforge.registries.RegistryBuilder fb = (legacyconnector.v1_20_1.net.minecraftforge.registries.RegistryBuilder) builder.get();
            net.neoforged.neoforge.registries.RegistryBuilder nb = fb.mcc$toNeo(register.getRegistryKey());
            copy(nb, (net.neoforged.neoforge.registries.RegistryBuilder) b);
        });
        return () -> new ForgeRegistryWrapper(reg);
    }

    /** Copies the settings of one NeoForge builder onto the one NeoForge supplied (plain value fields; the key stays). */
    private static void copy(net.neoforged.neoforge.registries.RegistryBuilder from, net.neoforged.neoforge.registries.RegistryBuilder to) {
        for (var f : net.neoforged.neoforge.registries.RegistryBuilder.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || java.lang.reflect.Modifier.isFinal(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                f.set(to, f.get(from));
            } catch (ReflectiveOperationException | RuntimeException e) {
                // an unreadable field keeps NeoForge's value
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object readRegistryIdUnsafe(net.minecraft.network.FriendlyByteBuf buf, IForgeRegistry registry) {
        return registry.mcc$registry().byId(buf.readVarInt());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void writeRegistryIdUnsafe(net.minecraft.network.FriendlyByteBuf buf, IForgeRegistry registry, Object value) {
        buf.writeVarInt(registry.mcc$registry().getId(value));
    }

    public static Object readRegistryId(net.minecraft.network.FriendlyByteBuf buf, IForgeRegistry registry) {
        return readRegistryIdUnsafe(buf, registry);
    }

    public static void writeRegistryId(net.minecraft.network.FriendlyByteBuf buf, IForgeRegistry registry, Object value) {
        writeRegistryIdUnsafe(buf, registry, value);
    }

    /** {@code ForgeSpawnEggItem.fromEntityType(type)}. */
    public static net.minecraft.world.item.SpawnEggItem spawnEggFromType(net.minecraft.world.entity.EntityType<?> type) {
        return net.minecraft.world.item.SpawnEggItem.byId(type);
    }

    /** {@code AnvilUpdateEvent.setCost(int)} / {@code getCost()}: the cost is a long now. */
    public static void anvilSetCost(net.neoforged.neoforge.event.AnvilUpdateEvent event, int cost) {
        event.setCost(cost);
    }

    public static int anvilGetCost(net.neoforged.neoforge.event.AnvilUpdateEvent event) {
        return (int) event.getCost();
    }

    private static legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result fromTri(net.neoforged.neoforge.common.util.TriState s) {
        return s == net.neoforged.neoforge.common.util.TriState.TRUE ? legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.ALLOW
                : s == net.neoforged.neoforge.common.util.TriState.FALSE ? legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.DENY
                : legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.DEFAULT;
    }

    private static net.neoforged.neoforge.common.util.TriState toTri(legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result r) {
        return r == legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.ALLOW ? net.neoforged.neoforge.common.util.TriState.TRUE
                : r == legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.DENY ? net.neoforged.neoforge.common.util.TriState.FALSE
                : net.neoforged.neoforge.common.util.TriState.DEFAULT;
    }

    public static legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result rightClickGetUseBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        return fromTri(e.getUseBlock());
    }

    public static legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result rightClickGetUseItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        return fromTri(e.getUseItem());
    }

    public static void rightClickSetUseBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e, legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result r) {
        e.setUseBlock(toTri(r));
    }

    public static void rightClickSetUseItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e, legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result r) {
        e.setUseItem(toTri(r));
    }

    /** {@code NewRegistryEvent.create(forgeBuilder)}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Supplier newRegistryCreate(net.neoforged.neoforge.registries.NewRegistryEvent event, legacyconnector.v1_20_1.net.minecraftforge.registries.RegistryBuilder builder) {
        net.minecraft.core.Registry reg = event.create(builder.mcc$toNeo(net.minecraft.resources.ResourceKey.createRegistryKey(builder.mcc$name())));
        return () -> new ForgeRegistryWrapper(reg);
    }

    /** A view of a vanilla registry as Forge's handle. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static IForgeRegistry wrap(Registry<?> registry) {
        return new ForgeRegistryWrapper((Registry) registry);
    }

    // ---- registry-aware saving ----------------------------------------------------------------------------------------------

    /** The registries NBT with registry-dependent contents (item stacks, fluid stacks) is saved with: the running server's, else the client level's. */
    public static net.minecraft.core.HolderLookup.Provider registries() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            net.minecraft.core.HolderLookup.Provider client = ForgeApiClient1201.registries();
            if (client != null) return client;
        }
        return net.minecraft.core.RegistryAccess.EMPTY;
    }

    public static net.minecraft.nbt.CompoundTag itemHandlerSerialize(net.neoforged.neoforge.items.ItemStackHandler handler) {
        return handler.serializeNBT(registries());
    }

    /** {@code super.serializeNBT()} of a mod's ItemStackHandler subclass: the handler's own (registry-aware) method. */
    public static net.minecraft.nbt.CompoundTag superItemHandlerSerialize(net.neoforged.neoforge.items.ItemStackHandler handler) {
        return itemHandlerSerialize(handler);
    }

    public static void superItemHandlerDeserialize(net.neoforged.neoforge.items.ItemStackHandler handler, net.minecraft.nbt.CompoundTag tag) {
        itemHandlerDeserialize(handler, tag);
    }

    public static void itemHandlerDeserialize(net.neoforged.neoforge.items.ItemStackHandler handler, net.minecraft.nbt.CompoundTag tag) {
        handler.deserializeNBT(registries(), tag);
    }

    public static net.neoforged.neoforge.fluids.capability.templates.FluidTank tankRead(net.neoforged.neoforge.fluids.capability.templates.FluidTank tank, net.minecraft.nbt.CompoundTag tag) {
        return tank.readFromNBT(registries(), tag);
    }

    public static net.minecraft.nbt.CompoundTag tankWrite(net.neoforged.neoforge.fluids.capability.templates.FluidTank tank, net.minecraft.nbt.CompoundTag tag) {
        return tank.writeToNBT(registries(), tag);
    }

    public static net.minecraft.nbt.CompoundTag fluidWriteToNBT(net.neoforged.neoforge.fluids.FluidStack stack, net.minecraft.nbt.CompoundTag tag) {
        net.minecraft.nbt.Tag saved = stack.save(registries(), tag);
        return saved instanceof net.minecraft.nbt.CompoundTag c ? c : tag;
    }

    public static net.neoforged.neoforge.fluids.FluidStack fluidLoad(net.minecraft.nbt.CompoundTag tag) {
        return net.neoforged.neoforge.fluids.FluidStack.parseOptional(registries(), tag);
    }

    public static void fluidWriteToPacket(net.neoforged.neoforge.fluids.FluidStack stack, net.minecraft.network.FriendlyByteBuf buf) {
        net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, stack);
    }

    public static net.neoforged.neoforge.fluids.FluidStack fluidReadFromPacket(net.minecraft.network.FriendlyByteBuf buf) {
        return net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
    }

    /** A fluid stack's NBT is its {@code CUSTOM_DATA} component; the compound handed out is a copy (changes need {@code setTag}). */
    public static net.minecraft.nbt.CompoundTag fluidGetTag(net.neoforged.neoforge.fluids.FluidStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static boolean fluidHasTag(net.neoforged.neoforge.fluids.FluidStack stack) {
        return stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA) != null;
    }

    public static net.minecraft.nbt.CompoundTag fluidGetOrCreateTag(net.neoforged.neoforge.fluids.FluidStack stack) {
        net.minecraft.nbt.CompoundTag tag = fluidGetTag(stack);
        return tag != null ? tag : new net.minecraft.nbt.CompoundTag();
    }

    public static void fluidSetTag(net.neoforged.neoforge.fluids.FluidStack stack, net.minecraft.nbt.CompoundTag tag) {
        if (tag == null) stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        else stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public static void fluidRemoveChildTag(net.neoforged.neoforge.fluids.FluidStack stack, String key) {
        net.minecraft.nbt.CompoundTag tag = fluidGetTag(stack);
        if (tag == null) return;
        tag.remove(key);
        fluidSetTag(stack, tag.isEmpty() ? null : tag);
    }

    public static legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional<net.neoforged.neoforge.fluids.capability.IFluidHandler> fluidUtilBlock(
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
        return legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional.ofNullable(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side));
    }

    public static legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional<net.neoforged.neoforge.fluids.capability.IFluidHandlerItem> fluidUtilItem(
            net.minecraft.world.item.ItemStack stack) {
        return legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional.ofNullable(stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM));
    }

    public static boolean canItemStacksStack(net.minecraft.world.item.ItemStack a, net.minecraft.world.item.ItemStack b) {
        return !a.isEmpty() && net.minecraft.world.item.ItemStack.isSameItemSameComponents(a, b);
    }

    public static net.minecraft.world.item.ItemStack copyStackWithSize(net.minecraft.world.item.ItemStack stack, int size) {
        return size == 0 ? net.minecraft.world.item.ItemStack.EMPTY : stack.copyWithCount(size);
    }

    /** {@code SidedInvWrapper.create(inventory, sides)}: one lazily resolved handler per side. */
    @SuppressWarnings("unchecked")
    public static legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional<net.neoforged.neoforge.items.IItemHandlerModifiable>[] sidedInvWrappers(
            net.minecraft.world.WorldlyContainer inventory, net.minecraft.core.Direction... sides) {
        legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional<net.neoforged.neoforge.items.IItemHandlerModifiable>[] out = new legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional[sides.length];
        for (int i = 0; i < sides.length; i++) {
            net.minecraft.core.Direction side = sides[i];
            out[i] = legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional.of(() -> new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(inventory, side));
        }
        return out;
    }

    // ---- recipes ------------------------------------------------------------------------------------------------------------

    /** {@code ShapedRecipe.itemStackFromJson(json)}: the 1.20.1 result object ({@code item}, {@code count}, {@code nbt}). */
    public static net.minecraft.world.item.ItemStack itemStackFromJson(com.google.gson.JsonObject json) {
        return legacyconnector.v1_20_1.net.minecraftforge.common.crafting.CraftingHelper.getItemStack(json, true, false);
    }

    // ---- enum extension -----------------------------------------------------------------------------------------------------

    /** Forge's {@code RecipeBookType.create(name)} (a mod adds its own recipe book tab). */
    public static net.minecraft.world.inventory.RecipeBookType recipeBookTypeCreate(String name) {
        return org.kylepitcock.legacyconnector.core.LegacyEnums.add(net.minecraft.world.inventory.RecipeBookType.class, name, new Class<?>[0], new Object[0]);
    }

    /** Forge's {@code RecipeBookCategories.create(name, icons...)} (client). */
    public static net.minecraft.client.RecipeBookCategories recipeBookCategoriesCreate(String name, net.minecraft.world.item.ItemStack... icons) {
        return ForgeApiClient1201.categoriesCreate(name, icons);
    }

    /**
     * {@code spec.setConfig(config)}: a mod loading its own config file by hand; NeoForge hands the spec a loaded config instead. {@code LoadedConfig} is a
     * package-private record of FML (the interface is sealed), built reflectively with a detached ModConfig and no path, so saving through it is not supported.
     */
    public static void specSetConfig(net.neoforged.neoforge.common.ModConfigSpec spec, com.electronwill.nightconfig.core.CommentedConfig config) {
        try {
            Class<?> type = Class.forName("net.neoforged.fml.config.LoadedConfig", true, net.neoforged.fml.config.IConfigSpec.class.getClassLoader());
            java.lang.reflect.Constructor<?> ctor = type.getDeclaredConstructors()[0];
            org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.setAccessible(ctor, true);
            // the spec locks through the config's ModConfig: a detached one (the active mod's container, no file) serves
            java.lang.reflect.Constructor<?> modConfigCtor = net.neoforged.fml.config.ModConfig.class.getDeclaredConstructors()[0];
            org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.setAccessible(modConfigCtor, true);
            Object modConfig = modConfigCtor.newInstance(net.neoforged.fml.config.ModConfig.Type.COMMON, spec, net.neoforged.fml.ModLoadingContext.get().getActiveContainer(),
                    "legacy-" + System.identityHashCode(spec) + ".toml", new java.util.concurrent.locks.ReentrantLock());
            // a file config the mod built itself (autosave on) would write the corrected values to the same file FML writes for the registered
            // ModConfig: the two writers collide (REPLACE_ATOMIC move denied on Windows, CoFH Core, issue 271). The spec reads a detached copy; FML's own
            // load of the registered config replaces it.
            com.electronwill.nightconfig.core.CommentedConfig used = config instanceof com.electronwill.nightconfig.core.file.FileConfig ? com.electronwill.nightconfig.core.CommentedConfig.copy(config) : config;
            spec.acceptConfig((net.neoforged.fml.config.IConfigSpec.ILoadedConfig) ctor.newInstance(used, null, modConfig));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not hand a config to " + spec, e);
        }
    }

    /** Forge 1.20.1's MavenVersionStringHelper implementation; the NeoForge 1.21.1 class no longer exposes this helper. */
    public static String artifactVersionToString(org.apache.maven.artifact.versioning.ArtifactVersion artifactVersion) {
        return artifactVersion.toString();
    }

    // ---- Forge 1.20.1 ResourcePackLoader -------------------------------------------------------------------------------

    /**
     * Forge 1.20.1 returned a concrete PathPackResources from
     * ResourcePackLoader.createPackForMod(IModFileInfo). NeoForge 1.21.1 returns a
     * Pack.ResourcesSupplier instead. Palladium 4.5.10 consumes the concrete legacy type:
     * it reads the pack ID, builtin flag and source path before constructing its own wrapper.
     *
     * <p>Use the same SecureJar root as NeoForge 1.21.1's own ResourcePackLoader. This is
     * important for ordinary mod JARs and exploded development mods: FML owns the secure
     * filesystem, so the compatibility layer must neither open a second ZIP filesystem nor
     * close/copy the underlying resources itself.
     */
    public static legacyconnector.v1_20_1.net.minecraftforge.resource.PathPackResources createPackForMod(
            net.neoforged.neoforgespi.language.IModFileInfo modFileInfo) {
        java.util.Objects.requireNonNull(modFileInfo, "modFileInfo");
        var modFile = java.util.Objects.requireNonNull(modFileInfo.getFile(), "modFileInfo.file");
        var mods = java.util.Objects.requireNonNull(modFileInfo.getMods(), "modFileInfo.mods");
        String packId = mods.isEmpty() ? modFile.getFileName() : mods.get(0).getModId();
        java.nio.file.Path root = java.util.Objects.requireNonNull(
                modFile.getSecureJar(), "modFile.secureJar").getRootPath();

        return new legacyconnector.v1_20_1.net.minecraftforge.resource.PathPackResources(packId, true, root);
    }


}
