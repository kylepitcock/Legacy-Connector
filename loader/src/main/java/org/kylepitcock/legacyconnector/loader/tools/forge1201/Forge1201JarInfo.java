package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Read-only identification of a Forge 1.20.1 mod jar.
 *
 * <p>This data type is deliberately separate from {@link LegacyJar}: 1.20.1 Forge bytecode must not
 * be passed through the 1.7.10/1.12.2 SRG remapper or shim until its own compatibility family exists.</p>
 */
public record Forge1201JarInfo(
        Path path,
        String modLoader,
        String loaderVersion,
        String minecraftVersionRange,
        String forgeVersionRange,
        List<Mod> mods) {

    public Forge1201JarInfo {
        mods = List.copyOf(mods);
    }

    public String displayName() {
        if (mods.isEmpty()) return path.getFileName().toString();
        return mods.stream().map(Mod::displayName)
                .reduce((a, b) -> a + ", " + b)
                .orElse(path.getFileName().toString());
    }

    public String modIds() {
        return mods.stream().map(Mod::modId)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public record Mod(String modId, String version, String displayName, List<Dependency> dependencies) {
        public Mod {
            dependencies = List.copyOf(dependencies);
        }

        public String dependencySummary() {
            return dependencies.stream()
                    .map(dependency -> (dependency.type() == DependencyType.REQUIRED ? ""
                            : dependency.type().name().toLowerCase(Locale.ROOT) + " ")
                            + dependency.modId() + " " + dependency.versionRange())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none declared");
        }
    }

    public enum DependencyType {
        REQUIRED,
        OPTIONAL,
        INCOMPATIBLE,
        DISCOURAGED,
        UNKNOWN
    }

    /** One dependency entry copied from a [[dependencies.<modid>]] section in mods.toml. */
    public record Dependency(
            String modId, String versionRange, DependencyType type, String ordering, String side) {
        public Dependency {
            type = type == null ? DependencyType.UNKNOWN : type;
        }

        /** Backwards-compatible constructor for the legacy Forge mandatory flag. */
        public Dependency(String modId, String versionRange, boolean mandatory, String ordering, String side) {
            this(modId, versionRange, mandatory ? DependencyType.REQUIRED : DependencyType.OPTIONAL, ordering, side);
        }

        public boolean mandatory() {
            return type == DependencyType.REQUIRED;
        }
    }
}
