package legacyconnector.v1_7_10.net.minecraft.world.storage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** The folder a world is saved in (the save folder, or a dimension's folder inside it), and the players' saved data. */
public class SaveHandler implements ISaveHandler, IPlayerFileData {
    private final File worldDirectory;
    private final String name;

    public SaveHandler(File worldDirectory, String name) {
        this.worldDirectory = worldDirectory;
        this.name = name;
    }

    @Override
    public void checkSessionLock() {
    }

    @Override
    public void flush() {
    }

    @Override
    public File getWorldDirectory() {
        return worldDirectory;
    }

    @Override
    public File getMapFileFromName(String mapName) {
        File data = new File(worldDirectory, "data");
        data.mkdirs();
        return new File(data, mapName + ".dat");
    }

    @Override
    public String getWorldDirectoryName() {
        return name;
    }

    @Override
    public IPlayerFileData getSaveHandler() {
        return this;
    }

    /** The server's player data folder (shared by all dimensions), or {@code playerdata} in this folder without a server. */
    private File mcc$playerDir() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        File dir = server != null ? server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile()
                : new File(worldDirectory, "playerdata");
        dir.mkdirs();
        return dir;
    }

    /** Saves the player as the server does ({@code <uuid>.dat}, the previous file kept as {@code .dat_old}). */
    @Override
    public void writePlayerData(EntityPlayer player) {
        if (player == null || !(player.mcc$entity instanceof Player modern)) return;
        try {
            File dir = mcc$playerDir();
            CompoundTag tag = modern.saveWithoutId(new CompoundTag());
            Path tmp = Files.createTempFile(dir.toPath(), modern.getStringUUID() + "-", ".dat");
            net.minecraft.nbt.NbtIo.writeCompressed(tag, tmp);
            net.minecraft.Util.safeReplaceFile(dir.toPath().resolve(modern.getStringUUID() + ".dat"), tmp,
                    dir.toPath().resolve(modern.getStringUUID() + ".dat_old"));
            net.neoforged.neoforge.event.EventHooks.firePlayerSavingEvent(modern, dir, modern.getStringUUID());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/SaveHandler").warn("Failed to save player data for {}", modern.getName().getString());
        }
    }

    /**
     * Loads the player's saved data into it (mods read offline players' inventories through a fake player this way) and
     * returns the data; null if the player was never saved.
     */
    @Override
    public NBTTagCompound readPlayerData(EntityPlayer player) {
        if (player == null || !(player.mcc$entity instanceof Player modern)) return null;
        File dir = mcc$playerDir();
        for (String suffix : new String[] {".dat", ".dat_old"}) {
            File file = new File(dir, modern.getStringUUID() + suffix);
            if (!file.isFile()) continue;
            try {
                CompoundTag tag = net.minecraft.nbt.NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    tag = net.minecraft.util.datafix.DataFixTypes.PLAYER.updateToCurrentVersion(server.getFixerUpper(), tag,
                            net.minecraft.nbt.NbtUtils.getDataVersion(tag, -1));
                }
                modern.load(tag);
                net.neoforged.neoforge.event.EventHooks.firePlayerLoadingEvent(modern, dir, modern.getStringUUID());
                return new NBTTagCompound(tag);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/SaveHandler").warn("Failed to load player data for {}", modern.getName().getString());
            }
        }
        return null;
    }

    /** The UUIDs of every player saved in this world. */
    @Override
    public String[] getAvailablePlayerDat() {
        String[] files = mcc$playerDir().list((d, n) -> n.endsWith(".dat"));
        if (files == null) return new String[0];
        for (int i = 0; i < files.length; i++) files[i] = files[i].substring(0, files[i].length() - 4);
        return files;
    }
}
