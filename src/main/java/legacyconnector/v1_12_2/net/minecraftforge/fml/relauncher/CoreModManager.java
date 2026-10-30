package legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher;

import java.util.ArrayList;
import java.util.List;

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
}
