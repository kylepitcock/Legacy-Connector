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
    /**
     * Minecraft 1.2.5 jars hosted through the 1.4.7 chain (opt-in {@code legacyconnector.mc125}): {@link #version} is 1.4.7, this is
     * {@link LegacyVersion#V1_2_5} and {@link #notchSide} the 1.2.5 obfuscation the jar uses ('c' client, 's' server). Null otherwise.
     */
    public LegacyVersion sourceVersion;
    public char notchSide = 'c';
    public final List<LegacyModDecl> mods = new ArrayList<>();
    /** Value of the FMLCorePlugin manifest attribute, if the jar ships a coremod. */
    public String coreModClass;
    public boolean coreModContainsMod;
    /**
     * Mod ids of mcmod.info for a jar with a coremod plugin and no {@code @Mod} class: FML 1.6.4 registered the plugin's mod container (NEI's
     * {@code NEIModContainer}) as the mod, so other mods depend on that id and a library bridge replaces it by it.
     */
    public final List<String> containerModIds = new ArrayList<>();
    public String tweakClass;
    public String mixinConfigs;
    /** Resource names of the mixin configs of the translated jar that NeoForge should register ({@code [[mixins]]}); set by the transformer. */
    public final List<String> mixinConfigResources = new ArrayList<>();
    public String accessTransformer;
    public final List<String> notes = new ArrayList<>();
    /**
     * Class names (internal, without .class) this jar must not contribute because another jar of the set owns them (Minecraft 1.2.5 repacks such as LogisticsPipes' BuildCraft copy,
     * see {@code Mc125Builds}); removed before translation and part of the translation cache key.
     */
    public final java.util.Set<String> droppedClasses = new java.util.TreeSet<>();
    /**
     * {provides, apiVersion, owner, package} of every package-info {@code @API} in the jar (BuildCraft's "BuildCraftAPI|transport",
     * ...): FML counted these as mods for {@code @Optional} and for dependency checks.
     */
    public final List<String[]> apis = new ArrayList<>();
    /** {class, field} of every @SidedProxy field in the jar. */
    public final List<String[]> sidedProxies = new ArrayList<>();

    /** For a mod distributed as a folder in mods/ ({@link DirectoryMods}): the folder; {@link #path} is the jar packed from it. */
    public volatile Path sourceDir;

    /** For a mod unpacked from a bundle archive (a zip that only holds mod archives): the file name of the bundle, null otherwise. */
    public volatile String bundledIn;

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
        for (String v : new String[] {"1.4.7", "1.5.2", "1.6.4", "1.7.10", "1.12.2", "1.20.1", "1.16.5", "b1.7.3"}) candidates.add(dir.resolve(v).resolve(name));
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
