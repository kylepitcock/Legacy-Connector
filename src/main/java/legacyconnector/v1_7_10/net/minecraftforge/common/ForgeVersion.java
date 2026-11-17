package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.Collections;
import java.util.Map;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer;
import legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.ComparableVersion;

/** The Forge version legacy 1.7.10 mods see (the last 1.7.10 build) and the update-check result types. */
public class ForgeVersion {
    public static final int majorVersion = 10;
    public static final int minorVersion = 13;
    public static final int revisionVersion = 4;
    public static final int buildVersion = 1614;

    /** The state of a mod's update check. */
    public enum Status {
        PENDING(0, false, false),
        FAILED(1, false, false),
        UP_TO_DATE(2, false, false),
        OUTDATED(3, true, true),
        AHEAD(4, false, false),
        BETA(5, false, false),
        BETA_OUTDATED(6, true, true);

        private final int sheetOffset;
        private final boolean draw;
        private final boolean animated;

        Status(int sheetOffset, boolean draw, boolean animated) {
            this.sheetOffset = sheetOffset;
            this.draw = draw;
            this.animated = animated;
        }

        public int getSheetOffset() {
            return sheetOffset;
        }

        public boolean shouldDraw() {
            return draw;
        }

        public boolean isAnimated() {
            return animated;
        }
    }

    /** What an update check found. */
    public static class CheckResult {
        public final Status status;
        public final ComparableVersion target;
        public final Map<ComparableVersion, String> changes;
        public final String url;

        public CheckResult(Status status, ComparableVersion target, Map<ComparableVersion, String> changes, String url) {
            this.status = status;
            this.target = target;
            this.changes = changes == null ? Collections.emptyMap() : changes;
            this.url = url;
        }
    }

    private static final CheckResult PENDING = new CheckResult(Status.PENDING, null, null, null);

    /** The update-check result of a mod. The layer does not run Forge's online update checks, so every mod stays pending. */
    public static CheckResult getResult(ModContainer mod) {
        return PENDING;
    }

    /** Forge's own update status: the layer runs no online check and reports the version it presents as current. */
    public static Status getStatus() {
        return Status.UP_TO_DATE;
    }

    /** The newest Forge version known (the one presented). */
    public static ComparableVersion getTarget() {
        return new ComparableVersion(getVersion());
    }

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
