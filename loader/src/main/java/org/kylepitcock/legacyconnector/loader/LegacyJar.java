package org.kylepitcock.legacyconnector.loader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything the inspector learned about a jar that looks like a legacy Forge mod.
 */
public final class LegacyJar {
    /** Where the jar was found; a coremod that moves it (CodeChickenCore's dependency loader) is followed by {@link #existing()}. */
    public volatile Path path;
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
    /**
     * {provides, apiVersion, owner, package} of every package-info {@code @API} in the jar (BuildCraft's "BuildCraftAPI|transport",
     * ...): FML counted these as mods for {@code @Optional} and for dependency checks.
     */
    public final List<String[]> apis = new ArrayList<>();
    /** {class, field} of every @SidedProxy field in the jar. */
    public final List<String[]> sidedProxies = new ArrayList<>();

    public LegacyJar(Path path) {
        this.path = path;
    }

    public String fileName() {
        return path.getFileName().toString();
    }

    /**
     * The jar's current location. Legacy coremods run while mods are being translated, and some move library jars
     * (CodeChickenCore's DepLoader files ForgeMultipart, Baubles ... under mods/<minecraft version>/ as 1.7.10 did),
     * so a jar that is gone from where it was found is looked for in the version folders next to it.
     */
    public Path existing() {
        Path p = path;
        if (Files.exists(p)) return p;
        Path dir = p.getParent();
        if (dir == null) return p;
        String name = p.getFileName().toString();
        List<Path> candidates = new ArrayList<>();
        for (String v : new String[] {"1.7.10", "1.12.2", "1.20.1", "1.16.5"}) candidates.add(dir.resolve(v).resolve(name));
        if (dir.getParent() != null) candidates.add(dir.getParent().resolve(name));
        for (Path c : candidates) {
            if (Files.exists(c)) {
                path = c;
                return c;
            }
        }
        return p;
    }

    /** A jar with no {@code @Mod} class has nothing the shim can bootstrap. */
    public boolean hasMods() {
        return !mods.isEmpty();
    }
}
