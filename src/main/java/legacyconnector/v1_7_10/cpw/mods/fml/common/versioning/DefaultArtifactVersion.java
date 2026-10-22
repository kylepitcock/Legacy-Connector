package legacyconnector.v1_7_10.cpw.mods.fml.common.versioning;

/** A labelled version (a mod id and its version), or a labelled version range. */
public class DefaultArtifactVersion implements ArtifactVersion {
    private final ComparableVersion comparableVersion;
    private final String label;
    private final boolean unbounded;
    /** Null for a plain version (it contains exactly itself); built lazily only when asked for. */
    private VersionRange range;

    public DefaultArtifactVersion(String versionNumber) {
        this.comparableVersion = new ComparableVersion(versionNumber);
        this.label = null;
        this.unbounded = false;
    }

    public DefaultArtifactVersion(String label, VersionRange range) {
        this.label = label;
        this.range = range;
        this.comparableVersion = null;
        this.unbounded = false;
    }

    public DefaultArtifactVersion(String label, String version) {
        this.label = label;
        this.comparableVersion = new ComparableVersion(version);
        this.unbounded = false;
    }

    public DefaultArtifactVersion(String string, boolean unbounded) {
        this.label = string;
        this.unbounded = true;
        this.comparableVersion = null;
        this.range = null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DefaultArtifactVersion o && (unbounded || o.unbounded
                ? java.util.Objects.equals(label, o.label) : compareTo(o) == 0 && java.util.Objects.equals(label, o.label));
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(label, comparableVersion);
    }

    @Override
    public int compareTo(ArtifactVersion o) {
        if (unbounded) return 0;
        if (o instanceof DefaultArtifactVersion d && d.comparableVersion != null && comparableVersion != null) {
            return comparableVersion.compareTo(d.comparableVersion);
        }
        return comparableVersion == null ? 0 : comparableVersion.compareTo(new ComparableVersion(o.getVersionString()));
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean containsVersion(ArtifactVersion source) {
        if (source == null) return false;
        if (label != null && source.getLabel() != null && !source.getLabel().equals(label)) return false;
        if (unbounded) return true;
        if (range != null) return range.containsVersion(source);
        return comparableVersion != null && compareTo(source) == 0;
    }

    @Override
    public String getVersionString() {
        return comparableVersion == null ? "unknown" : comparableVersion.toString();
    }

    @Override
    public String getRangeString() {
        return range != null ? range.toString() : comparableVersion != null ? comparableVersion.toString() : "any";
    }

    @Override
    public String toString() {
        return label == null ? getVersionString() : label + "@" + (comparableVersion != null ? getVersionString() : getRangeString());
    }

    public VersionRange getRange() {
        if (range == null && comparableVersion != null) range = VersionRange.createFromVersion(comparableVersion.toString());
        return range;
    }
}
