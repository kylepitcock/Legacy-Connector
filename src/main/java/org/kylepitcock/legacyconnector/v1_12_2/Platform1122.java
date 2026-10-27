package org.kylepitcock.legacyconnector.v1_12_2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Mod;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.SidedProxy;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLConstructionEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLInitializationEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.LegacyPlatform;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1.12.2 FML semantics: {@code @Mod} lifecycle handlers, instance/proxy/metadata injection. */
public final class Platform1122 implements LegacyPlatform {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/1.12.2");
    private static final String SIDED_PROXY = Type.getDescriptor(SidedProxy.class);

    static {
        org.kylepitcock.legacyconnector.core.synth.Synth.addStaticResolver(org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122::resolveStatic);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.registerStaticMethods();
        org.kylepitcock.legacyconnector.v1_12_2.bridge.SynthMethods1122.register();
        org.kylepitcock.legacyconnector.v1_12_2.bridge.SynthItems1122.register();
        org.kylepitcock.legacyconnector.v1_12_2.bridge.RuntimeApis1122.register();
        org.kylepitcock.legacyconnector.v1_12_2.bridge.BlockFaces1122.register();
        org.kylepitcock.legacyconnector.v1_12_2.bridge.World1122.register();
        org.kylepitcock.legacyconnector.v1_12_2.bridge.ClientRegistrations1122.register();
        registerServerLifecycle();
        org.kylepitcock.legacyconnector.v1_12_2.bridge.EventBridge1122.register();
    }

