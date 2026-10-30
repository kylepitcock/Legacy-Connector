package legacyconnector.v1_12_2.net.minecraft.world.storage;

import java.io.File;

/**
 * The save folder of a world. The modern server owns level.dat and the session lock, so saving world info does nothing here;
 * mods use the handler for their own files under the world folder ({@code getWorldDirectory}, {@code data/<name>.dat}).
 */
public class SaveHandler implements ISaveHandler, IPlayerFileData {
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

    // ------------------------------------------------------------------ player data (the modern server's player storage)

    @Override
    public IPlayerFileData getPlayerNBTManager() {
        return this;
    }

    private static net.minecraft.world.level.storage.PlayerDataStorage mcc$playerStorage() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        try {
            var f = net.minecraft.server.MinecraftServer.class.getDeclaredField("playerDataStorage");
            f.setAccessible(true);
            return (net.minecraft.world.level.storage.PlayerDataStorage) f.get(server);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    @Override
    public void writePlayerData(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        var storage = mcc$playerStorage();
        if (storage != null && player != null && player.mcc$modern() instanceof net.minecraft.world.entity.player.Player p) storage.save(p);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound readPlayerData(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        var storage = mcc$playerStorage();
        if (storage == null || player == null || !(player.mcc$modern() instanceof net.minecraft.world.entity.player.Player p)) return null;
        return storage.load(p).map(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound::new).orElse(null);
    }

    @Override
    public String[] getAvailablePlayerDat() {
        var storage = mcc$playerStorage();
        File dir = storage == null ? new File(worldDirectory, "playerdata") : storage.getPlayerDir();
        String[] files = dir.list((d, n) -> n.endsWith(".dat"));
        if (files == null) return new String[0];
        for (int i = 0; i < files.length; i++) files[i] = files[i].substring(0, files[i].length() - 4);
        return files;
    }
}
