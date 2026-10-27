package legacyconnector.v1_12_2.net.minecraftforge.client.resource;

/** A reload listener told which kinds of resources were reloaded (always all of them: reloads are full). */
public interface ISelectiveResourceReloadListener extends legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener {
    @Override
    default void onResourceManagerReload(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager resourceManager) {
        onResourceManagerReload(resourceManager, type -> true);
    }

    void onResourceManagerReload(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager resourceManager,
            java.util.function.Predicate<IResourceType> resourcePredicate);
}
