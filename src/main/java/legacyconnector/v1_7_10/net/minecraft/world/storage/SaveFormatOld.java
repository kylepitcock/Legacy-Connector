package legacyconnector.v1_7_10.net.minecraft.world.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Worlds in a saves folder, addressed by folder name. Save handlers point at the world folders; world data is
 * read from the running world only (other worlds' level.dat files are not parsed), and worlds are never deleted or
 * renamed from legacy code while the game runs.
 */
public class SaveFormatOld implements ISaveFormat {
    protected final File savesDirectory;

    public SaveFormatOld(File savesDirectory) {
        this.savesDirectory = savesDirectory;
    }

    @Override
    public String func_154333_a() {
        return "Old Format";
    }

    @Override
    public ISaveHandler getSaveLoader(String folder, boolean storePlayerData) {
        return new SaveHandler(new File(savesDirectory, folder), folder);
    }

    /** Summaries of the saved worlds; none are listed (the modern world list owns that screen). */
    @Override
    public List getSaveList() {
        return new ArrayList();
    }

    @Override
    public void flushCache() {
    }

    /** The running world's data when {@code folder} is its folder; null otherwise. */
    @Override
    public WorldInfo getWorldInfo(String folder) {
        var server = legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.getServer();
        if (server == null || server.mcc$modern() == null || !folder.equals(server.getFolderName())) return null;
        var world = server.worldServerForDimension(0);
        return world == null ? null : world.getWorldInfo();
    }

    @Override
    public boolean func_154335_d(String folder) {
        return !new File(savesDirectory, folder).exists();
    }

    @Override
    public boolean deleteWorldDirectory(String folder) {
        return false;
    }

    @Override
    public void renameWorld(String folder, String newName) {
    }

    @Override
    public boolean func_154334_a(String folder) {
        return new File(savesDirectory, folder).isDirectory();
    }

    @Override
    public boolean isOldMapFormat(String folder) {
        return false;
    }

    @Override
    public boolean canLoadWorld(String folder) {
        return new File(new File(savesDirectory, folder), "level.dat").isFile();
    }
}
