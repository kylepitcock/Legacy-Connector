package mccompat.v1_7_10.net.minecraft.client.resources;

/** Notified when the game's resources (re)load. */
public interface IResourceManagerReloadListener {
    void onResourceManagerReload(IResourceManager manager);
}
