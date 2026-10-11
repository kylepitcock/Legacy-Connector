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
 * Bytecode transformers are never applied (they patch 1.7.10 Minecraft classes that do not exist here).
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
        for (String name : classes) {
            // Each plugin gets a bounded amount of time: a network wait must not block startup.
            Thread worker = new Thread(() -> runOne(name, loader, gameDir, plugins), "Retrofit coremod " + name);
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
    }

    private static void runOne(String name, ClassLoader loader, File gameDir, List<Object> plugins) {
        {
            try {
                Class<?> cls = Class.forName(name, true, loader);
                Object plugin = cls.getDeclaredConstructor().newInstance();
                synchronized (plugins) {
                    plugins.add(plugin);
                }
                if (!(plugin instanceof IFMLLoadingPlugin p)) return;
                Map<String, Object> data = baseData(gameDir, cls);
                data.put("coremodList", plugins);
                p.injectData(data);
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
                String[] transformers = p.getASMTransformerClass();
                LOG.info("Legacy coremod {} loaded{}", name,
                        transformers == null || transformers.length == 0 ? "" : " (its " + transformers.length + " bytecode transformer(s) are not applied)");
            } catch (Throwable t) {
                Throwable root = t;
                while ((root instanceof java.lang.reflect.InvocationTargetException || root instanceof ExceptionInInitializerError)
                        && root.getCause() != null) root = root.getCause();
                LOG.warn("Legacy coremod {} could not be loaded: {}", name, root.toString(), root);
            }
        }
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
            var uri = src.getLocation().toURI();
            return "file".equals(uri.getScheme()) ? new File(uri) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
