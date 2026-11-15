package legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.launchwrapper.ITweaker;
import legacyconnector.v1_12_2.net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * FML's coremod bookkeeping, as mods read it: the jars of coremods. The layer loads coremod jars' mods itself, so every coremod jar is
 * "reparseable" (searched for mods as well) and none is ignored.
 */
public class CoreModManager {
    private CoreModManager() {
    }

    /** The jars FML skipped while searching for mods (coremod-only jars): none here. */
    public static List<String> getIgnoredMods() {
        return new ArrayList<>();
    }

    /** The coremod jars that were searched for mods too (file names). */
    public static List<String> getReparseableCoremods() {
        List<String> out = new ArrayList<>();
        for (var c : legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader.mcc$coreContainers()) {
            var source = c.getSource();
            if (source != null && !out.contains(source.getName())) out.add(source.getName());
        }
        return out;
    }

    /** FML's list of plugins to load (private there); the layer loads coremod plugins in the loader, so it only holds what a plugin adds reflectively. */
    private static final List<FMLPluginWrapper> loadPlugins = new ArrayList<>();

    /** FML's wrapper around a loading plugin: its shape (fields, constructor, tweaker methods) for plugins that inject further plugins reflectively. */
    public static class FMLPluginWrapper implements ITweaker {
        public final String name;
        public final legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.IFMLLoadingPlugin coreModInstance;
        public final List<String> predepends;
        public final File location;
        public final int sortIndex;

        public FMLPluginWrapper(String name, legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.IFMLLoadingPlugin coreModInstance, File location, int sortIndex, String... predepends) {
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
