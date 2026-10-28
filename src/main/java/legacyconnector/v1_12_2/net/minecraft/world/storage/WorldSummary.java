package legacyconnector.v1_12_2.net.minecraft.world.storage;

/** 1.12.2 entry of the world list: folder, display name, last played time and size. */
public class WorldSummary implements Comparable<WorldSummary> {
    private final String fileName;
    private final String displayName;
    private final long lastTimePlayed;
    private final long sizeOnDisk;
    private final boolean requiresConversion;
    private final boolean hardcore;
    private final boolean cheatsEnabled;

    public WorldSummary(WorldInfo info, String fileNameIn, String displayNameIn, long sizeOnDiskIn, boolean requiresConversionIn) {
        this.fileName = fileNameIn;
        this.displayName = displayNameIn;
        this.lastTimePlayed = info == null ? 0L : info.getLastTimePlayed();
        this.sizeOnDisk = sizeOnDiskIn;
        this.requiresConversion = requiresConversionIn;
        this.hardcore = info != null && info.isHardcoreModeEnabled();
        this.cheatsEnabled = info != null && info.areCommandsAllowed();
    }

    public String getFileName() {
        return fileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSizeOnDisk() {
        return sizeOnDisk;
    }

    public boolean requiresConversion() {
        return requiresConversion;
    }

    public long getLastTimePlayed() {
        return lastTimePlayed;
    }

    public boolean isHardcoreModeEnabled() {
        return hardcore;
    }

    public boolean getCheatsEnabled() {
        return cheatsEnabled;
    }

    @Override
    public int compareTo(WorldSummary other) {
        if (lastTimePlayed < other.lastTimePlayed) return 1;
        return lastTimePlayed > other.lastTimePlayed ? -1 : fileName.compareTo(other.fileName);
    }
}
