package mccompat.v1_7_10.cpw.mods.fml.common.versioning;

/** One interval of a version range. */
public class Restriction {
    private final ArtifactVersion lowerBound;
    private final boolean lowerBoundInclusive;
    private final ArtifactVersion upperBound;
    private final boolean upperBoundInclusive;
    public static final Restriction EVERYTHING = new Restriction(null, false, null, false);

    public Restriction(ArtifactVersion lower, boolean lowerInclusive, ArtifactVersion upper, boolean upperInclusive) {
        this.lowerBound = lower;
        this.lowerBoundInclusive = lowerInclusive;
        this.upperBound = upper;
        this.upperBoundInclusive = upperInclusive;
    }

    public ArtifactVersion getLowerBound() {
        return lowerBound;
    }

    public boolean isLowerBoundInclusive() {
        return lowerBoundInclusive;
    }

    public ArtifactVersion getUpperBound() {
        return upperBound;
    }

    public boolean isUpperBoundInclusive() {
        return upperBoundInclusive;
    }

    public boolean containsVersion(ArtifactVersion version) {
        if (lowerBound != null) {
            int c = lowerBound.compareTo(version);
            if (c == 0 && !lowerBoundInclusive || c > 0) return false;
        }
        if (upperBound != null) {
            int c = upperBound.compareTo(version);
            if (c == 0 && !upperBoundInclusive || c < 0) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return (lowerBoundInclusive ? "[" : "(") + (lowerBound == null ? "" : lowerBound.getVersionString()) + ","
                + (upperBound == null ? "" : upperBound.getVersionString()) + (upperBoundInclusive ? "]" : ")");
    }
}
