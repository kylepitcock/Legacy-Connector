package org.kylepitcock.legacyconnector.compat.jdk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

/**
 * Java 8 had no module boundaries: legacy mods link against JDK-internal {@code sun.*} classes directly (JourneyMap's
 * map painter extends {@code sun.awt.image.IntegerComponentRaster}'s pipeline; the IllegalAccessError left every map
 * tile blank). The JDK's internal {@code sun.*} packages are exported to every module before legacy mods load. Exported
 * only, not opened: their private members stay closed, as they are to the reflection redirects.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class JdkInternals {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/JDK");
    private static final String[] MODULES = {"java.base", "java.desktop"};
    private static boolean done;

    private JdkInternals() {}

    public static synchronized void exportToLegacyMods() {
        if (done) return;
        done = true;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            Field implField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.Lookup impl = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(implField), unsafe.staticFieldOffset(implField));
            MethodHandle addExports = impl.findVirtual(Module.class, "implAddExports", MethodType.methodType(void.class, String.class));
            int count = 0;
            for (String name : MODULES) {
                Module module = ModuleLayer.boot().findModule(name).orElse(null);
                if (module == null) continue;
                for (String pkg : module.getPackages()) {
                    if (!pkg.startsWith("sun.") || module.isExported(pkg)) continue;
                    addExports.invoke(module, pkg);
                    count++;
                }
            }
            LOG.info("Exported {} JDK-internal sun.* package(s) for legacy mods", count);
        } catch (Throwable t) {
            LOG.warn("Could not export JDK-internal packages; legacy mods using sun.* classes may fail", t);
        }
    }
}
