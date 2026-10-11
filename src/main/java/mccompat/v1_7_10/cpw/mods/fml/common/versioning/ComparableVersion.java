package mccompat.v1_7_10.cpw.mods.fml.common.versioning;

/** Maven-style version ordering ("1.2.10" > "1.2.9", qualifiers sorted as Maven does). */
public class ComparableVersion implements Comparable<ComparableVersion> {
    private final org.apache.maven.artifact.versioning.ComparableVersion delegate;
    private final String value;

    public ComparableVersion(String version) {
        this.value = version == null ? "" : version;
        this.delegate = new org.apache.maven.artifact.versioning.ComparableVersion(this.value);
    }

    public final void parseVersion(String version) {
    }

    @Override
    public int compareTo(ComparableVersion o) {
        return delegate.compareTo(o.delegate);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ComparableVersion c && delegate.equals(c.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
