package net.minecraftforge.fml.relauncher;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of FML's coremod manager that plugins read: the lists of ignored and loaded coremods. Legacy Connector loads the plugins itself
 * (LegacyCoreModTransformers), so the lists only hold what it hands out.
 */
public class CoreModManager {
    private static final List<String> ignoredModFiles = new ArrayList<>();
    private static final List<String> reparsedCoremods = new ArrayList<>();
    private static final List<String> loadedCoremods = new ArrayList<>();

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
}
