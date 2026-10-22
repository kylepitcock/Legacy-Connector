package mccompat.v1_12_2.net.minecraftforge.fml.common.versioning;

/** Parses dependency strings ("modid@[1.0,)") and checks versions against them. */
public class VersionParser {
    public static ArtifactVersion parseVersionReference(String reference) {
        if (reference == null || reference.isBlank()) throw new RuntimeException("Empty reference name");
        String[] parts = reference.split("@", 2);
        String label = parts[0].trim();
        if (parts.length == 1) return new DefaultArtifactVersion(label, true);
        try {
            return new DefaultArtifactVersion(label, VersionRange.createFromVersionSpec(parts[1].trim()));
        } catch (InvalidVersionSpecificationException e) {
            return new DefaultArtifactVersion(label, true);
        }
    }

    public static boolean satisfies(ArtifactVersion target, ArtifactVersion source) {
        return target.containsVersion(source);
    }

    public static VersionRange parseRange(String range) {
        try {
            return VersionRange.createFromVersionSpec(range);
        } catch (InvalidVersionSpecificationException e) {
            return null;
        }
    }
}
