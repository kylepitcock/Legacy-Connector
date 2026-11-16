package org.kylepitcock.legacyconnector.vb1_7_3;

import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;

/**
 * {@code Class.forName} for Beta mods (rewritten by the loader's {@code BetaClassForName}). A name that resolves as given does. A bare Beta
 * name (no package: Beta mods shared the default package) is looked up in the calling mod's package, then in every loaded Beta mod's package
 * (an optional dependency check such as {@code Class.forName("mod_IC2")}), then in the layer's hand-written API shim
 * ({@code net.minecraft.src}: {@code ModLoader}, {@code forge.MinecraftForge}). A name found nowhere fails as it did.
 */
public final class ClassNamesB173 {
    private static final String SHIM = "legacyconnector.vb1_7_3.net.minecraft.src.";

    private ClassNamesB173() {}

    public static Class<?> forName(String name, Class<?> caller) throws ClassNotFoundException {
        return forName(name, true, caller.getClassLoader(), caller);
    }

    public static Class<?> forName(String name, boolean initialize, ClassLoader loader, Class<?> caller) throws ClassNotFoundException {
        try {
            return Class.forName(name, initialize, loader);
        } catch (ClassNotFoundException missing) {
            if (name == null || name.isEmpty() || name.startsWith("java.") || name.startsWith("[")) throw missing;
            Class<?> found = null;
            if (name.indexOf('.') < 0) {
                found = attempt(caller.getPackageName() + "." + name, initialize, caller.getClassLoader());
                if (found == null) {
                    for (LegacyModRecord mod : LegacyMods.all()) {
                        if (!"b1.7.3".equals(mod.version) || mod.mainClass == null || mod.mainClass.getClassLoader() == null) continue;
                        found = attempt(mod.mainClass.getPackageName() + "." + name, initialize, mod.mainClass.getClassLoader());
                        if (found != null) break;
                    }
                }
            }
            if (found == null) found = attempt(SHIM + name, initialize, ClassNamesB173.class.getClassLoader());
            if (found == null) throw missing;
            return found;
        }
    }

    private static Class<?> attempt(String name, boolean initialize, ClassLoader loader) {
        try {
            return Class.forName(name, initialize, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
}
