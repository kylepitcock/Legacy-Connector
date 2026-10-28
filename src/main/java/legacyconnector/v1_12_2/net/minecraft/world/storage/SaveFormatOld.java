package legacyconnector.v1_12_2.net.minecraft.world.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.nbt.CompressedStreamTools;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * 1.12.2 saves folder over a directory of world folders (a client's {@code saves}, a server's world container): lists the worlds,
 * reads their level.dat (the modern file keeps the same "Data" compound layout), deletes and renames them. {@link #mcc$server} is
 * the running server's.
 */
public class SaveFormatOld implements ISaveFormat {
    protected final File savesDirectory;
    protected final legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer dataFixer;

    public SaveFormatOld(File savesDirectoryIn, legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer dataFixerIn) {
        this.dataFixer = dataFixerIn;
        if (!savesDirectoryIn.exists()) savesDirectoryIn.mkdirs();
        this.savesDirectory = savesDirectoryIn;
    }

    /** The saves folder of the running server (its world's parent folder), or null without a server. */
    public static SaveFormatOld mcc$server() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        File world = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().toFile();
        return new SaveFormatOld(world.getParentFile(), null);
    }

    @Override
    public String getName() {
        return "Anvil";
    }

    @Override
    public List<WorldSummary> getSaveList() {
        List<WorldSummary> out = new ArrayList<>();
        File[] dirs = savesDirectory.listFiles(File::isDirectory);
        if (dirs == null) return out;
        for (File dir : dirs) {
            String name = dir.getName();
            WorldInfo info = getWorldInfo(name);
            if (info == null) continue;
            String display = info.getWorldName();
            out.add(new WorldSummary(info, name, display == null || display.isEmpty() ? name : display, sizeOf(dir), false));
        }
        return out;
    }

    private static long sizeOf(File dir) {
        try (var walk = java.nio.file.Files.walk(dir.toPath())) {
            return walk.filter(java.nio.file.Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
        } catch (java.io.IOException | RuntimeException e) {
            return 0L;
        }
    }

    @Override
    public void flushCache() {
    }

    @Override
    public WorldInfo getWorldInfo(String saveName) {
        File dir = new File(savesDirectory, saveName);
        if (!dir.exists()) return null;
        for (String file : new String[] {"level.dat", "level.dat_old"}) {
            File f = new File(dir, file);
            if (!f.exists()) continue;
            try (var in = new java.io.FileInputStream(f)) {
                NBTTagCompound root = CompressedStreamTools.readCompressed(in);
                return new WorldInfo(root.getCompoundTag("Data"));
            } catch (java.io.IOException | RuntimeException e) {
                // try the backup
            }
        }
        return null;
    }

    @Override
    public void renameWorld(String dirName, String newName) {
        File f = new File(new File(savesDirectory, dirName), "level.dat");
        if (!f.exists()) return;
        try {
            NBTTagCompound root;
            try (var in = new java.io.FileInputStream(f)) {
                root = CompressedStreamTools.readCompressed(in);
            }
            root.getCompoundTag("Data").setString("LevelName", newName);
            try (var out = new java.io.FileOutputStream(f)) {
                CompressedStreamTools.writeCompressed(root, out);
            }
        } catch (java.io.IOException | RuntimeException e) {
            // the name stays
        }
    }

    @Override
    public boolean isNewLevelIdAcceptable(String saveName) {
        File f = new File(savesDirectory, saveName);
        if (f.exists()) return false;
        try {
            if (!f.mkdir()) return false;
            return f.delete();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Deletes a world folder; never the world the server is running. */
    @Override
    public boolean deleteWorldDirectory(String saveName) {
        File dir = new File(savesDirectory, saveName);
        if (!dir.exists()) return true;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().toFile().equals(dir.getAbsoluteFile())) {
            return false;
        }
        try (var walk = java.nio.file.Files.walk(dir.toPath())) {
            for (var p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.deleteIfExists(p);
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            return false;
        }
    }

    @Override
    public ISaveHandler getSaveLoader(String saveName, boolean storePlayerdata) {
        return new SaveHandler(savesDirectory, saveName, storePlayerdata, dataFixer);
    }

    @Override
    public boolean isConvertible(String saveName) {
        return false;
    }

    @Override
    public boolean isOldMapFormat(String saveName) {
        return false;
    }

    @Override
    public boolean canLoadWorld(String folderName) {
        return new File(savesDirectory, folderName).isDirectory();
    }

    @Override
    public File getFile(String saveName, String filePath) {
        return new File(new File(savesDirectory, saveName), filePath);
    }
}
