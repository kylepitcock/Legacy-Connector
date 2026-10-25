package legacyconnector.v1_12_2.net.minecraft.client.resources;

/** The client's resource manager; reload listeners run once as they register (the packs are already loaded). */
public class SimpleReloadableResourceManager implements IReloadableResourceManager {
    @Override
    public void registerReloadListener(IResourceManagerReloadListener listener) {
        try {
            listener.onResourceManagerReload(this);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").warn("A resource reload listener failed: {}", e.toString());
        }
    }
}
