package mccompat.v1_7_10.mcp.mobius.waila.api;

/** Waila registration (FMP decorators/renderers are accepted through the synthesized API and ignored). */
public interface IWailaRegistrar {
    void addConfig(String modName, String key, String configtext);

    void addConfig(String modName, String key, String configtext, boolean defaultValue);

    void addConfigRemote(String modName, String key, String configtext);

    void addConfigRemote(String modName, String key, String configtext, boolean defaultValue);

    void addConfig(String modName, String key);

    void addConfig(String modName, String key, boolean defaultValue);

    void addConfigRemote(String modName, String key);

    void addConfigRemote(String modName, String key, boolean defaultValue);

    void registerStackProvider(IWailaDataProvider provider, Class<?> block);

    void registerHeadProvider(IWailaDataProvider provider, Class<?> block);

    void registerBodyProvider(IWailaDataProvider provider, Class<?> block);

    void registerTailProvider(IWailaDataProvider provider, Class<?> block);

    void registerNBTProvider(IWailaDataProvider provider, Class<?> block);

    void registerHeadProvider(IWailaEntityProvider provider, Class<?> entity);

    void registerBodyProvider(IWailaEntityProvider provider, Class<?> entity);

    void registerTailProvider(IWailaEntityProvider provider, Class<?> entity);

    void registerOverrideEntityProvider(IWailaEntityProvider provider, Class<?> entity);

    void registerNBTProvider(IWailaEntityProvider provider, Class<?> entity);

    void registerSyncedNBTKey(String key, Class<?> target);
}
