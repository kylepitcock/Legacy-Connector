package legacyconnector.v1_7_10.net.minecraft.world.storage;

import java.io.File;

/** The folder a world is saved in (the save folder, or a dimension's folder inside it). */
public class SaveHandler implements ISaveHandler {
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
}
