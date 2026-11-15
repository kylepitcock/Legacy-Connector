package cpw.mods.fml.relauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * The parts of FML's coremod manager that plugins read or reach into by reflection: the lists of ignored and loaded coremods, and the
 * {@code loadPlugins} list a plugin adds another plugin's wrapper to (CodeChickenCore's deobfuscation plugin). Legacy Connector loads the
 * plugins itself (LegacyCoreModTransformers); it takes the wrappers added to {@code loadPlugins} while a plugin was constructed.
 */
public class CoreModManager {
    private static final List<String> ignoredModFiles = new ArrayList<>();
    private static final List<String> reparsedCoremods = new ArrayList<>();
    private static final List<String> loadedCoremods = new ArrayList<>();
    private static List<FMLPluginWrapper> loadPlugins = new ArrayList<>();

    static {
        // FML's list starts with its own plugins; plugins that add theirs reflectively count on those positions
        loadPlugins.add(new FMLPluginWrapper("FMLCorePlugin", new FMLCorePlugin(), null, Integer.MIN_VALUE));
        loadPlugins.add(new FMLPluginWrapper("FMLForgePlugin", new FMLCorePlugin(), null, Integer.MIN_VALUE));
    }

    public static String[] getIgnoredMods() {
        return ignoredModFiles.toArray(new String[0]);
    }

    public static String[] getReparseableCoremods() {
        return reparsedCoremods.toArray(new String[0]);
    }

    public static List<String> getLoadedCoremods() {
        return loadedCoremods;
    }

    public static List<String> getTransformers() {
        return new ArrayList<>();
    }

    public static class FMLPluginWrapper implements ITweaker {
        public final String name;
        public final IFMLLoadingPlugin coreModInstance;
        public final List<String> predepends;
        public final File location;
        public final int sortIndex;

        public FMLPluginWrapper(String name, IFMLLoadingPlugin coreModInstance, File location, int sortIndex, String... predepends) {
            super();
            this.name = name;
            this.coreModInstance = coreModInstance;
            this.location = location;
            this.sortIndex = sortIndex;
            this.predepends = java.util.Arrays.asList(predepends);
        }

        @Override
        public String toString() {
            return String.format("%s {%d} [%s]", this.name, this.sortIndex, this.coreModInstance.getClass().getName());
        }

        @Override
        public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        }

        @Override
        public void injectIntoClassLoader(LaunchClassLoader classLoader) {
            Map<String, Object> data = new HashMap<>();
            data.put("mcLocation", net.minecraft.launchwrapper.Launch.minecraftHome);
            data.put("coremodList", new ArrayList<Object>(loadPlugins));
            data.put("runtimeDeobfuscationEnabled", Boolean.TRUE);
            data.put("coremodLocation", location);
            data.put("classLoader", classLoader);
            String[] transformers = coreModInstance.getASMTransformerClass();
            if (transformers != null) for (String t : transformers) {
                if (t != null && !t.isEmpty()) classLoader.registerTransformer(t);
            }
            String setup = coreModInstance.getSetupClass();
            if (setup != null && !setup.isEmpty()) {
                try {
                    IFMLCallHook call = (IFMLCallHook) Class.forName(setup, true, classLoader).getDeclaredConstructor().newInstance();
                    call.injectData(new HashMap<>(data));
                    call.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            coreModInstance.injectData(data);
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
