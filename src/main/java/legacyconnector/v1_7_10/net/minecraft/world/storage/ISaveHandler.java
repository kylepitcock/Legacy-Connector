package legacyconnector.v1_7_10.net.minecraft.world.storage;

import java.io.File;

public interface ISaveHandler {
    void checkSessionLock();

    void flush();

    File getWorldDirectory();

    /** Saves the world's level data (the save handler of a mod's own detached world stores nothing). */
    default void saveWorldInfo(WorldInfo info) {
    }

    File getMapFileFromName(String name);

    String getWorldDirectoryName();

    /** The players' data store; a save handler without players (a mod's fake world) has none. */
    default IPlayerFileData getSaveHandler() {
        return null;
    }
}
