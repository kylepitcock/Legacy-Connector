package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

/**
 * Which {@code @API} packages (named "Owner|part" or "OwnerAPI") are available. Mods ask before enabling an integration; an API
 * is present when the mod that owns it is loaded (or a modern mod bridged in its place).
 */
public class ModAPIManager {
    public static final ModAPIManager INSTANCE = new ModAPIManager();

    public boolean hasAPI(String name) {
        if (name == null || name.isEmpty()) return false;
        if (Loader.isModLoaded(name)) return true;
        int bar = name.indexOf('|');
        if (bar > 0) return Loader.isModLoaded(name.substring(0, bar));
        return name.endsWith("API") && name.length() > 3 && Loader.isModLoaded(name.substring(0, name.length() - 3));
    }

    private java.util.List<ModContainer> mcc$apis;

    /**
     * The API packages present (one container per {@code @API} package: its {@code provides} id and {@code apiVersion}), as FML listed
     * them; read from the API packages' annotations.
     */
    public synchronized Iterable<? extends ModContainer> getAPIList() {
        if (mcc$apis == null) {
            java.util.Map<String, ModContainer> byId = new java.util.LinkedHashMap<>();
            ClassLoader loader = Loader.mcc$apiLoader != null ? Loader.mcc$apiLoader : ModAPIManager.class.getClassLoader();
            for (String pkg : System.getProperty("legacyconnector.legacyApiPackages", "").split(",")) {
                if (pkg.isEmpty()) continue;
                API api;
                try {
                    api = Class.forName(pkg + ".package-info", false, loader).getAnnotation(API.class);
                } catch (ClassNotFoundException | LinkageError e) {
                    continue;
                }
                if (api == null || byId.containsKey(api.provides())) continue;
                ModMetadata md = new ModMetadata();
                md.modId = api.provides();
                md.name = api.provides();
                md.version = api.apiVersion();
                md.parent = api.owner();
                byId.put(api.provides(), new DummyModContainer(md));
            }
            mcc$apis = new java.util.ArrayList<>(byId.values());
        }
        return mcc$apis;
    }
}
