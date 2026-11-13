package cpw.mods.fml.relauncher;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class FMLLaunchHandler {
    /**
     * FML's singleton and its {@code classLoader}: coremods read both by reflection from their transformer's static initializer
     * ({@code ReflectionHelper.getPrivateValue(FMLLaunchHandler.class, null, "INSTANCE")}, then "classLoader"); without them the
     * transformer cannot be created (#245).
     */
    private static FMLLaunchHandler INSTANCE = new FMLLaunchHandler();
    private LaunchClassLoader classLoader;

    private FMLLaunchHandler() {
        this.classLoader = Launch.classLoader;
    }

    /** Called once the coremod class loader exists (this class may initialize before it). */
    public static void mcc$setClassLoader(Object loader) {
        if (loader instanceof LaunchClassLoader) INSTANCE.classLoader = (LaunchClassLoader) loader;
    }

    public static Side side() {
        return "SERVER".equals(System.getProperty("legacyconnector.side")) ? Side.SERVER : Side.CLIENT;
    }
}
