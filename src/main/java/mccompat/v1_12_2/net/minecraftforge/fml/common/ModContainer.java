package mccompat.v1_12_2.net.minecraftforge.fml.common;

import java.io.File;
import java.util.Map;

/** Subset of the legacy ModContainer interface that mods actually call. */
public interface ModContainer {
    String getModId();

    String getName();

    String getVersion();

    File getSource();

    ModMetadata getMetadata();

    Object getMod();

    String getDisplayVersion();

    Map<String, String> getCustomModProperties();

    default mccompat.v1_12_2.net.minecraftforge.fml.common.versioning.ArtifactVersion getProcessedVersion() {
        return new mccompat.v1_12_2.net.minecraftforge.fml.common.versioning.DefaultArtifactVersion(getModId(), getVersion());
    }

    default java.util.Set<mccompat.v1_12_2.net.minecraftforge.fml.common.versioning.ArtifactVersion> getRequirements() {
        return new java.util.HashSet<>();
    }

    default java.util.List<mccompat.v1_12_2.net.minecraftforge.fml.common.versioning.ArtifactVersion> getDependencies() {
        return new java.util.ArrayList<>();
    }

    default java.util.List<mccompat.v1_12_2.net.minecraftforge.fml.common.versioning.ArtifactVersion> getDependants() {
        return new java.util.ArrayList<>();
    }

    default String getSortingRules() {
        return "";
    }

    default boolean matches(Object mod) {
        return mod == getMod();
    }

    default boolean isImmutable() {
        return false;
    }

    default String getGuiClassName() {
        return null;
    }

    default java.util.List<String> getOwnedPackages() {
        return new java.util.ArrayList<>();
    }

    default boolean shouldLoadInEnvironment() {
        return true;
    }
}
