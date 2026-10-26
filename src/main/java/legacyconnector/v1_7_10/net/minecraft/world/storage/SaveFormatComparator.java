package legacyconnector.v1_7_10.net.minecraft.world.storage;

import legacyconnector.v1_7_10.net.minecraft.world.WorldSettings;

/** A saved world in the world list: folder, display name, size, last played, game type and flags (sorted newest first). */
public class SaveFormatComparator implements Comparable<SaveFormatComparator> {
    private final String fileName;
    private final String displayName;
    private final long lastTimePlayed;
    private final long sizeOnDisk;
    private final boolean requiresConversion;
    private final WorldSettings.GameType theEnumGameType;
    private final boolean hardcore;
    private final boolean cheatsEnabled;

    public SaveFormatComparator(String fileName, String displayName, long lastTimePlayed, long sizeOnDisk, WorldSettings.GameType gameType,
            boolean requiresConversion, boolean hardcore, boolean cheatsEnabled) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.lastTimePlayed = lastTimePlayed;
        this.sizeOnDisk = sizeOnDisk;
        this.theEnumGameType = gameType;
        this.requiresConversion = requiresConversion;
        this.hardcore = hardcore;
        this.cheatsEnabled = cheatsEnabled;
    }

    /** The world's folder name. */
    public String getFileName() {
        return fileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long func_154336_c() {
        return sizeOnDisk;
    }

    public boolean requiresConversion() {
        return requiresConversion;
    }

    public long getLastTimePlayed() {
        return lastTimePlayed;
    }

    @Override
    public int compareTo(SaveFormatComparator other) {
        return lastTimePlayed < other.lastTimePlayed ? 1 : lastTimePlayed > other.lastTimePlayed ? -1 : fileName.compareTo(other.fileName);
    }

    public WorldSettings.GameType getEnumGameType() {
        return theEnumGameType;
    }

    public boolean isHardcoreModeEnabled() {
        return hardcore;
    }

    public boolean getCheatsEnabled() {
        return cheatsEnabled;
    }
}
