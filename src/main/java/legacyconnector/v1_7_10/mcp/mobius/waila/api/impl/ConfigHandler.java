package legacyconnector.v1_7_10.mcp.mobius.waila.api.impl;

import java.util.HashMap;
import java.util.Set;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaConfigHandler;
import org.kylepitcock.legacyconnector.compat.waila.WailaBridge;

/**
 * Waila's config handler (mods add their tooltip options to it and read them back). Backed by the Waila -> Jade bridge's
 * option table, the same one {@code IWailaRegistrar.addConfig} fills and providers' {@code IWailaConfigHandler} reads.
 */
public class ConfigHandler implements IWailaConfigHandler {
    private static final ConfigHandler INSTANCE = new ConfigHandler();

    public static ConfigHandler instance() {
        return INSTANCE;
    }

    public void addModule(String modName, HashMap<String, String> options) {
        if (options != null) options.forEach((key, text) -> addConfig(modName, key, text));
    }

    public void addConfig(String modName, String key, String name) {
        WailaBridge.INSTANCE.addConfig(modName, key, name);
    }

    public void addConfig(String modName, String key, String name, boolean defaultValue) {
        WailaBridge.INSTANCE.addConfig(modName, key, name, defaultValue);
    }

    public void addConfigServer(String modName, String key, String name) {
        WailaBridge.INSTANCE.addConfigRemote(modName, key, name);
    }

    public void addConfigServer(String modName, String key, String name, boolean defaultValue) {
        WailaBridge.INSTANCE.addConfigRemote(modName, key, name, defaultValue);
    }

    @Override
    public Set<String> getModuleNames() {
        return WailaBridge.INSTANCE.getModuleNames();
    }

    @Override
    public HashMap<String, String> getConfigKeys(String module) {
        return WailaBridge.INSTANCE.getConfigKeys(module);
    }

    public void setConfig(String category, String key, boolean state) {
        WailaBridge.INSTANCE.mcc$setConfig(key, state);
    }

    @Override
    public boolean getConfig(String key, boolean defaultValue) {
        return WailaBridge.INSTANCE.getConfig(key, defaultValue);
    }

    @Override
    public boolean getConfig(String key) {
        return WailaBridge.INSTANCE.getConfig(key);
    }

    public boolean getConfig(String category, String key, boolean defaultValue) {
        return WailaBridge.INSTANCE.getConfig(key, defaultValue);
    }

    public boolean isServerRequired(String key) {
        return false;
    }
}
