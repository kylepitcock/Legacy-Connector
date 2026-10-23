package legacyconnector.v1_7_10.net.minecraftforge.common;

/** The Forge version legacy 1.7.10 mods see (the last 1.7.10 build). */
public class ForgeVersion {
    public static final int majorVersion = 10;
    public static final int minorVersion = 13;
    public static final int revisionVersion = 4;
    public static final int buildVersion = 1614;

    public static int getMajorVersion() {
        return majorVersion;
    }

    public static int getMinorVersion() {
        return minorVersion;
    }

    public static int getRevisionVersion() {
        return revisionVersion;
    }

    public static int getBuildVersion() {
        return buildVersion;
    }

    public static String getVersion() {
        return String.format("%d.%d.%d.%d", majorVersion, minorVersion, revisionVersion, buildVersion);
    }
}
