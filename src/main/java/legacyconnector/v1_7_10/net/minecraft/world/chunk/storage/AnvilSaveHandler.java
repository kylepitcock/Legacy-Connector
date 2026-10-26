package legacyconnector.v1_7_10.net.minecraft.world.chunk.storage;

import java.io.File;

/** The save handler of an Anvil world folder; each dimension's chunk loader reads its own folder (DIM-1, DIM1, a mod's save folder). */
public class AnvilSaveHandler extends legacyconnector.v1_7_10.net.minecraft.world.storage.SaveHandler {
    public AnvilSaveHandler(File savesDirectory, String worldName, boolean storePlayerData) {
        super(new File(savesDirectory, worldName), worldName);
    }

    public IChunkLoader getChunkLoader(legacyconnector.v1_7_10.net.minecraft.world.WorldProvider provider) {
        File dir = getWorldDirectory();
        String folder = provider == null ? null : provider.getSaveFolder();
        if (folder != null) {
            File dimension = new File(dir, folder);
            dimension.mkdirs();
            return new AnvilChunkLoader(dimension);
        }
        return new AnvilChunkLoader(dir);
    }

    /** The world data is saved by the modern game. */
    public void saveWorldInfoWithPlayer(legacyconnector.v1_7_10.net.minecraft.world.storage.WorldInfo info, legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound player) {
    }

    @Override
    public void flush() {
        try {
            legacyconnector.v1_7_10.net.minecraft.world.storage.ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
