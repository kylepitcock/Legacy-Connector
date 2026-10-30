package legacyconnector.v1_12_2.net.minecraft.world.storage;

/**
 * A world's save folder. The members naming chunk loaders and player data are left to the implementing
 * class (calls on the world's handler find them through the synthesized helpers).
 */
public interface ISaveHandler {
    WorldInfo loadWorldInfo();

    void checkSessionLock() throws legacyconnector.v1_12_2.net.minecraft.world.MinecraftException;

    void saveWorldInfoWithPlayer(WorldInfo info, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound player);

    void saveWorldInfo(WorldInfo info);

    void flush();

    java.io.File getWorldDirectory();

    java.io.File getMapFileFromName(String name);

    legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager getStructureTemplateManager();

    /** The players' saved data of this save (the modern server's player storage). */
    default IPlayerFileData getPlayerNBTManager() {
        return this instanceof IPlayerFileData data ? data : null;
    }

    /** The chunk storage of a dimension of this save (its region folder). */
    default legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.IChunkLoader getChunkLoader(legacyconnector.v1_12_2.net.minecraft.world.WorldProvider provider) {
        java.io.File dir = getWorldDirectory();
        String folder = provider == null ? null : provider.getSaveFolder();
        if (dir != null && folder != null) dir = new java.io.File(dir, folder);
        return new legacyconnector.v1_12_2.net.minecraft.world.chunk.storage.AnvilChunkLoader(dir, null);
    }
}
