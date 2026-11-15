package legacyconnector.v1_7_10.cpw.mods.fml.relauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.launchwrapper.ITweaker;
import legacyconnector.v1_7_10.net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * FML's coremod loader. The lists name the jars FML loaded as coremods only (and so hid from mod discovery); here every
 * coremod jar is translated and loaded like any other mod, so none is hidden and the lists are empty.
 */
public class CoreModManager {
    /** FML's private flag (Factorization reads it reflectively): the layer always runs obfuscation-free. */
    private static boolean deobfuscatedEnvironment = false;

    private static final List<String> loadedCoremods = new ArrayList<>();
    private static final List<String> reparsedCoremods = new ArrayList<>();

    public static List<String> getLoadedCoremods() {
        return loadedCoremods;
    }

    public static List<String> getReparseableCoremods() {
        return reparsedCoremods;
    }

    /** FML's list of plugins to load (private there); the layer loads coremod plugins in the loader, so it only holds what a plugin adds reflectively. */
    private static final List<FMLPluginWrapper> loadPlugins = new ArrayList<>();

    /** FML's wrapper around a loading plugin: its shape (fields, constructor, tweaker methods) for plugins that inject further plugins reflectively. */
    public static class FMLPluginWrapper implements ITweaker {
        public final String name;
        public final legacyconnector.v1_7_10.cpw.mods.fml.relauncher.IFMLLoadingPlugin coreModInstance;
        public final List<String> predepends;
        public final File location;
        public final int sortIndex;

        public FMLPluginWrapper(String name, legacyconnector.v1_7_10.cpw.mods.fml.relauncher.IFMLLoadingPlugin coreModInstance, File location, int sortIndex, String... predepends) {
            this.name = name;
            this.coreModInstance = coreModInstance;
            this.location = location;
            this.sortIndex = sortIndex;
            this.predepends = java.util.Arrays.asList(predepends);
        }

        @Override
        public String toString() {
            return String.format("%s {%d} [%s]", name, sortIndex, coreModInstance.getClass().getName());
        }

        @Override
        public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        }

        @Override
        public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        }

        @Override
        public String getLaunchTarget() {
            return "";
        }

        @Override
        public String[] getLaunchArguments() {
            return new String[0];
        }
    }
}
