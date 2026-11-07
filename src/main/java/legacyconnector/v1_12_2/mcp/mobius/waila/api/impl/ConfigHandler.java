package legacyconnector.v1_12_2.mcp.mobius.waila.api.impl;

import java.util.HashMap;
import java.util.Set;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaConfigHandler;

/** Waila's config handler (mods add their tooltip options to it and read them back), backed by the same option table as the registrar. */
public class ConfigHandler implements IWailaConfigHandler {
    private static final ConfigHandler INSTANCE = new ConfigHandler();

    public static ConfigHandler instance() {
        return INSTANCE;
    }

    public void addModule(String modName, HashMap<String, String> options) {
        if (options != null) options.forEach((key, text) -> ModuleRegistrar.instance().addConfig(modName, key, text));
    }

    public void addConfig(String modName, String key, String name) {
        ModuleRegistrar.instance().addConfig(modName, key, name);
    }

    public void addConfig(String modName, String key, String name, boolean defaultValue) {
        ModuleRegistrar.instance().addConfig(modName, key, name, defaultValue);
    }

    public void setConfig(String key, boolean state) {
        ModuleRegistrar.instance().mcc$setConfig(key, state);
    }

    @Override
    public Set<String> getModuleNames() {
        return ModuleRegistrar.instance().getModuleNames();
    }

    @Override
    public HashMap<String, String> getConfigKeys(String module) {
        return ModuleRegistrar.instance().getConfigKeys(module);
    }

    @Override
    public boolean getConfig(String key, boolean defaultValue) {
        return ModuleRegistrar.instance().getConfig(key, defaultValue);
    }

    @Override
    public boolean getConfig(String key) {
        return ModuleRegistrar.instance().getConfig(key);
    }
}
