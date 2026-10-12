package org.kylepitcock.mccompat.v1_7_10;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import mccompat.v1_7_10.cpw.mods.fml.common.Mod;
import mccompat.v1_7_10.cpw.mods.fml.common.SidedProxy;
import mccompat.v1_7_10.cpw.mods.fml.common.event.FMLEvent;
import mccompat.v1_7_10.cpw.mods.fml.common.event.FMLInitializationEvent;
import mccompat.v1_7_10.cpw.mods.fml.common.event.FMLInterModComms;
import mccompat.v1_7_10.cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import mccompat.v1_7_10.cpw.mods.fml.common.event.FMLPostInitializationEvent;
import mccompat.v1_7_10.cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.kylepitcock.mccompat.core.LegacyModRecord;
import org.kylepitcock.mccompat.core.LegacyMods;
import org.kylepitcock.mccompat.core.LegacyPlatform;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.CreativeTabBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.TileEntityBridge;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1.7.10 FML semantics: {@code @Mod} lifecycle handlers, instance/proxy/metadata injection, IMC. */
public final class Platform1710 implements LegacyPlatform {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/1.7.10");
    private static final String SIDED_PROXY = Type.getDescriptor(SidedProxy.class);

    @Override
    public String version() {
        return "1.7.10";
    }

    @Override
    public void construct(LegacyModRecord mod) throws Exception {
        mccompat.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.CONSTRUCTING);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        org.kylepitcock.mccompat.v1_7_10.bridge.CoreModPlugins.runAll(loader);
        // FML's Scala adapter: an `object` mod is the singleton Foo$.MODULE$, and its handlers live on Foo$.
        Class<?> scalaModule = null;
        try {
            scalaModule = Class.forName(mod.mainClassName + "$", false, loader);
            scalaModule.getField("MODULE$");
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            scalaModule = null;
        }
        mod.mainClass = scalaModule != null ? scalaModule : Class.forName(mod.mainClassName, true, loader);
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
        mccompat.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.PREINITIALIZATION);
        org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge.setDeferDiscovery(true);
        try {
            dispatch(mod, new FMLPreInitializationEvent(mod));
        } finally {
            org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge.setDeferDiscovery(false);
            org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge.retryFailedDiscovery();
        }
    }

    @Override
    public void init(LegacyModRecord mod) throws Exception {
        mccompat.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.INITIALIZATION);
        dispatch(mod, new FMLInitializationEvent());
    }

    @Override
    public void postInit(LegacyModRecord mod) throws Exception {
        mccompat.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.POSTINITIALIZATION);
        // IMC is delivered between init and postInit, like FML 7.10.
        var messages = FMLInterModComms.mcc$drain(mod.legacyId);
        if (!messages.isEmpty()) dispatch(mod, new FMLInterModComms.IMCEvent(messages));
        dispatch(mod, new FMLPostInitializationEvent());
    }

    @Override
    public void afterPostInit() {
        mccompat.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.AVAILABLE);
        List<LegacyBlockAdapter> adapters = new ArrayList<>();
        for (var b : BlockBridge.modBlocks()) if (b.mcc$modern instanceof LegacyBlockAdapter a) adapters.add(a);
        org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge.retryFailedDiscovery();
        org.kylepitcock.mccompat.v1_7_10.bridge.FluidBridge.register();
        TileEntityBridge.registerType(adapters);
        CreativeTabBridge.registerTabs();
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) org.kylepitcock.mccompat.compat.curios.CuriosBaubles.registerItems();
        org.kylepitcock.mccompat.compat.waila.WailaBridge.processRegistrations();
        org.kylepitcock.mccompat.v1_7_10.bridge.LegacyBiomeLayers.register();
        org.kylepitcock.mccompat.v1_7_10.bridge.LegacyWorldPresets.registerNames();
        org.kylepitcock.mccompat.v1_7_10.entity.EntitySelfTest.register();
        org.kylepitcock.mccompat.v1_7_10.entity.BlockSelfTest.register();
        org.kylepitcock.mccompat.v1_7_10.entity.DimensionSelfTest.register();
        org.kylepitcock.mccompat.v1_7_10.entity.WorldGenSelfTest.register();
        org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge.registerSaving();
        org.kylepitcock.mccompat.v1_7_10.bridge.LegacyContentSummary.register();
        org.kylepitcock.mccompat.core.RecipeSyncGuard.register();
        LOG.info("1.7.10: {} block(s), {} item(s), {} recipe(s) translated", adapters.size(),
                org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge.modItems().size(), org.kylepitcock.mccompat.v1_7_10.bridge.RecipeBridge.count());
    }

    @Override
    public void loadComplete(LegacyModRecord mod) throws Exception {
        mccompat.v1_7_10.cpw.mods.fml.common.Loader.mcc$setState(mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.AVAILABLE);
        dispatch(mod, new FMLLoadCompleteEvent());
    }
}
