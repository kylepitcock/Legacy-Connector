package mccompat.v1_12_2.net.minecraftforge.fml.common.versioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A Maven-syntax version range ("[1.0,2.0)", "1.5" as a recommended version, "[1.0,]"). */
public class VersionRange {
    private final ArtifactVersion recommendedVersion;
    private final List<Restriction> restrictions;

    private VersionRange(ArtifactVersion recommended, List<Restriction> restrictions) {
        this.recommendedVersion = recommended;
        this.restrictions = restrictions;
    }

    public ArtifactVersion getRecommendedVersion() {
        return recommendedVersion;
    }

    public List<Restriction> getRestrictions() {
        return restrictions;
    }

    public static VersionRange newRange(ArtifactVersion version, List<Restriction> restrictions) {
        return new VersionRange(version, restrictions);
    }

    public static VersionRange createFromVersion(String version) {
        return new VersionRange(new DefaultArtifactVersion(version), Collections.singletonList(Restriction.EVERYTHING));
    }

    public static VersionRange createFromVersionSpec(String spec) throws InvalidVersionSpecificationException {
        if (spec == null) return null;
        List<Restriction> restrictions = new ArrayList<>();
        String process = spec.trim();
        ArtifactVersion recommended = null;
        while (process.startsWith("[") || process.startsWith("(")) {
            int index1 = process.indexOf(')');
            int index2 = process.indexOf(']');
            int index = index2;
            if (index2 < 0 || index1 >= 0 && index1 < index2) index = index1;
            if (index < 0) throw new InvalidVersionSpecificationException("Unbounded range: " + spec);
            restrictions.add(parseRestriction(process.substring(0, index + 1)));
            process = process.substring(index + 1).trim();
            if (process.startsWith(",")) process = process.substring(1).trim();
        }
        if (!process.isEmpty()) {
            if (!restrictions.isEmpty()) throw new InvalidVersionSpecificationException("Only fully-qualified sets allowed in multiple set scenario: " + spec);
            recommended = new DefaultArtifactVersion(process);
            restrictions.add(Restriction.EVERYTHING);
        }
        return new VersionRange(recommended, restrictions);
    }

    private static Restriction parseRestriction(String spec) throws InvalidVersionSpecificationException {
        boolean lowerInclusive = spec.startsWith("[");
        boolean upperInclusive = spec.endsWith("]");
        String inner = spec.substring(1, spec.length() - 1).trim();
        int comma = inner.indexOf(',');
        if (comma < 0) {
            if (!lowerInclusive || !upperInclusive) throw new InvalidVersionSpecificationException("Single version must be surrounded by []: " + spec);
            ArtifactVersion v = new DefaultArtifactVersion(inner);
            return new Restriction(v, true, v, true);
        }
        String lower = inner.substring(0, comma).trim();
        String upper = inner.substring(comma + 1).trim();
        return new Restriction(lower.isEmpty() ? null : new DefaultArtifactVersion(lower), lowerInclusive,
                upper.isEmpty() ? null : new DefaultArtifactVersion(upper), upperInclusive);
    }

    public boolean containsVersion(ArtifactVersion version) {
        for (Restriction r : restrictions) if (r.containsVersion(version)) return true;
        return false;
    }

    public boolean hasRestrictions() {
        return !restrictions.isEmpty() && recommendedVersion == null;
    }

    public boolean isUnboundedAbove() {
        return restrictions.isEmpty() || restrictions.get(restrictions.size() - 1).getUpperBound() == null;
    }

    public VersionRange restrict(VersionRange other) {
        return other;
    }

    @Override
    public String toString() {
        if (recommendedVersion != null) return recommendedVersion.getVersionString();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < restrictions.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(restrictions.get(i));
        }
        return sb.toString();
    }
}
