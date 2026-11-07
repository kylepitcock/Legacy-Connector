package org.kylepitcock.legacyconnector.compat.waila;

import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaPlugin;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.WailaPlugin;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.impl.ModuleRegistrar;
import net.neoforged.fml.ModList;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Waila (HWYLA) -> Jade bridge for 1.12.2 mods, Jade-independent half: instantiates the {@code @WailaPlugin} classes of the 1.12.2 mods and
 * lets them register their providers in the {@link ModuleRegistrar}, which the Jade plugin reads.
 */
public final class Waila1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Waila");
    private static boolean done;

    private Waila1122() {}

    /** Runs every {@code @WailaPlugin} once, after all legacy mods finished postInit (Waila did this when the game finished loading). */
    public static synchronized void processPlugins() {
        if (done) return;
        done = true;
        String desc = Type.getDescriptor(WailaPlugin.class);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        int n = 0;
        for (var file : ModList.get().getModFiles()) {
            var scan = file.getFile().getScanResult();
            if (scan == null) continue;
            for (var a : scan.getAnnotations()) {
                if (!a.annotationType().getDescriptor().equals(desc)) continue;
                String name = a.clazz().getClassName();
                try {
                    Object required = a.annotationData().get("value");
                    if (required instanceof String id && !id.isEmpty() && !isModLoaded(id)) continue;
                    Class<?> cls = Class.forName(name, true, loader);
                    if (!IWailaPlugin.class.isAssignableFrom(cls)) continue;
                    var ctor = cls.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    ((IWailaPlugin) ctor.newInstance()).register(ModuleRegistrar.instance());
                    n++;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    LOG.warn("Waila plugin {} failed: {}", name, e.toString());
                }
            }
        }
        var r = ModuleRegistrar.instance();
        if (n > 0) LOG.info("Ran {} 1.12.2 Waila plugin(s): {} block and {} entity providers", n, r.head.size() + r.body.size() + r.tail.size() + r.stack.size(),
                r.entityHead.size() + r.entityBody.size() + r.entityTail.size());
    }

    private static boolean isModLoaded(String id) {
        try {
            return ModList.get().isLoaded(id) || legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader.isModLoaded(id);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
