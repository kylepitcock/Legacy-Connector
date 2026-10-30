package legacyconnector.v1_12_2.net.minecraft.server.dedicated;

/**
 * The dedicated server as 1.12 mods see it: on a dedicated server it is the {@code MinecraftServer} view itself (mods cast to it). Its
 * {@link #settings} are the server's {@code server.properties}.
 */
public class DedicatedServer extends legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer {
    /** Phantom field: accesses are redirected to {@link #mcc$get$settings}. */
    @Deprecated
    public PropertyManager settings;
    private static PropertyManager mcc$settings;

    public DedicatedServer() {
    }

    private static net.minecraft.server.MinecraftServer mcc$server() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    /** {@code server.properties} of the server directory (read once). */
    public static synchronized PropertyManager mcc$get$settings(DedicatedServer s) {
        if (mcc$settings == null) {
            var m = mcc$server();
            java.io.File dir = m == null ? net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile() : m.getServerDirectory().toFile();
            mcc$settings = new PropertyManager(new java.io.File(dir, "server.properties"));
        }
        return mcc$settings;
    }

    public static void mcc$set$settings(DedicatedServer s, PropertyManager value) {
        synchronized (DedicatedServer.class) {
            mcc$settings = value;
        }
    }

    public String getStringProperty(String key, String defaultValue) {
        return mcc$get$settings(this).getStringProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        return mcc$get$settings(this).getIntProperty(key, defaultValue);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return mcc$get$settings(this).getBooleanProperty(key, defaultValue);
    }

    public void setProperty(String key, Object value) {
        mcc$get$settings(this).setProperty(key, value);
    }

    public void saveProperties() {
        mcc$get$settings(this).saveProperties();
    }

    public String getSettingsFilename() {
        java.io.File f = mcc$get$settings(this).getPropertiesFile();
        return f != null ? f.getAbsolutePath() : "No settings file";
    }

    /** The port the server listens on. */
    public int getPort() {
        var m = mcc$server();
        return m == null ? 25565 : m.getPort();
    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }
}
