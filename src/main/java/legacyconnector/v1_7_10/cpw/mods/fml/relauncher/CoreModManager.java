package legacyconnector.v1_7_10.cpw.mods.fml.relauncher;

import java.util.ArrayList;
import java.util.List;

/**
 * FML's coremod loader. The lists name the jars FML loaded as coremods only (and so hid from mod discovery); here every
 * coremod jar is translated and loaded like any other mod, so none is hidden and the lists are empty.
 */
public class CoreModManager {
    private static final List<String> loadedCoremods = new ArrayList<>();
    private static final List<String> reparsedCoremods = new ArrayList<>();

    public static List<String> getLoadedCoremods() {
        return loadedCoremods;
    }

    public static List<String> getReparseableCoremods() {
        return reparsedCoremods;
    }
}
