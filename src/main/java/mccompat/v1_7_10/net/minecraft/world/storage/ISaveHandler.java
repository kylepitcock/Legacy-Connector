package mccompat.v1_7_10.net.minecraft.world.storage;

import java.io.File;

public interface ISaveHandler {
    void checkSessionLock();

    void flush();

    File getWorldDirectory();

    File getMapFileFromName(String name);

    String getWorldDirectoryName();
}
