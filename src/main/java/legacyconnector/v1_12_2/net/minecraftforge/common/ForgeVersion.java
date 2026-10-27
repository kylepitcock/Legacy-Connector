package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.Collections;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.versioning.ComparableVersion;

/**
 * Forge's version (the last 1.12.2 build, which legacy mods check against) and its update checker. Update checks of legacy mods
 * are not run: every mod's result stays {@link Status#PENDING}, so no mod shows an update notice for a 1.12.2 version.
 */
public class ForgeVersion {
    public static final int majorVersion = 14;
    public static final int minorVersion = 23;
    public static final int revisionVersion = 5;
    public static final int buildVersion = 2860;
    public static final String mcVersion = "1.12.2";
    public static final String mcpVersion = "9.42";

    private static final CheckResult PENDING_CHECK = new CheckResult(Status.PENDING, null, null, null);

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
        return majorVersion + "." + minorVersion + "." + revisionVersion + "." + buildVersion;
    }

    public static Status getStatus() {
        return PENDING_CHECK.status;
    }

    public static String getTarget() {
        return null;
    }

    public static CheckResult getResult(ModContainer mod) {
        return PENDING_CHECK;
    }

    public static void startVersionCheck() {
    }

    public enum Status {
        PENDING(),
        FAILED(),
        UP_TO_DATE(),
        OUTDATED(3, true),
        AHEAD(),
        BETA(),
        BETA_OUTDATED(6, true);

        private final int sheetOffset;
        private final boolean draw;
        private final boolean animated;

        Status() {
            this(0, false, false);
        }

        Status(int sheetOffset, boolean animated) {
            this(sheetOffset, true, animated);
        }

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

    public static class CheckResult {
        public final Status status;
        public final ComparableVersion target;
        public final Map<ComparableVersion, String> changes;
        public final String url;

        public CheckResult(Status status, ComparableVersion target, Map<ComparableVersion, String> changes, String url) {
            this.status = status;
            this.target = target;
            this.changes = changes == null ? Collections.emptyMap() : Collections.unmodifiableMap(changes);
            this.url = url;
        }
    }
}