    /** The modern server's lifecycle as FML's server events, sent to every 1.12.2 mod's {@code @Mod.EventHandler} methods. */
    private static void registerServerLifecycle() {
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        var legacyServer = legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
        bus.addListener((net.neoforged.neoforge.event.server.ServerAboutToStartEvent e) -> dispatchAll(LoaderState.SERVER_ABOUT_TO_START,
                () -> new legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent(legacyServer)));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStartingEvent e) -> dispatchAll(LoaderState.SERVER_STARTING,
                () -> new legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLServerStartingEvent(legacyServer)));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> dispatchAll(LoaderState.SERVER_STARTED,
                legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLServerStartedEvent::new));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> dispatchAll(LoaderState.SERVER_STOPPING,
                legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLServerStoppingEvent::new));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> dispatchAll(LoaderState.SERVER_STOPPED,
                legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLServerStoppedEvent::new));
    }

    private static void dispatchAll(LoaderState state, java.util.function.Supplier<FMLEvent> event) {
        boolean any = false;
        for (LegacyModRecord mod : LegacyMods.ordered()) {
            if (!"1.12.2".equals(mod.version) || mod.instance == null || !(mod.platformData instanceof LegacyModContainer1122)) continue;
            if (!any) {
                Loader.mcc$setState(state);
                any = true;
            }
            try {
                dispatch(mod, event.get());
            } catch (Exception | LinkageError e) {
                LOG.error("{} failed handling a server lifecycle event", mod.legacyId, e);
            }
        }
        if (any && state == LoaderState.SERVER_STOPPED) Loader.mcc$setState(LoaderState.AVAILABLE);
    }

    @Override
    public String version() {
        return "1.12.2";
    }

    @Override
    public void construct(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.CONSTRUCTING);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        org.kylepitcock.legacyconnector.v1_12_2.bridge.CoreModPlugins1122.runAll(loader);
        Loader.mcc$loadApiPackages(loader);
        LegacyModContainer1122 container = new LegacyModContainer1122(mod);
        mod.platformData = container;
        // the active container is set before the mod class loads: static initializers ask FML which mod they belong to
        LegacyModContainer1122.setActive(container);
        try {
            mod.mainClass = Class.forName(mod.mainClassName, true, loader);
            var ctor = mod.mainClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            mod.instance = ctor.newInstance();
            injectInstanceAndMetadata(mod, container);
            injectProxies(mod, loader);
            registerSubscribers(mod, loader);
            registerConfigs(mod, loader);
            injectCapabilities(mod, loader);
        } finally {
            LegacyModContainer1122.setActive(null);
        }
        dispatch(mod, new FMLConstructionEvent());
    }

    private static void injectInstanceAndMetadata(LegacyModRecord mod, LegacyModContainer1122 container) throws IllegalAccessException {
        for (Field f : mod.mainClass.getDeclaredFields()) {
            Mod.Instance inst = f.getAnnotation(Mod.Instance.class);
            Mod.Metadata meta = f.getAnnotation(Mod.Metadata.class);
            if (inst == null && meta == null) continue;
            f.setAccessible(true);
            Object target = Modifier.isStatic(f.getModifiers()) ? null : mod.instance;
            if (inst != null) {
                Object value = mod.instance;
                if (!inst.value().isEmpty() && !inst.value().equals(mod.legacyId)) {
                    LegacyModRecord other = LegacyMods.byLegacyId(inst.value());
                    value = other == null ? null : other.instance;
                }
                if (value != null && f.getType().isInstance(value)) f.set(target, value);
            }
            if (meta != null) f.set(target, container.getMetadata());
        }
    }

    /** @SidedProxy may sit on any class of the mod; FML found them through its annotation scan, as we do. */
    private static void injectProxies(LegacyModRecord mod, ClassLoader loader) throws Exception {
        List<ModFileScanData.AnnotationData> found = new ArrayList<>();
        var scan = mod.container.getModInfo().getOwningFile().getFile().getScanResult();
        if (scan != null) {
            for (var a : scan.getAnnotations()) if (a.annotationType().getDescriptor().equals(SIDED_PROXY)) found.add(a);
        }
        boolean client = FMLEnvironment.dist.isClient();
        List<String[]> targets = new ArrayList<>();
        for (var a : found) targets.add(new String[] {a.clazz().getClassName(), a.memberName()});
        for (var t : targets) {
            Class<?> owner;
            try {
                owner = Class.forName(t[0], true, loader);
            } catch (ClassNotFoundException | LinkageError e) {
                continue;
            }
            Field f = owner.getDeclaredField(t[1]);
            SidedProxy sp = f.getAnnotation(SidedProxy.class);
            if (sp == null) continue;
            if (!sp.modId().isEmpty() && !sp.modId().equals(mod.legacyId)) continue;
            String cls = client ? sp.clientSide() : sp.serverSide();
            if (cls.isEmpty()) {
                // Forge's default: the nested ClientProxy / ServerProxy class of the field type
                String nested = f.getType().getName() + (client ? "$ClientProxy" : "$ServerProxy");
                try {
                    Class.forName(nested, false, loader);
                    cls = nested;
                } catch (ClassNotFoundException | LinkageError e) {
                    cls = f.getType().getName();
                }
            }
            Object proxy;
            try {
                var c = Class.forName(cls, true, loader).getDeclaredConstructor();
                c.setAccessible(true);
                proxy = c.newInstance();
            } catch (ClassNotFoundException e) {
                LOG.error("{}: proxy class {} not found", mod.legacyId, cls);
                continue;
            } catch (ReflectiveOperationException | LinkageError e) {
                LOG.error("{}: the proxy {} could not be created ({}): {}", mod.legacyId, cls, e.getClass().getSimpleName(),
                        java.lang.reflect.Modifier.isAbstract(Class.forName(cls, false, loader).getModifiers()) ? "it is abstract" : String.valueOf(e.getMessage()));
                continue;
            }
            f.setAccessible(true);
            f.set(Modifier.isStatic(f.getModifiers()) ? null : mod.instance, proxy);
        }
    }

    /** {@code @CapabilityInject} fields and methods wait in the manager until their capability type is registered. */
    private static void injectCapabilities(LegacyModRecord mod, ClassLoader loader) {
        var scan = mod.container.getModInfo().getOwningFile().getFile().getScanResult();
        if (scan == null) return;
        String desc = Type.getDescriptor(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityInject.class);
        for (var a : scan.getAnnotations()) {
            if (!a.annotationType().getDescriptor().equals(desc)) continue;
            try {
                Class<?> owner = Class.forName(a.clazz().getClassName(), false, loader);
                String member = a.memberName();
                java.lang.reflect.Member target = null;
                if (member.contains("(")) {
                    for (Method m : owner.getDeclaredMethods()) if ((m.getName() + Type.getMethodDescriptor(m)).equals(member)) target = m;
                } else {
                    target = owner.getDeclaredField(member);
                }
                // the capability type may be a class of an absent mod: read its name from the scan data instead of loading it
                Object value = a.annotationData().get("value");
                if (target != null && value instanceof Type t) {
                    legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityManager.INSTANCE.mcc$addInjection(t.getClassName(), target);
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                LOG.warn("{}: cannot prepare the capability injection {}: {}", mod.legacyId, a.memberName(), e.toString());
            }
        }
    }

    /** {@code @Config} classes: their static fields are filled from the config file before preInit. */
    private static void registerConfigs(LegacyModRecord mod, ClassLoader loader) {
        var scan = mod.container.getModInfo().getOwningFile().getFile().getScanResult();
        if (scan == null) return;
        String desc = Type.getDescriptor(legacyconnector.v1_12_2.net.minecraftforge.common.config.Config.class);
        for (var a : scan.getAnnotations()) {
            if (!a.annotationType().getDescriptor().equals(desc)) continue;
            try {
                legacyconnector.v1_12_2.net.minecraftforge.common.config.ConfigManager.mcc$register(Class.forName(a.clazz().getClassName(), false, loader));
            } catch (ClassNotFoundException | LinkageError e) {
                LOG.warn("{}: cannot read the config class {}: {}", mod.legacyId, a.clazz().getClassName(), e.toString());
            }
        }
    }

    /** {@code @Mod.EventBusSubscriber} classes: their static handlers go on the Forge bus when the mod is constructed. */
    private static void registerSubscribers(LegacyModRecord mod, ClassLoader loader) {
        var scan = mod.container.getModInfo().getOwningFile().getFile().getScanResult();
        if (scan == null) return;
        String desc = Type.getDescriptor(Mod.EventBusSubscriber.class);
        boolean client = FMLEnvironment.dist.isClient();
        for (var a : scan.getAnnotations()) {
            if (!a.annotationType().getDescriptor().equals(desc)) continue;
            try {
                Class<?> cls = Class.forName(a.clazz().getClassName(), false, loader);
                Mod.EventBusSubscriber sub = cls.getAnnotation(Mod.EventBusSubscriber.class);
                if (sub == null) continue;
                boolean match = false;
                for (var side : sub.value()) match |= client ? side == legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.CLIENT
                        : side == legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.SERVER;
                if (match) legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(cls);
            } catch (ClassNotFoundException | LinkageError e) {
                LOG.warn("{}: cannot register the event subscriber {}: {}", mod.legacyId, a.clazz().getClassName(), e.toString());
            }
        }
    }

    /** Invokes every {@code @Mod.EventHandler} method of the mod whose parameter accepts {@code event}. */
    private static void dispatch(LegacyModRecord mod, FMLEvent event) throws Exception {
        LegacyModContainer1122 container = (LegacyModContainer1122) mod.platformData;
        LegacyModContainer1122.setActive(container);
        try {
            try {
                container.mcc$bus().post(event);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("{}: a lifecycle listener on the container bus failed for {}: {}", mod.legacyId, event.getClass().getSimpleName(), e.toString());
            }
            for (Method m : handlers(mod.mainClass)) {
                if (m.getParameterTypes()[0].isInstance(event)) {
                    m.setAccessible(true);
                    try {
                        m.invoke(Modifier.isStatic(m.getModifiers()) ? null : mod.instance, event);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause() instanceof Exception ex ? ex : new RuntimeException(e.getCause());
                    }
                }
            }
        } finally {
            LegacyModContainer1122.setActive(null);
        }
    }

    private static List<Method> handlers(Class<?> cls) {
        List<Method> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Mod.EventHandler.class) && m.getParameterCount() == 1
                        && seen.add(m.getName() + ":" + m.getParameterTypes()[0].getName())) out.add(m);
            }
        }
        return out;
    }

    /** The capabilities Forge itself registers exist before any mod's preInit. */
    @Override
    public void beforePreInit() {
        legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler.register();
        legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler.register();
        legacyconnector.v1_12_2.net.minecraftforge.energy.CapabilityEnergy.register();
        // mods create their own registries here, before any preInit (EnderIO needs its mod object registry then)
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.NewRegistry());
    }

    @Override
    public void preInit(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.PREINITIALIZATION);
        legacyconnector.v1_12_2.net.minecraftforge.common.config.ConfigManager.sync(mod.legacyId, legacyconnector.v1_12_2.net.minecraftforge.common.config.Config.Type.INSTANCE);
        dispatch(mod, new FMLPreInitializationEvent(mod));
    }

    @Override
    public void init(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.INITIALIZATION);
        dispatch(mod, new FMLInitializationEvent());
    }

    @Override
    public void interMod(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.INITIALIZATION);
        // IMC is delivered after every mod's init and before any postInit
        var messages = legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLInterModComms.mcc$drain(mod.legacyId);
        if (!messages.isEmpty()) dispatch(mod, new legacyconnector.v1_12_2.net.minecraftforge.fml.common.event.FMLInterModComms.IMCEvent(messages));
    }

    @Override
    public void postInit(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.POSTINITIALIZATION);
        dispatch(mod, new FMLPostInitializationEvent());
    }

    /** Forge 1.12 posted the registry events after preInit: blocks first, then items (item blocks look their block up). */
    @Override
    public void registryEvents() {
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register<>(
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS));
        injectObjectHolders(); // Forge fills the holders after each registry event: item registration reads block holders
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register<>(
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ITEMS));
        injectObjectHolders(); // later registry events (recipes) read the holders
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register<>(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.SOUND_EVENTS));
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register<>(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.POTIONS));
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register<>(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ENCHANTMENTS));
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register<>(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.RECIPES));
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register<>(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BIOMES));
        post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register<>(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ENTITIES));
        // registries typed by classes only the synthesizer has; the events go out once the type is known
        org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122R.postSynthesized(Platform1122::post);
        for (var custom : org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.customRegistries()) {
            if (custom.getRegistrySuperType() != null) post(new legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent.Register(custom));
        }
        if (FMLEnvironment.dist.isClient()) post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.ModelRegistryEvent());
        MISSING_HOLDERS.clear();
        injectObjectHolders();
        if (!MISSING_HOLDERS.isEmpty()) LOG.warn("Object holders with nothing registered under their name: {}", MISSING_HOLDERS);
    }

    private static final java.util.Set<String> MISSING_HOLDERS = new java.util.TreeSet<>();

    /** {@code ObjectHolderRegistry.applyObjectHolders()}: mods refill their holders after registering late. */
    public static void applyObjectHolders() {
        injectObjectHolders();
    }

    /** {@code @GameRegistry.ObjectHolder} fields (and classes with a mod id) receive the registered block or item of their name. */
    private static void injectObjectHolders() {
        String desc = Type.getDescriptor(legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder.class);
        for (LegacyModRecord mod : LegacyMods.all()) {
            if (!"1.12.2".equals(mod.version) || mod.container == null) continue;
            var scan = mod.container.getModInfo().getOwningFile().getFile().getScanResult();
            if (scan == null) continue;
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            for (var a : scan.getAnnotations()) {
                if (!a.annotationType().getDescriptor().equals(desc)) continue;
                try {
                    Class<?> owner = Class.forName(a.clazz().getClassName(), false, loader);
                    String member = a.memberName();
                    if (member.equals(a.clazz().getClassName()) || member.isEmpty()) {
                        var holder = owner.getAnnotation(legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder.class);
                        if (holder == null) continue;
                        for (Field f : owner.getDeclaredFields()) {
                            if (Modifier.isStatic(f.getModifiers()) && !f.isAnnotationPresent(legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder.class)) {
                                holderField(f, holder.value() + ":" + f.getName().toLowerCase(java.util.Locale.ROOT));
                            }
                        }
                    } else {
                        Field f = owner.getDeclaredField(member);
                        var holder = f.getAnnotation(legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder.class);
                        if (holder != null) holderField(f, holder.value().contains(":") ? holder.value() : mod.legacyId + ":" + holder.value());
                    }
                } catch (ReflectiveOperationException | LinkageError e) {
                    LOG.warn("{}: cannot inject the object holder {}: {}", mod.legacyId, a.memberName(), e.toString());
                }
            }
        }
    }

    private static void holderField(Field f, String name) {
        var key = new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name);
        Object value = null;
        if (legacyconnector.v1_12_2.net.minecraft.block.Block.class.isAssignableFrom(f.getType())) value = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(key);
        else if (legacyconnector.v1_12_2.net.minecraft.item.Item.class.isAssignableFrom(f.getType())) value = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ITEMS.getValue(key);
        if (value == null || !f.getType().isInstance(value)) {
            if (value == null && (legacyconnector.v1_12_2.net.minecraft.block.Block.class.isAssignableFrom(f.getType())
                    || legacyconnector.v1_12_2.net.minecraft.item.Item.class.isAssignableFrom(f.getType()))) MISSING_HOLDERS.add(name);
            return;
        }
        try {
            org.kylepitcock.legacyconnector.core.StaticFields.set(f, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Cannot inject {} into {}: {}", name, f, e.toString());
        }
    }

    private static void post(legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event event) {
        legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public void afterPostInit() {
        Loader.mcc$setState(LoaderState.AVAILABLE);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122.registerType();
        java.util.Map<String, int[]> perMod = new java.util.TreeMap<>();
        for (var item : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems()) {
            var name = item.getRegistryName();
            if (name != null) perMod.computeIfAbsent(name.getNamespace(), k -> new int[2])[0]++;
        }
        for (var block : org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValues()) {
            var name = block.getRegistryName();
            if (name != null && block.mcc$modern != null && !"minecraft".equals(name.getNamespace())) perMod.computeIfAbsent(name.getNamespace(), k -> new int[2])[1]++;
        }
        StringBuilder sb = new StringBuilder();
        perMod.forEach((k, v) -> sb.append(' ').append(k).append('=').append(v[0]).append('/').append(v[1]));
        LOG.info("1.12.2 items/blocks per mod:{}", sb);
        String dbg = System.getProperty("legacyconnector.debugItems");
        if (dbg != null) {
            StringBuilder ids = new StringBuilder();
            for (var item : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems()) {
                var name = item.getRegistryName();
                if (name != null && name.getNamespace().equals(dbg)) ids.append(' ').append(name.getPath());
            }
            LOG.info("1.12.2 items of {}:{}", dbg, ids);
        }
        LOG.info("1.12.2 content registered: {} blocks, {} items, {} recipes, {} fluids, {} ore dictionary names",
                org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems().stream().filter(i -> i instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock).count(),
                org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modItems().size(),
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.RECIPES.getValues().size(),
                legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry.getRegisteredFluids().size(),
                legacyconnector.v1_12_2.net.minecraftforge.oredict.OreDictionary.getOreNames().length);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.CreativeTabBridge1122.registerTabs();
    }

    @Override
    public void loadComplete(LegacyModRecord mod) throws Exception {
        dispatch(mod, new FMLLoadCompleteEvent());
    }
}
