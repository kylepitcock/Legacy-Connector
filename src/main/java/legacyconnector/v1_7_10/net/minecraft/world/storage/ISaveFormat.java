package legacyconnector.v1_7_10.net.minecraft.world.storage;

import java.util.List;

/** The saves folder: worlds by folder name (only the running world has live data; see {@link SaveFormatOld}). */
public interface ISaveFormat {
    String func_154333_a();

    ISaveHandler getSaveLoader(String folder, boolean storePlayerData);

    List getSaveList();

    void flushCache();

    WorldInfo getWorldInfo(String folder);

    boolean func_154335_d(String folder);

    boolean deleteWorldDirectory(String folder);

    void renameWorld(String folder, String newName);

    boolean func_154334_a(String folder);

    boolean isOldMapFormat(String folder);

    boolean canLoadWorld(String folder);
}
