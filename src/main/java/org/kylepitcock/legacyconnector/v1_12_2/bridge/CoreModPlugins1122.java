package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs 1.12.2 coremod plugins the way FML did before mods loaded: the plugin is instantiated, gets its injectData, and its
 * setup hook runs. Mods check that their coremod was loaded (EnderCore fails with "accessed too early" otherwise). The plugins are
 * driven by reflection, so they work with whatever the legacy plugin interface looks like after relocation.
 */
public final class CoreModPlugins1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/CoreMods");
    private static final String RESOURCE = "META-INF/legacyconnector-coremod.txt";
    private static boolean ran;

    private CoreModPlugins1122() {}

    public static synchronized void runAll(ClassLoader loader) {
        if (ran) return;
        ran = true;
        Set<String> classes = new LinkedHashSet<>();
        try {
            var urls = loader.getResources(RESOURCE);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream in = url.openStream()) {
                    String name = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (!name.isEmpty()) classes.add(name);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not list legacy coremods: {}", e.toString());
        }
        File gameDir = FMLPaths.GAMEDIR.get().toFile();
        System.setProperty("java.awt.headless", "true");
        List<Object[]> constructed = new ArrayList<>();
        for (String name : classes) {
            try {
                Class<?> cls = Class.forName(name, true, loader);
                var ctor = cls.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object plugin = ctor.newInstance();
                int index = 0;
                for (var a : cls.getAnnotations()) {
                    if (a.annotationType().getSimpleName().equals("SortingIndex")) index = (Integer) a.annotationType().getMethod("value").invoke(a);
                }
                constructed.add(new Object[] {name, plugin, index});
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                LOG.warn("Legacy coremod {} could not be constructed: {}", name, e.toString());
            }
        }
        constructed.sort(Comparator.comparingInt(o -> (Integer) o[2]));
        for (Object[] o : constructed) setUp((String) o[0], o[1], loader, gameDir);
    }

    private static void setUp(String name, Object plugin, ClassLoader loader, File gameDir) {
        Map<String, Object> data = new HashMap<>();
        data.put("mcLocation", gameDir);
        data.put("coremodList", new ArrayList<>());
        data.put("coremodLocation", null);
        data.put("runtimeDeobfuscationEnabled", Boolean.FALSE);
        data.put("mcVersion", "1.12.2");
        try {
            Method inject = find(plugin.getClass(), "injectData", Map.class);
            if (inject != null) inject.invoke(plugin, data);
            Method setupClass = find(plugin.getClass(), "getSetupClass");
            String setup = setupClass == null ? null : (String) setupClass.invoke(plugin);
            if (setup != null && !setup.isBlank()) {
                Object hook = Class.forName(setup, true, loader).getDeclaredConstructor().newInstance();
                Method hookInject = find(hook.getClass(), "injectData", Map.class);
                if (hookInject != null) hookInject.invoke(hook, data);
                Method call = find(hook.getClass(), "call");
                if (call != null) call.invoke(hook);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOG.warn("Legacy coremod {} failed during setup: {}", name, e.toString());
        }
    }

    private static Method find(Class<?> cls, String name, Class<?>... params) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                // look further up
            }
        }
        for (Class<?> i : cls.getInterfaces()) {
            Method m = find(i, name, params);
            if (m != null) return m;
        }
        return null;
    }
}
