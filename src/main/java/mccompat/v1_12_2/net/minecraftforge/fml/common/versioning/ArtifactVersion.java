package mccompat.v1_12_2.net.minecraftforge.fml.common.versioning;

/** A mod version (with a label: the mod id it belongs to), or a version range. */
public interface ArtifactVersion extends Comparable<ArtifactVersion> {
    String getLabel();

    String getVersionString();

    boolean containsVersion(ArtifactVersion source);

    String getRangeString();
}
