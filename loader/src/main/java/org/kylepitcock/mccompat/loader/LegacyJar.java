package org.kylepitcock.mccompat.loader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything the inspector learned about a jar that looks like a legacy Forge mod.
 */
public final class LegacyJar {
    public final Path path;
    public LegacyVersion version;
    /** Why {@link #version} was chosen; shown in the compatibility report. */
    public String versionEvidence = "";
    public final List<LegacyModDecl> mods = new ArrayList<>();
    /** Value of the FMLCorePlugin manifest attribute, if the jar ships a coremod. */
    public String coreModClass;
    public boolean coreModContainsMod;
    public String tweakClass;
    public String mixinConfigs;
    public String accessTransformer;
    public final List<String> notes = new ArrayList<>();

    public LegacyJar(Path path) {
        this.path = path;
    }

    public String fileName() {
        return path.getFileName().toString();
    }

    /** A jar with no {@code @Mod} class has nothing the shim can bootstrap. */
    public boolean hasMods() {
        return !mods.isEmpty();
    }
}
