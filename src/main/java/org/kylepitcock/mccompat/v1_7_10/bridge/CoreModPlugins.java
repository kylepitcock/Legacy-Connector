package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mccompat.v1_7_10.cpw.mods.fml.relauncher.IFMLCallHook;
import mccompat.v1_7_10.cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs legacy coremod plugins the way FML did before mods loaded: the plugin is instantiated, gets its
 * injectData, and its setup hook runs. Many mods check that their coremod was loaded (or are configured by it).
 * Their class transformers already ran over the legacy mod classes before translation (LegacyCoreModTransformers);
 * those aimed at 1.7.10 Minecraft classes have nothing to patch.
 */
public final class CoreModPlugins {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/CoreMods");
    private static final String RESOURCE = "META-INF/retrofit-coremod.txt";
    private static boolean ran;

    private CoreModPlugins() {}

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
        List<Object> plugins = new ArrayList<>();
        File gameDir = FMLPaths.GAMEDIR.get().toFile();
        // Plugins may show download dialogs (CodeChickenCore's DepLoader); there is no one to answer them.
        System.setProperty("java.awt.headless", "true");
        // As FML: every plugin is constructed first; then, by sorting index, each is set up (plugins check each
        // other's state there, e.g. OpenBlocks requires OpenModsLib's plugin to have run).
        List<Object[]> constructed = new ArrayList<>();
        for (String name : classes) {
            timed(name, loader, () -> {
                Class<?> cls = Class.forName(name, true, loader);
                Object plugin = cls.getDeclaredConstructor().newInstance();
                var index = cls.getAnnotation(IFMLLoadingPlugin.SortingIndex.class);
                synchronized (plugins) {
                    plugins.add(plugin);
                    constructed.add(new Object[] {name, plugin, index == null ? 0 : index.value()});
                }
            });
        }
        List<Object[]> ordered;
        synchronized (plugins) {
            ordered = new ArrayList<>(constructed);
        }
        ordered.sort(java.util.Comparator.comparingInt(o -> (Integer) o[2]));
        for (Object[] o : ordered) {
            String name = (String) o[0];
            timed(name, loader, () -> setUp(name, o[1], loader, gameDir, plugins));
        }
    }

    private interface Step {
        void run() throws Throwable;
    }

    /** One step of a plugin, on its own thread with a time limit: a network wait must not block startup. */
    private static void timed(String name, ClassLoader loader, Step step) {
        Thread worker = new Thread(() -> {
            try {
                step.run();
            } catch (Throwable t) {
                Throwable root = t;
                while ((root instanceof java.lang.reflect.InvocationTargetException || root instanceof ExceptionInInitializerError)
                        && root.getCause() != null) root = root.getCause();
                LOG.warn("Legacy coremod {} could not be loaded: {}", name, root.toString(), root);
            }
        }, "Retrofit coremod " + name);
        worker.setDaemon(true);
        worker.setContextClassLoader(loader);
        worker.start();
        try {
            worker.join(30_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (worker.isAlive()) LOG.warn("Legacy coremod {} is still running after 30 s; continuing without waiting for it", name);
    }

    private static void setUp(String name, Object plugin, ClassLoader loader, File gameDir, List<Object> plugins) throws Throwable {
        if (!(plugin instanceof IFMLLoadingPlugin p)) return;
        Class<?> cls = plugin.getClass();
        // As FML: the transformers exist before injectData, which may configure them. They were already applied to
        // the legacy classes before translation; these instances only serve the plugin's own code.
        String[] transformers = p.getASMTransformerClass();
        if (transformers != null) for (String t : transformers) {
            if (t == null || t.isBlank()) continue;
            try {
                Class.forName(t.trim(), true, loader).getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                LOG.debug("Legacy coremod {}: transformer {} could not be created: {}", name, t, e.toString());
            }
        }
        Map<String, Object> data = baseData(gameDir, cls);
        data.put("coremodList", plugins);
        p.injectData(data);
        // FML registered the plugin's mod container as a mod: mods check isModLoaded("<its id>").
        String containerClass = p.getModContainerClass();
        if (containerClass != null && !containerClass.isEmpty()) {
            Object container = containerClass.equals(cls.getName()) ? plugin
                    : Class.forName(containerClass, true, loader).getDeclaredConstructor().newInstance();
            if (container instanceof mccompat.v1_7_10.cpw.mods.fml.common.ModContainer mc) {
                if (mc instanceof mccompat.v1_7_10.cpw.mods.fml.common.DummyModContainer d && d.mcc$source == null) d.mcc$source = location(cls);
                mccompat.v1_7_10.cpw.mods.fml.common.Loader.mcc$addCoreModContainer(mc);
            }
        }
        String setup = p.getSetupClass();
        if (setup != null && !setup.isEmpty()) {
            Object hook = Class.forName(setup, true, loader).getDeclaredConstructor().newInstance();
            if (hook instanceof IFMLCallHook h) {
                Map<String, Object> hookData = baseData(gameDir, cls);
                hookData.put("classLoader", mccompat.v1_7_10.net.minecraft.launchwrapper.Launch.classLoader);
                hookData.put("deobfuscationFileName", null);
                h.injectData(hookData);
                h.call();
            }
        }
        LOG.info("Legacy coremod {} loaded", name);
    }

    private static Map<String, Object> baseData(File gameDir, Class<?> plugin) {
        Map<String, Object> data = new HashMap<>();
        data.put("mcLocation", gameDir);
        // A production environment (translated code uses MCP names; string SRG names are renamed at translation).
        data.put("runtimeDeobfuscationEnabled", Boolean.TRUE);
        data.put("coremodLocation", location(plugin));
        return data;
    }

    private static File location(Class<?> cls) {
        try {
            var src = cls.getProtectionDomain().getCodeSource();
            if (src == null || src.getLocation() == null) return null;
            var uri = org.kylepitcock.mccompat.compat.jdk.ResourceCompat.getLocation(src).toURI();
            return "file".equals(uri.getScheme()) ? new File(uri) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
