package org.kylepitcock.legacyconnector.v1_7_10;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import legacyconnector.v1_7_10.cpw.mods.fml.common.Mod;
import legacyconnector.v1_7_10.cpw.mods.fml.common.SidedProxy;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLInitializationEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLInterModComms;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLPostInitializationEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.LegacyPlatform;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CreativeTabBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.TileEntityBridge;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1.7.10 FML semantics: {@code @Mod} lifecycle handlers, instance/proxy/metadata injection, IMC. */
public final class Platform1710 implements LegacyPlatform {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/1.7.10");
    private static final String SIDED_PROXY = Type.getDescriptor(SidedProxy.class);

    @Override
    public String version() {
        return "1.7.10";
    }

    @Override
    public void construct(LegacyModRecord mod) throws Exception {
        legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.CONSTRUCTING);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        org.kylepitcock.legacyconnector.v1_7_10.bridge.CoreModPlugins.runAll(loader);
        // FML's Scala adapter: an `object` mod is the singleton Foo$.MODULE$, and its handlers live on Foo$.
        Class<?> scalaModule = null;
        try {
            scalaModule = Class.forName(mod.mainClassName + "$", false, loader);
            scalaModule.getField("MODULE$");
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            scalaModule = null;
        }
        mod.mainClass = scalaModule != null ? scalaModule : Class.forName(mod.mainClassName, true, loader);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodHooks.afterConstruct(loader);
        LegacyModContainer1710 container = new LegacyModContainer1710(mod);
        mod.platformData = container;
        LegacyModContainer1710.setActive(container);
        try {
            if (scalaModule != null) {
                mod.instance = scalaModule.getField("MODULE$").get(null);
            } else {
                var ctor = mod.mainClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                mod.instance = ctor.newInstance();
            }
            injectInstanceAndMetadata(mod, container);
            injectProxies(mod, loader);
        } finally {
            LegacyModContainer1710.setActive(null);
        }
        dispatch(mod, new legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLConstructionEvent());
    }

    private static void injectInstanceAndMetadata(LegacyModRecord mod, LegacyModContainer1710 container) throws IllegalAccessException {
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

    @Override
    public void registryEvents() {
        injectObjectHolders();
    }

    private static final String OBJECT_HOLDER = Type.getDescriptor(legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.ObjectHolder.class);

    /**
     * FML filled {@code @GameRegistry.ObjectHolder} fields (whole classes keyed by field name, or single fields keyed by
     * "modid:name") from the registries once the mods had registered their content; static finals included.
     */
    private static void injectObjectHolders() {
        for (LegacyModRecord mod : LegacyMods.ordered()) {
            if (!mod.version.equals("1.7.10")) continue;
            try {
                var scan = mod.container.getModInfo().getOwningFile().getFile().getScanResult();
                if (scan == null) continue;
                var loader = mod.mainClass != null ? mod.mainClass.getClassLoader() : Platform1710.class.getClassLoader();
                java.util.Map<String, String> classHolders = new java.util.HashMap<>();
                List<ModFileScanData.AnnotationData> fieldHolders = new ArrayList<>();
                for (var a : scan.getAnnotations()) {
                    if (!a.annotationType().getDescriptor().equals(OBJECT_HOLDER)) continue;
                    if (a.targetType() == java.lang.annotation.ElementType.TYPE) {
                        classHolders.put(a.clazz().getClassName(), String.valueOf(a.annotationData().get("value")));
                    } else {
                        fieldHolders.add(a);
                    }
                }
                for (var e : classHolders.entrySet()) {
                    Class<?> owner = Class.forName(e.getKey(), false, loader);
                    for (Field f : owner.getDeclaredFields()) {
                        if (!Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                        var own = f.getAnnotation(legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.ObjectHolder.class);
                        applyHolder(f, own != null ? own.value() : e.getValue() + ":" + f.getName(), e.getValue());
                    }
                }
                for (var a : fieldHolders) {
                    Class<?> owner = Class.forName(a.clazz().getClassName(), false, loader);
                    Field f = owner.getDeclaredField(a.memberName());
                    if (classHolders.containsKey(owner.getName())) continue;
                    applyHolder(f, String.valueOf(a.annotationData().get("value")), mod.legacyId);
                }
            } catch (Throwable t) {
                LOG.warn("{}: could not inject @ObjectHolder fields: {}", mod.legacyId, t.toString());
            }
        }
    }

    private static void applyHolder(Field f, String name, String defaultMod) throws Exception {
        String full = name.contains(":") ? name : defaultMod + ":" + name;
        Object value = null;
        Class<?> type = f.getType();
        if (legacyconnector.v1_7_10.net.minecraft.block.Block.class.isAssignableFrom(type)) {
            value = BlockBridge.byName(full);
        } else if (legacyconnector.v1_7_10.net.minecraft.item.Item.class.isAssignableFrom(type)) {
            value = org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.byName(full);
        }
        if (value == null || !type.isInstance(value)) return;
        f.setAccessible(true);
        org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat.set(f, null, value);
    }

    /** @SidedProxy may sit on any class of the mod; FML found them through its annotation scan, as we do. */
    private static void injectProxies(LegacyModRecord mod, ClassLoader loader) throws Exception {
        List<ModFileScanData.AnnotationData> found = new ArrayList<>();
        var scan = mod.container.getModInfo().getOwningFile().getFile().getScanResult();
        if (scan != null) {
            for (var a : scan.getAnnotations()) if (a.annotationType().getDescriptor().equals(SIDED_PROXY)) found.add(a);
        }
        boolean client = FMLEnvironment.dist.isClient();
        // The translator's own list: classes moved to shared jars are not in this mod file's scan.
        List<String[]> targets = new ArrayList<>();
        for (var a : found) targets.add(new String[] {a.clazz().getClassName(), a.memberName()});
        if (mod.metadata != null && mod.metadata.has("sidedProxies")) {
            for (var e : mod.metadata.getAsJsonArray("sidedProxies")) {
                String[] parts = e.getAsString().split("#", 2);
                boolean dup = false;
                for (String[] t : targets) if (t[0].equals(parts[0]) && t[1].equals(parts[1])) dup = true;
                if (!dup && parts.length == 2) targets.add(parts);
            }
        }
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
            if (cls.isEmpty()) cls = f.getType().getName();
            Object proxy;
            try {
                var c = Class.forName(cls, true, loader).getDeclaredConstructor();
                c.setAccessible(true);
                proxy = c.newInstance();
            } catch (ClassNotFoundException e) {
                LOG.error("{}: proxy class {} not found", mod.legacyId, cls);
                continue;
            }
            f.setAccessible(true);
            f.set(Modifier.isStatic(f.getModifiers()) ? null : mod.instance, proxy);
        }
    }

    /** Invokes every {@code @Mod.EventHandler} method of the mod whose parameter accepts {@code event}. */
    private static void dispatch(LegacyModRecord mod, FMLEvent event) throws Exception {
        LegacyModContainer1710 container = (LegacyModContainer1710) mod.platformData;
        LegacyModContainer1710.setActive(container);
        try {
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
            LegacyModContainer1710.setActive(null);
        }
    }

    private static List<Method> handlers(Class<?> cls) {
        List<Method> out = new ArrayList<>();
        // An override of an annotated superclass method is the same handler: invoking both would run it twice.
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Mod.EventHandler.class) && m.getParameterCount() == 1
                        && seen.add(m.getName() + ":" + m.getParameterTypes()[0].getName())) out.add(m);
            }
        }
        return out;
    }

    /** Dispatches a server lifecycle event to every 1.7.10 mod. */
    public static void dispatchAll(Function<LegacyModRecord, FMLEvent> factory) {
        for (LegacyModRecord mod : LegacyMods.ordered()) {
            if (!mod.version.equals("1.7.10") || mod.instance == null) continue;
            try {
                dispatch(mod, factory.apply(mod));
            } catch (Exception e) {
                LOG.error("{} failed handling a server lifecycle event", mod.legacyId, e);
            }
        }
    }

    @Override
    public void preInit(LegacyModRecord mod) throws Exception {
        legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.PREINITIALIZATION);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.setDeferDiscovery(true);
        try {
            dispatch(mod, new FMLPreInitializationEvent(mod));
        } finally {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.setDeferDiscovery(false);
            org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.retryFailedDiscovery();
        }
    }

    @Override
    public void init(LegacyModRecord mod) throws Exception {
        legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.INITIALIZATION);
        dispatch(mod, new FMLInitializationEvent());
    }

    @Override
    public void interMod(LegacyModRecord mod) throws Exception {
        legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.INITIALIZATION);
        // IMC is delivered after every mod's init and before any postInit, like FML 7.10.
        var messages = FMLInterModComms.mcc$drain(mod.legacyId);
        if (!messages.isEmpty()) dispatch(mod, new FMLInterModComms.IMCEvent(messages));
    }

    @Override
    public void postInit(LegacyModRecord mod) throws Exception {
        legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.POSTINITIALIZATION);
        dispatch(mod, new FMLPostInitializationEvent());
    }

    @Override
    public void afterPostInit() {
        injectObjectHolders();
        legacyconnector.v1_7_10.net.minecraft.item.crafting.CraftingManager.getInstance().mcc$syncSwappedList();
        legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.AVAILABLE);
        List<LegacyBlockAdapter> adapters = new ArrayList<>();
        for (var b : BlockBridge.modBlocks()) if (b.mcc$modern instanceof LegacyBlockAdapter a) adapters.add(a);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.retryFailedDiscovery();
        org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge.register();
        TileEntityBridge.registerType(adapters);
        CreativeTabBridge.registerTabs();
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) org.kylepitcock.legacyconnector.compat.curios.CuriosBaubles.registerItems();
        org.kylepitcock.legacyconnector.compat.waila.WailaBridge.processRegistrations();
        org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyBiomeLayers.register();
        org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyWorldPresets.registerNames();
        org.kylepitcock.legacyconnector.v1_7_10.entity.EntitySelfTest.register();
        org.kylepitcock.legacyconnector.v1_7_10.entity.BlockSelfTest.register();
        org.kylepitcock.legacyconnector.v1_7_10.entity.DimensionSelfTest.register();
        org.kylepitcock.legacyconnector.v1_7_10.entity.WorldGenSelfTest.register();
        org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.registerSaving();
        org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyContentSummary.register();
        org.kylepitcock.legacyconnector.core.RecipeSyncGuard.register();
        LOG.info("1.7.10: {} block(s), {} item(s), {} recipe(s) translated", adapters.size(),
                org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.modItems().size(), org.kylepitcock.legacyconnector.v1_7_10.bridge.RecipeBridge.count());
    }

    @Override
    public void loadComplete(LegacyModRecord mod) throws Exception {
        legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.AVAILABLE);
        dispatch(mod, new FMLLoadCompleteEvent());
    }
}
