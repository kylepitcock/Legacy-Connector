package legacyconnector.v1_6_4.facade.net.minecraft.world.storage;

import java.io.File;

/** Facade of the 1.7.10 shim's {@code ISaveHandler} for 1.4.7 and 1.5.2 mods: the chunk loader of a dimension. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class ISaveHandler {
    private ISaveHandler() {}

    /**
     * {@code getChunkLoader(provider)}: the Anvil loader of the dimension's folder (Forge: the provider's save folder under the world folder, the world
     * folder itself for the overworld).
     */
    public static legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.IChunkLoader getChunkLoader(legacyconnector.v1_7_10.net.minecraft.world.storage.ISaveHandler self,
            legacyconnector.v1_7_10.net.minecraft.world.WorldProvider provider) {
        File dir = self.getWorldDirectory();
        String folder = provider == null ? null : provider.getSaveFolder();
        if (folder != null) {
            dir = new File(dir, folder);
            dir.mkdirs();
        }
        return new legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.AnvilChunkLoader(dir);
    }
}
