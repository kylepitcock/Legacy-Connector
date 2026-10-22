package org.kylepitcock.mccompat.v1_12_2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import mccompat.v1_12_2.net.minecraftforge.fml.common.Loader;
import mccompat.v1_12_2.net.minecraftforge.fml.common.LoaderState;
import mccompat.v1_12_2.net.minecraftforge.fml.common.Mod;
import mccompat.v1_12_2.net.minecraftforge.fml.common.SidedProxy;
import mccompat.v1_12_2.net.minecraftforge.fml.common.event.FMLConstructionEvent;
import mccompat.v1_12_2.net.minecraftforge.fml.common.event.FMLEvent;
import mccompat.v1_12_2.net.minecraftforge.fml.common.event.FMLInitializationEvent;
import mccompat.v1_12_2.net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import mccompat.v1_12_2.net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import mccompat.v1_12_2.net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.kylepitcock.mccompat.core.LegacyModRecord;
import org.kylepitcock.mccompat.core.LegacyMods;
import org.kylepitcock.mccompat.core.LegacyPlatform;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1.12.2 FML semantics: {@code @Mod} lifecycle handlers, instance/proxy/metadata injection. */
public final class Platform1122 implements LegacyPlatform {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/1.12.2");
    private static final String SIDED_PROXY = Type.getDescriptor(SidedProxy.class);

    @Override
    public String version() {
        return "1.12.2";
    }

    @Override
    public void construct(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.CONSTRUCTING);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        mod.mainClass = Class.forName(mod.mainClassName, true, loader);
        LegacyModContainer1122 container = new LegacyModContainer1122(mod);
        mod.platformData = container;
        LegacyModContainer1122.setActive(container);
        try {
            var ctor = mod.mainClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            mod.instance = ctor.newInstance();
            injectInstanceAndMetadata(mod, container);
            injectProxies(mod, loader);
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
        LegacyModContainer1122 container = (LegacyModContainer1122) mod.platformData;
        LegacyModContainer1122.setActive(container);
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

    @Override
    public void preInit(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.PREINITIALIZATION);
        dispatch(mod, new FMLPreInitializationEvent(mod));
    }

    @Override
    public void init(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.INITIALIZATION);
        dispatch(mod, new FMLInitializationEvent());
    }

    @Override
    public void postInit(LegacyModRecord mod) throws Exception {
        Loader.mcc$setState(LoaderState.POSTINITIALIZATION);
        dispatch(mod, new FMLPostInitializationEvent());
    }

    @Override
    public void afterPostInit() {
        Loader.mcc$setState(LoaderState.AVAILABLE);
    }

    @Override
    public void loadComplete(LegacyModRecord mod) throws Exception {
        dispatch(mod, new FMLLoadCompleteEvent());
    }
}
