package cpw.mods.fml.relauncher;

import java.net.URL;

/** FML 1.5.2/1.6.4's class loader name: coremods cast their own class loader to it, so the stand-in loader of the coremod phase is one. */
public class RelaunchClassLoader extends net.minecraft.launchwrapper.LaunchClassLoader {
    private final java.util.Map<String, Class<?>> cachedClasses = new java.util.HashMap<>();

    @Override
    protected java.util.Map<String, Class<?>> mcc$cache() {
        return cachedClasses;
    }

    public RelaunchClassLoader(URL[] sources, ClassLoader parent) {
        super(sources, parent);
    }
}
