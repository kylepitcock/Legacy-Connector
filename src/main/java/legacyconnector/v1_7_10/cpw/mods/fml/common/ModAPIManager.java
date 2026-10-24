package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.util.Locale;
import java.util.Set;

/**
 * Which {@code @API} packages (named "Owner|part") are available. Mods ask before enabling an integration. The shim provides
 * the CoFH energy/block/item APIs itself (RF is backed by NeoForge energy); other APIs are present when the mod that owns
 * them is loaded.
 */
public class ModAPIManager {
    public static final ModAPIManager INSTANCE = new ModAPIManager();

    private static final Set<String> PROVIDED = Set.of("cofhapi", "cofhapi|energy", "cofhapi|block", "cofhapi|item", "cofhapi|core", "cofhapi|transport",
            "cofhapi|tileentity", "cofhapi|inventory", "cofhapi|world", "cofhapi|fluid", "cofhapi|audio", "cofhapi|render", "cofhapi|client");

    public boolean hasAPI(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        if (PROVIDED.contains(lower)) return true;
        if (Loader.isModLoaded(name)) return true;
        int bar = name.indexOf('|');
        return bar > 0 && Loader.isModLoaded(name.substring(0, bar));
    }
}
