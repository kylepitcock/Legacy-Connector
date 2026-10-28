package legacyconnector.v1_12_2.net.minecraft.world.storage;

/** 1.12.2 saves folder: the worlds in it, their level.dat and their save handlers ({@link SaveFormatOld}). */
public interface ISaveFormat {
    String getName();

    ISaveHandler getSaveLoader(String saveName, boolean storePlayerdata);

    java.util.List<WorldSummary> getSaveList();

    void flushCache();

    WorldInfo getWorldInfo(String saveName);

    boolean isNewLevelIdAcceptable(String saveName);

    boolean deleteWorldDirectory(String saveName);

    void renameWorld(String dirName, String newName);

    boolean isConvertible(String saveName);

    boolean isOldMapFormat(String saveName);

    boolean canLoadWorld(String folderName);

    java.io.File getFile(String saveName, String filePath);
}
