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

    /** Summaries of the saved worlds (each folder with a level.dat), read from their level data; newest first. */
    @Override
    public List getSaveList() {
        List<SaveFormatComparator> out = new ArrayList<>();
        File[] folders = savesDirectory == null ? null : savesDirectory.listFiles(File::isDirectory);
        if (folders == null) return out;
        for (File folder : folders) {
            File level = new File(folder, "level.dat");
            if (!level.isFile()) continue;
            String name = folder.getName();
            long lastPlayed = level.lastModified();
            var gameType = legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.SURVIVAL;
            boolean hardcore = false, cheats = false;
            try {
                var data = net.minecraft.nbt.NbtIo.readCompressed(level.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap()).getCompound("Data");
                if (data.contains("LevelName")) name = data.getString("LevelName");
                if (data.contains("LastPlayed")) lastPlayed = data.getLong("LastPlayed");
                gameType = legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.getByID(data.getInt("GameType"));
                hardcore = data.getBoolean("hardcore");
                cheats = data.getBoolean("allowCommands");
            } catch (java.io.IOException | RuntimeException e) {
                // an unreadable level.dat: listed by its folder
            }
            out.add(new SaveFormatComparator(folder.getName(), name, lastPlayed, 0L, gameType, false, hardcore, cheats));
        }
        java.util.Collections.sort(out);
        return out;
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
