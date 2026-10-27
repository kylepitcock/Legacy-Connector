package legacyconnector.v1_12_2.net.minecraft.world.storage;

import java.io.File;

/**
 * The save folder of a world. The modern server owns level.dat and the session lock, so saving world info does nothing here;
 * mods use the handler for their own files under the world folder ({@code getWorldDirectory}, {@code data/<name>.dat}).
 */
public class SaveHandler implements ISaveHandler {
    private final File worldDirectory;
    private final File mapDataDir;
    private final String saveDirectoryName;
    private final net.minecraft.world.level.Level level;

    public SaveHandler(File savesDirectory, String directoryName, boolean playersDirectoryIn, legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer dataFixer) {
        this(new File(savesDirectory, directoryName), null);
    }

    private SaveHandler(File worldDirectory, net.minecraft.world.level.Level level) {
        this.worldDirectory = worldDirectory;
        this.saveDirectoryName = worldDirectory.getName();
        this.mapDataDir = new File(worldDirectory, "data");
        this.level = level;
    }

    /** The handler of a modern level (its world folder; the client gets the working directory). */
    public static SaveHandler mcc$of(net.minecraft.world.level.Level level) {
        File dir = level instanceof net.minecraft.server.level.ServerLevel s
                ? s.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile() : new File(".");
        try {
            dir = dir.getCanonicalFile();
        } catch (java.io.IOException e) {
            dir = dir.getAbsoluteFile();
        }
        return new SaveHandler(dir, level);
    }

    @Override
    public WorldInfo loadWorldInfo() {
        return level != null ? WorldInfo.mcc$of(level) : null;
    }

    @Override
    public void checkSessionLock() {
    }

    @Override
    public void saveWorldInfoWithPlayer(WorldInfo info, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound player) {
    }

    @Override
    public void saveWorldInfo(WorldInfo info) {
    }

    @Override
    public void flush() {
    }

    @Override
    public File getWorldDirectory() {
        return worldDirectory;
    }

    @Override
    public File getMapFileFromName(String name) {
        return new File(mapDataDir, name + ".dat");
    }

    private static final java.util.Map<String, legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager> TEMPLATES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** One template manager per world folder (templates are cached in it). */
    @Override
    public legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager getStructureTemplateManager() {
        String dir = new File(worldDirectory, "structures").getPath();
        return TEMPLATES.computeIfAbsent(dir, d -> new legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager(d, null));
    }

    public String getWorldDirectoryName() {
        return saveDirectoryName;
    }
}
