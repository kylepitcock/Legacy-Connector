package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.io.File;
import java.util.Map;

/** Subset of the legacy ModContainer interface that mods actually call. */
public interface ModContainer {
    /** FML asked each container to register its event handlers on the lifecycle bus; true when it did. */
    default boolean registerBus(com.google.common.eventbus.EventBus bus, LoadController controller) {
        return false;
    }

    String getModId();

    String getName();

    String getVersion();

    File getSource();

    ModMetadata getMetadata();

    Object getMod();

    String getDisplayVersion();

    Map<String, String> getCustomModProperties();

    default legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.ArtifactVersion getProcessedVersion() {
        return new legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.DefaultArtifactVersion(getModId(), getVersion());
    }

    default java.util.Set<legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.ArtifactVersion> getRequirements() {
        return new java.util.HashSet<>();
    }

    default java.util.List<legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.ArtifactVersion> getDependencies() {
        return new java.util.ArrayList<>();
    }

    default java.util.List<legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.ArtifactVersion> getDependants() {
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
