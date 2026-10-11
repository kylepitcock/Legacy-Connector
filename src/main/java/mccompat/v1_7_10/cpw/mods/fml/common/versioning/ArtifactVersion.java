package mccompat.v1_7_10.cpw.mods.fml.common.versioning;

/** A mod version (with a label: the mod id it belongs to), or a version range. */
public interface ArtifactVersion extends Comparable<ArtifactVersion> {
    String getLabel();

    String getVersionString();

    boolean containsVersion(ArtifactVersion source);

    String getRangeString();
}
