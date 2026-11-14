package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.Locale;
import java.util.zip.ZipFile;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand;
import org.kylepitcock.legacyconnector.loader.synth.SynthJarBuilder;
import org.kylepitcock.legacyconnector.loader.transform.LegacyJarTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the loader half, discovered by FML as an {@link IModFileCandidateLocator} service.
 *
 * <p>Runs before the built-in mods-folder locator (higher priority), claims every legacy Forge jar in the mods
 * folder via {@link ILaunchContext#addLocated(Path)} so FML does not reject it as "made for an older Forge",
 * translates it into a cached NeoForge jar and submits that instead. The original jar is never modified.</p>
 */
public final class LegacyModLocator implements IModFileCandidateLocator {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");
    /** Nested runtime (shim) mod inside the distributed loader jar. Absent in development. */
    static final String EMBEDDED_RUNTIME = "/META-INF/legacyconnector/legacyconnector-runtime.jar";
    static final String SHIM_INDEX_PROPERTY = "legacyconnector.shimIndex";

    /**
     * 1.12 jars may carry libraries and coremods as nested jars named by the {@code ContainedDeps} manifest attribute (stored under
     * {@code META-INF/libraries/}); Forge unpacked them next to the mods. They are extracted and handled as legacy jars of their own.
     */
    private static void addContainedDeps(Path jarPath, Path nestedDir, List<LegacyJar> legacy, java.util.Set<String> seenNames) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jarPath.toFile())) {
            var entry = zip.getEntry("META-INF/MANIFEST.MF");
            if (entry == null) return;
            String deps;
            try (var in = zip.getInputStream(entry)) {
                deps = new Manifest(in).getMainAttributes().getValue("ContainedDeps");
            }
            if (deps == null || deps.isBlank()) return;
            for (String dep : deps.trim().split("\\s+")) {
                var nested = zip.getEntry("META-INF/libraries/" + dep);
                if (nested == null) nested = zip.getEntry(dep);
                if (nested == null || !seenNames.add(dep.toLowerCase())) continue;
                Files.createDirectories(nestedDir);
                Path target = nestedDir.resolve(dep);
                try (var in = zip.getInputStream(nested)) {
                    Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                LegacyJar jar = LegacyJarInspector.inspect(target);
                if (jar != null) legacy.add(jar);
            }
        } catch (IOException | RuntimeException e) {
            LOG.debug("Legacy Connector: could not read the contained dependencies of {}", jarPath, e);
        }
    }

    /**
     * Forge 1.20.1 Jar-in-Jar mods are translated by Forge1201Transformer,
     * but they are not separate entries in the top-level legacy jar list.
     * Include their mod IDs in the runtime facade event fan-out so nested
     * mods (for example PalladiumCore) receive the old Forge registration
     * events that their translated code subscribes to.
     */
    static Set<String> nestedForge1201ModIds(LegacyJar parent, Path scanDir) {
        Set<String> ids = new java.util.LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(parent.existing().toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".jar")
                        || !(name.startsWith("META-INF/jarjar/") || name.startsWith("META-INF/jars/"))) {
                    continue;
                }

                Files.createDirectories(scanDir);
                Path nestedPath = Files.createTempFile(scanDir, "legacyconnector-forge1201-modid-", ".jar");
                try {
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, nestedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    LegacyJar nested = LegacyJarInspector.inspect(nestedPath);
                    if (nested == null || nested.version != LegacyVersion.V1_20_1) continue;
                    for (LegacyModDecl mod : nested.mods) {
                        String id = mod.fmlId();
                        if (id != null && !id.isBlank()) ids.add(id);
                    }
                } finally {
                    Files.deleteIfExists(nestedPath);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.debug("Legacy Connector: could not inspect nested Forge 1.20.1 mod IDs in {}", parent.fileName(), e);
        }
        return ids;
    }

    /**
     * Distribution archives of the 1.6 era carried the mod itself as an archive in a folder named "Put contents into the Mods Folder" next to a
     * server-jar patch (Goblins! Mod: the patch has the classes only, the textures and sounds are in the inner zip). Users unzipped them; this extracts
     * the inner archive and loads that instead of the wrapper. Null when the archive is not such a wrapper.
     */
    private static Path packagedMod(Path archive, Path nestedDir) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String n = e.getName();
                if (e.isDirectory() || !n.matches("(?i)[^/]*mods?[ _-]?folder[^/]*/[^/]+[.](zip|jar)")) continue;
                Files.createDirectories(nestedDir);
                Path target = nestedDir.resolve(archive.getFileName().toString().replaceAll("[.](zip|jar)$", "") + "-" + n.substring(n.lastIndexOf('/') + 1));
                try (var in = zip.getInputStream(e)) {
                    Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                return target;
            }
        } catch (IOException | RuntimeException e) {
            LOG.debug("Legacy Connector: could not look for a packaged mod in {}", archive, e);
        }
        return null;
    }

    /**
     * A zip whose whole content sits in one top-level folder ("Advanced Survival/mymod/Main.class", "Advanced Survival/assets/...", often next to a
     * __MACOSX folder): Forge's zip reader took classes by their package, but assets and mcmod.info under the folder were never found. Repacked here with
     * the folder removed; null when the archive is not shaped like that.
     */
    private static Path unwrapRootFolder(Path archive, Path nestedDir) {
        if (!archive.getFileName().toString().toLowerCase().endsWith(".zip")) return null;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive.toFile())) {
            String root = null;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                String n = entries.nextElement().getName();
                if (n.startsWith("__MACOSX/") || n.endsWith(".DS_Store")) continue;
                int slash = n.indexOf('/');
                if (slash <= 0) return null;
                String top = n.substring(0, slash + 1);
                if (root == null) root = top;
                else if (!root.equals(top)) return null;
            }
            if (root == null || root.equalsIgnoreCase("META-INF/") || root.equals("assets/") || root.equals("mods/") || root.equals("net/") || root.equals("cpw/")) return null;
            // a mod's own package folder is not a wrapper: the folder has to hold the archive's root files (assets, mcmod.info)
            if (zip.getEntry(root + "assets/") == null && zip.getEntry(root + "mcmod.info") == null && zip.getEntry(root + "pack.mcmeta") == null) return null;
            Files.createDirectories(nestedDir);
            Path target = nestedDir.resolve(archive.getFileName().toString().replaceAll("[.]zip$", "") + "-unwrapped.jar");
            try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(target))) {
                entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    String n = e.getName();
                    if (n.startsWith("__MACOSX/") || n.endsWith(".DS_Store") || n.equals(root) || e.isDirectory()) continue;
                    out.putNextEntry(new ZipEntry(n.substring(root.length())));
                    try (var in = zip.getInputStream(e)) {
                        in.transferTo(out);
                    }
                    out.closeEntry();
                }
            }
            return target;
        } catch (IOException | RuntimeException e) {
            LOG.debug("Legacy Connector: could not unwrap the root folder of {}", archive, e);
            return null;
        }
    }

    private static boolean forge1201NoticeLogged;

    /** Forge 1.20.1 support is experimental but on by default; {@code experimental.forge1201=false} (config) or {@code -Dlegacyconnector.forge1201=false} turns it off. */
    /** The jar holds CodeChickenLib's {@code CCRenderState} (its own package or a repackaged copy): static scratch state shared by every renderer built on it. */
    private static boolean sharesStaticRenderState(Path jar) {
        if (jar == null || !java.nio.file.Files.isRegularFile(jar)) return false;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith("/lib/render/CCRenderState.class")) return true;
            }
        } catch (java.io.IOException | RuntimeException e) {
            // an unreadable jar is reported elsewhere
        }
        return false;
    }

    private static boolean forge1201Enabled(Path modsDir) {
        System.setProperty("legacyconnector.forge1201.softlink", Boolean.toString(LibraryBridges.forge1201SoftLinkOn(modsDir)));
        return LibraryBridges.forge1201On(modsDir);
    }

    @Override
    public int getPriority() {
        return 100; // before ModsFolderLocator (0)
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path modsDir = FMLPaths.MODSDIR.get();
        Path cacheDir = gameDir.resolve(".legacyconnector").resolve("cache");
        try {
            Files.createDirectories(cacheDir);
            currentSide = side();
            Path runtimeJar = extractRuntime(cacheDir);
            if (runtimeJar != null) {
                pipeline.addPath(runtimeJar, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }

            LOG.info("Legacy Connector {} (loader) starting", Provenance.translatorVersion());
            ShimIndex index = loadShimIndex(runtimeJar);
            LOG.info("Legacy Connector: shim index has {} legacy API classes", index.size());

            unreadableNotes.clear();
            List<LegacyJar> legacy = new ArrayList<>();
            java.util.Set<String> seenNames = new java.util.HashSet<>();
            // FML also loaded mods/<minecraft version>/ (where dependency downloaders such as CodeChickenCore's put
            // CodeChickenLib); a jar present in both places is taken from mods/. These folders are also where jars that ship
            // launcher services (a shaded Mixin, as many 1.12.2 mods have) must go: NeoForge's early scan of mods/ itself
            // loads such jars as launcher services before anything can translate them, and the game stops.
            // mods/b1.7.3/: Beta 1.7.3 ModLoader/Forge mods (BetaJars)
            for (Path dir : List.of(modsDir, modsDir.resolve("1.4.7"), modsDir.resolve("1.5.2"), modsDir.resolve("1.6.4"), modsDir.resolve("1.7.10"), modsDir.resolve("1.12.2"), modsDir.resolve("1.20.1"), modsDir.resolve(BetaJars.FOLDER))) {
                if (!Files.isDirectory(dir)) continue;
                try (Stream<Path> files = Files.list(dir)) {
                    for (Path p0 : files.filter(f -> isModArchive(f))
                            .sorted(Comparator.comparing(f -> f.getFileName().toString().toLowerCase())).toList()) {
                        Path p = p0;
                        if (context.isLocated(p) || !seenNames.add(p.getFileName().toString().toLowerCase())) continue;
                        try {
                            Path packaged = packagedMod(p, gameDir.resolve(".legacyconnector").resolve("nested"));
                            if (packaged == null) packaged = unwrapRootFolder(p, gameDir.resolve(".legacyconnector").resolve("nested"));
                            LegacyJar jar = LegacyJarInspector.inspect(packaged != null ? packaged : p);
                            if (jar == null && LegacyJarInspector.takeDerivative(p)) {
                                // a translated jar taken out of a cache: only Legacy Connector itself loads those, from its own cache
                                context.addLocated(p);
                                LOG.warn("Legacy Connector: {} is a translated copy of another mod made by Legacy Connector (not an original). "
                                        + "It is not loaded; put the original mod in mods/ instead and Legacy Connector translates it here.", p.getFileName());
                                continue;
                            }
                            if (jar != null && jar.version == LegacyVersion.V1_20_1 && !forge1201Enabled(modsDir)) {
                                if (!forge1201NoticeLogged) {
                                    forge1201NoticeLogged = true;
                                    LOG.info("Legacy Connector: Forge 1.20.1 mods (such as {}) are left alone: experimental.forge1201=false in config/legacyconnector.properties turns that support off. "
                                            + "Remove the line (or set it to true) to load them.", p.getFileName());
                                }
                                continue;
                            }
                            if (jar != null && jar.version != null && dir.equals(modsDir) && autoSortMods(modsDir)) {
                                Path moved = moveIntoVersionFolder(p, modsDir, jar.version);
                                if (moved != null) {
                                    if (jar.path != null && jar.path.equals(p)) jar.path = moved;
                                    p = moved;
                                }
                            }
                            if (jar != null) legacy.add(jar);
                            addContainedDeps(p, gameDir.resolve(".legacyconnector").resolve("nested"), legacy, seenNames);
                        } catch (IOException | RuntimeException e) {
                            // a truncated or corrupt archive: skipped with a warning and a report line, the other mods are translated as usual
                            noteUnreadable(p, e);
                        }
                    }
                }
                // legacy mods distributed as a folder (MCHeli): packed into a jar, the legacy container's source stays the folder
                for (Path[] packed : DirectoryMods.pack(dir, gameDir.resolve(".legacyconnector").resolve("dirmods"))) {
                    if (!seenNames.add(packed[0].getFileName().toString().toLowerCase())) continue;
                    try {
                        LegacyJar jar = LegacyJarInspector.inspect(packed[0]);
                        if (jar == null) continue;
                        jar.sourceDir = packed[1];
                        legacy.add(jar);
                    } catch (IOException | RuntimeException e) {
                        LOG.debug("Legacy Connector: could not inspect {}", packed[1], e);
                    }
                }
            }
            if (legacy.isEmpty()) return;
            List<SideBySide.Unit> crossVersionUnits = dropDuplicateMods(legacy, SideBySide.enabled(LibraryBridges.readConfig(modsDir)));

            List<LibraryBridges.Bridge> bridges = LibraryBridges.active(modsDir);
            SideBySide.setCurrent(SideBySide.NONE);
            NativeLibraries1201.setIsolated(Set.of());
            Map<LegacyJar, String> sideBySideNotes = Map.of();
            {
                SideBySidePlanner.Result sbs = SideBySidePlanner.plan(legacy, forge1201Enabled(modsDir) ? LibraryBridges.installedModernIds() : Set.of(),
                        LibraryBridges.readConfig(modsDir), crossVersionUnits);
                SideBySide.setCurrent(sbs.plan());
                sideBySideNotes = sbs.notes();
                for (String line : sbs.notes().values()) LOG.info("Legacy Connector:{}", line.stripTrailing());
            }
            if (forge1201Enabled(modsDir)) for (String missing : NativeLibraries1201.missing(legacy, LibraryBridges.installedModernIds())) LOG.warn("Legacy Connector: {}", missing);
            // Scala, trove, ... are handed to FML only when a legacy mod uses them and nothing else in the pack provides them.
            Set<Path> libraryKeep = new HashSet<>();
            LIBRARY_SKIPS.clear();
            BundledLibraries.Result libraries = BundledLibraries.provide(pipeline, cacheDir,
                    legacy.stream().filter(j -> skipReason(j) == null && LibraryBridges.replacing(j, bridges) == null && LibraryBridges.obsoleteReason(j) == null).toList(),
                    List.of(modsDir, modsDir.resolve("1.4.7"), modsDir.resolve("1.5.2"), modsDir.resolve("1.6.4"), modsDir.resolve("1.7.10"), modsDir.resolve("1.12.2")), LOADER_DIGEST, libraryKeep);
            LIBRARY_SKIPS.putAll(libraries.unsupported());
            // b1.7.3 mods released as a client and a server build: the build for this side loads
            LIBRARY_SKIPS.putAll(BetaJars.selectBuilds(legacy, side()));
            libraryNotes = libraries.notes();
            liteNotes = liteLoaderNotes(List.of(modsDir, modsDir.resolve("1.5.2"), modsDir.resolve("1.6.4"), modsDir.resolve("1.7.10"), modsDir.resolve("1.12.2")));
            LegacyVersion.setBridgedRoots(bridges.stream().map(LibraryBridges.Bridge::apiRoot).toList());
            List<String> present = new ArrayList<>(legacy.stream().flatMap(j -> j.mods.stream()).map(m -> m.legacyId).toList());
            for (LibraryBridges.Bridge b : bridges) if (b.legacyModId() != null) present.add(b.legacyModId());
            // APIs a jar declares (@API in a package-info) were mods to FML: @Optional and dependencies name them
            // (Forestry's BuildCraft pipe code is @Optional.Method(modid = "BuildCraftAPI|transport")).
            java.util.Map<String, String> apis = new java.util.LinkedHashMap<>();
            java.util.Set<String> apiPackages = new java.util.LinkedHashSet<>();
            for (LegacyJar j : legacy) for (String[] api : j.apis) {
                apis.putIfAbsent(api[0], api[1] == null ? "" : api[1]);
                apiPackages.add(api[3]);
            }
            present.addAll(apis.keySet());
            System.setProperty("legacyconnector.legacyApis", apis.entrySet().stream().map(e -> e.getKey() + "@" + e.getValue())
                    .collect(java.util.stream.Collectors.joining(",")));
            System.setProperty("legacyconnector.legacyApiPackages", String.join(",", apiPackages));
            // Coremods decide what to strip while the jars are translated, before any mod is registered with the game.
            System.setProperty("legacyconnector.presentLegacyMods", String.join(",", present));
            // A render library that keeps its state in statics (CodeChickenLib's CCRenderState, also repackaged inside other mods): block, tile entity and item
            // renderers built on it must not run at once, which they do when chunks mesh on worker threads while the render thread draws items.
            if (System.getProperty("legacyconnector.renderLock") == null) {
                for (LegacyJar j : legacy) {
                    if (skipReason(j) == null && j.version != LegacyVersion.V1_12_2 && j.version != LegacyVersion.V1_20_1 && sharesStaticRenderState(j.path)) {
                        System.setProperty("legacyconnector.renderLock", "true");
                        LOG.info("Legacy Connector: {} has a render library with static state; legacy renderers on the render thread take the capture lock", j.path.getFileName());
                        break;
                    }
                }
            }
            // Read by the runtime's Loader.isModLoaded: bridged libraries count as loaded.
            System.setProperty("legacyconnector.bridgedMods", String.join(",", bridges.stream().map(LibraryBridges.Bridge::legacyModId)
                    .filter(java.util.Objects::nonNull).toList()));
            if (!bridges.isEmpty()) LOG.info("Legacy Connector: bridging {} to their modern replacements", bridges);
            // Mods whose required dependency is not installed do not run: FML would not have started them either.
            MISSING_DEPENDENCIES.clear();
            {
                List<LegacyJar> checked = legacy.stream().filter(j -> skipReason(j) == null && LibraryBridges.replacing(j, bridges) == null
                        && LibraryBridges.obsoleteReason(j) == null).toList();
                Set<String> have = LegacyDependencies.presentIds(legacy, bridges, LibraryBridges.seenModernIds(), apis.keySet());
                StringBuilder lines = new StringBuilder();
                for (LegacyDependencies.Problem p : LegacyDependencies.check(checked, have, bridges, "CLIENT".equals(side()))) {
                    LegacyJar owner = checked.stream().filter(j -> j.mods.contains(p.mod())).findFirst().orElse(null);
                    if (p.fatal()) {
                        boolean client = "CLIENT".equals(side());
                        LOG.warn("Legacy Connector: {} (the mod is still loaded)", p.message());
                        // A warning, not an error: FML shows its loading-warnings screen with a Proceed button; the mod is still loaded (it may work without the dependency).
                        // A dedicated server has no screen: the log line and the report section are what it gets.
                        if (client && owner != null) {
                            pipeline.addIssue(ModLoadingIssue.warning(LegacyDependencies.issueKey(p),
                                    LegacyDependencies.issueArgs(p, owner.version.id())).withAffectedPath(owner.path));
                        }
                        MISSING_DEPENDENCIES.computeIfAbsent(owner, k -> new ArrayList<>()).add(p.message());
                        lines.append(p.mod().legacyId).append("\t").append(p.message()).append("\n");
                    } else {
                        LOG.warn("Legacy Connector: {}", p.message());
                        if (owner != null) owner.notes.add(p.message());
                    }
                }
                System.setProperty("legacyconnector.missingDependencies", lines.toString());
            }
            String side = side();
            LegacyJarTransformer transformer = new LegacyJarTransformer(index, LegacyModLocator.class.getClassLoader()).presentMods(present).side(side);
            // Legacy coremods' class transformers, loaded on the first cache miss (only then is anything translated).
            List<LegacyJar> loading = legacy.stream()
                    .filter(j -> skipReason(j) == null && LibraryBridges.replacing(j, bridges) == null && LibraryBridges.obsoleteReason(j) == null).toList();
            // The runtime wires the Forge 1.20.1 facades (events, capabilities, networking) for translated
            // mods and translated Forge 1.20.1 mods nested in their Jar-in-Jar archives.
            Set<String> forge1201Mods = new java.util.LinkedHashSet<>();
            Set<String> nestedForge1201Mods = new java.util.LinkedHashSet<>();
            Path nestedModScanDir = gameDir.resolve(".legacyconnector").resolve("nested-mod-scan");
            for (LegacyJar jar : loading) {
                if (jar.version != LegacyVersion.V1_20_1) continue;
                for (LegacyModDecl mod : jar.mods) {
                    String id = mod.fmlId();
                    if (id != null && !id.isBlank()) forge1201Mods.add(id);
                }
                Set<String> nestedIds = nestedForge1201ModIds(jar, nestedModScanDir);
                nestedForge1201Mods.addAll(nestedIds);
            }
            forge1201Mods.addAll(nestedForge1201Mods);
            System.setProperty("legacyconnector.forge1201.mods", String.join(",", forge1201Mods));
            if (!nestedForge1201Mods.isEmpty()) {
                LOG.info("Legacy Connector: Forge 1.20.1 facade event routing includes nested mod IDs: {}", nestedForge1201Mods);
            }
            // One coremod class loader per API flavour (1.7.10; 1.6.4 and 1.5.2), loaded on the first class of a jar that uses it.
            var coremods = new java.util.EnumMap<LegacyVersion, org.kylepitcock.legacyconnector.loader.transform.LegacyCoreModTransformers>(LegacyVersion.class);
            transformer.patcher((jar, name, bytes) -> {
                if (jar.version.family() != LegacyVersion.V1_7_10) return bytes;
                LegacyVersion api = org.kylepitcock.legacyconnector.loader.transform.LegacyCoreModTransformers.apiFor(jar.version);
                // Legacy coremod transformers keep their own state and were written for one thread: one class at a time.
                synchronized (coremods) {
                    var transformers = coremods.get(api);
                    if (transformers == null) {
                        transformers = org.kylepitcock.legacyconnector.loader.transform.LegacyCoreModTransformers.load(loading, cacheDir, gameDir, side, runtimeJar, api);
                        coremods.put(api, transformers);
                    }
                    return transformers.apply(name, bytes);
                }
            });
            LegacyModDecl.setFmlIds(fmlIdRenames(loading, legacy, List.of(modsDir, modsDir.resolve("1.4.7"), modsDir.resolve("1.5.2"), modsDir.resolve("1.6.4"), modsDir.resolve("1.7.10"), modsDir.resolve("1.12.2"))));
            if (!LegacyModDecl.fmlIds().isEmpty()) LOG.info("Legacy Connector: mod ids a modern mod also uses; the legacy mods load as {}", LegacyModDecl.fmlIds());
            org.kylepitcock.legacyconnector.loader.meta.ModMetadataGenerator.setLoadedMods(legacy.stream()
                    .filter(j -> skipReason(j) == null && LibraryBridges.replacing(j, bridges) == null && LibraryBridges.obsoleteReason(j) == null)
                    .flatMap(j -> j.mods.stream()).map(LegacyModDecl::fmlId).toList());
            Map<LegacyJar, String> outcomes = new LinkedHashMap<>();
            List<Path> translated = new ArrayList<>();
            java.util.Set<Path> ownerKeeping = new java.util.HashSet<>(); // 1.4.7 jars: Forestry (and Binnie's mods) look their own jar up by file
            Set<Path> keep = new HashSet<>(libraryKeep);
            if (runtimeJar != null) keep.add(runtimeJar);
            for (LegacyVersion api : List.of(LegacyVersion.V1_7_10, LegacyVersion.V1_6_4)) {
                keep.add(cacheDir.resolve(org.kylepitcock.legacyconnector.loader.transform.LegacyCoreModTransformers.apiJarName(api)));
            }
            // Mods link against each other (RotaryCraft extends DragonAPI classes), so a jar's translation depends on
            // every legacy jar of its version: that set is part of the cache key, and peers are scanned on a miss.
            var crossVersion = crossVersionPackages(loading, legacy, List.of(modsDir, modsDir.resolve("1.4.7"), modsDir.resolve("1.5.2"), modsDir.resolve("1.6.4"), modsDir.resolve("1.7.10"), modsDir.resolve("1.12.2")));
            if (!crossVersion.modernClashes().isEmpty()) {
                Set<String> pkgs = crossVersion.modernClashes();
                LOG.info("Legacy Connector: {} packages that legacy jars share with modern mods are relocated in the legacy jars: {}",
                        pkgs.size(), pkgs.size() > 12 ? new ArrayList<>(pkgs).subList(0, 12) + " ..." : pkgs);
            }
            if (!crossVersion.isEmpty()) {
                transformer.crossVersion(crossVersion);
                for (LegacyVersion v : LegacyVersion.values()) {
                    Set<String> pkgs = crossVersion.relocated(v);
                    if (!pkgs.isEmpty()) LOG.info("Legacy Connector: {} packages that {} jars share with another legacy version are relocated in its jars: {}",
                            pkgs.size(), v.id(), pkgs.size() > 12 ? new ArrayList<>(pkgs).subList(0, 12) + " ..." : pkgs);
                }
            }
            Map<LegacyVersion, Set<String>> bundledElsewhere = classesOfOtherVersions(legacy);
            transformer.bundledByOtherVersions(bundledElsewhere);
            Map<LegacyVersion, byte[]> modSet = modSetDigests(legacy, bridges, crossVersion, bundledElsewhere);
            Map<LegacyVersion, Map<String, ClassInfo>> peers = new EnumMap<>(LegacyVersion.class);
            Map<LegacyVersion, SynthDemand> demand = new EnumMap<>(LegacyVersion.class);
            List<RiskyMods.Entry> risky = new ArrayList<>();
            int uncached = 0;
            for (LegacyJar jar : legacy) {
                if (skipReason(jar) != null || LibraryBridges.replacing(jar, bridges) != null || LibraryBridges.obsoleteReason(jar) != null) continue;
                Path cached = cacheDir.resolve(cacheName(jar, modSet.get(jar.version)));
                if (!Files.exists(cached) || !Files.exists(cached.resolveSibling(cached.getFileName() + ".synth.txt"))) uncached++;
            }
            TranslationProgress progress = new TranslationProgress(uncached);
            // The jars that are not cached are independent of each other once all classes have been scanned: translate them in parallel.
            TranslationPool pool = new TranslationPool(transformer, TranslationPool.defaultThreads(uncached));
            if (uncached > 1) {
                progress.step("reading all legacy jars");
                for (LegacyJar jar : legacy) {
                    if (skipReason(jar) != null || LibraryBridges.replacing(jar, bridges) != null || LibraryBridges.obsoleteReason(jar) != null) continue;
                    Path out = cacheDir.resolve(cacheName(jar, modSet.get(jar.version)));
                    if (Files.exists(out) && Files.exists(out.resolveSibling(out.getFileName() + ".synth.txt"))) continue;
                    Map<String, ClassInfo> jarPeers = peers.computeIfAbsent(jar.version, v -> scanPeers(transformer, legacy, v, bridges));
                    pool.submit(jar, out, jarPeers);
                }
            }
            for (LegacyJar jar : legacy) {
                // Claim the original even if we end up skipping it, so FML does not raise its own
                // "this is an old Forge mod" error on top of ours.
                context.addLocated(jar.path);
                LibraryBridges.Bridge replacedBy = LibraryBridges.replacing(jar, bridges);
                String obsolete = LibraryBridges.obsoleteReason(jar);
                String skip = replacedBy != null ? ("native1201".equals(replacedBy.configKey()) ? "native bridge: " : "") + "replaced by " + (replacedBy.modernModId() == null ? "NeoForge" : replacedBy.modernModId()) + " (legacy " + replacedBy.legacyModId()
                        + " API bridged)" : obsolete != null ? "not loaded: " + obsolete : skipReason(jar);
                if (skip != null) {
                    outcomes.put(jar, skip);
                    // Only logged: a ModLoadingIssue opens FML's warning screen (Crash Assistant reads it as a crash). Bridged libraries are expected.
                    if (replacedBy == null && obsolete == null) LOG.warn("Legacy Connector skipped {}: {}", jar.fileName(), skip);
                    continue;
                }
                boolean translating = false;
                try {
                    Path out = cacheDir.resolve(cacheName(jar, modSet.get(jar.version)));
                    Path linkReport = out.resolveSibling(out.getFileName() + ".links.txt");
                    Path demandFile = out.resolveSibling(out.getFileName() + ".synth.txt");
                    Path riskFile = out.resolveSibling(out.getFileName() + ".risk.txt");
                    if (!Files.exists(out) || !Files.exists(demandFile)) {
                        translating = true;
                        if (!peers.containsKey(jar.version)) progress.step("reading all legacy jars");
                        Map<String, ClassInfo> jarPeers = peers.computeIfAbsent(jar.version, v -> scanPeers(transformer, legacy, v, bridges));
                        progress.starting(jar.fileName());
                        long start = System.nanoTime();
                        LegacyJarTransformer.Result result = pool.take(jar, out, jarPeers);
                        result.demand.write(demandFile);
                        LOG.info("Legacy Connector: translated {} ({} classes, {} JDK fixes, {} virtual fields, {} synthesized API classes) in {} ms",
                                jar.fileName(), result.classes, result.compatRedirects, result.virtualFieldRedirects,
                                result.links.synthesizedClasses.size(), (System.nanoTime() - start) / 1_000_000);
                        StringBuilder text = new StringBuilder();
                        for (String n : result.notes) text.append("  note: ").append(n).append('\n');
                        text.append("  ").append(result.links.errorCount()).append(" unresolved link(s)")
                                .append(result.links.errorCount() == 0 ? " - every legacy API reference links\n" : ":\n");
                        text.append(result.links.render());
                        Files.writeString(linkReport, text);
                        try {
                            RiskyMods.write(riskFile, RiskyMods.summarize(result.links));
                        } catch (IOException | RuntimeException e) {
                            LOG.debug("Legacy Connector: could not write the risk summary of {}", jar.fileName(), e);
                        }
                        if (result.links.errorCount() > 0) {
                            LOG.warn("Legacy Connector: {} has {} reference(s) that cannot be linked; see logs/legacyconnector-report.txt",
                                    jar.fileName(), result.links.errorCount());
                        }
                    }
                    demand.computeIfAbsent(jar.version, v -> new SynthDemand()).merge(SynthDemand.read(demandFile));
                    keep.add(out);
                    keep.add(linkReport);
                    keep.add(demandFile);
                    keep.add(riskFile);
                    RiskyMods.Summary risk = RiskyMods.read(riskFile);
                    if (risk != null) risky.add(RiskyMods.entry(jar, risk));
                    outcomes.put(jar, sideBySideNotes.getOrDefault(jar, "") + (Files.exists(linkReport) ? Files.readString(linkReport) : "  (cached, no link report)\n"));
                    translated.add(out);
                    if (jar.version != null && jar.version.flatNames()) ownerKeeping.add(out);
                } catch (Exception e) {
                    LOG.error("Legacy Connector: failed to translate {}", jar.fileName(), e);
                    outcomes.put(jar, "translation failed: " + e);
                    pipeline.addIssue(ModLoadingIssue.error("Legacy Connector could not translate " + jar.fileName())
                            .withAffectedPath(jar.path).withCause(e));
                }
                if (translating) progress.finished();
            }
            progress.close();
            pool.close();

            // One generated jar per legacy version with every API class/member the shim lacks, for all mods at once
            // (mods pass these types to each other, so there must be exactly one copy of each).
            for (var e : demand.entrySet()) {
                if (e.getValue().isEmpty()) continue;
                Path synthJar = buildSynthJar(cacheDir, e.getKey(), index, e.getValue());
                keep.add(synthJar);
                pipeline.addPath(synthJar, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }

            SplitPackageResolver.Outcome split = SplitPackageResolver.resolve(translated, cacheDir, ownerKeeping);
            if (split.sharedLibrary() != null) {
                LOG.info("Legacy Connector: {} packages shipped by several legacy jars were merged into {}",
                        split.sharedPackages().size(), split.sharedLibrary().getFileName());
                keep.add(split.sharedLibrary());
                pipeline.addPath(split.sharedLibrary(), ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }
            for (Path p : split.jars()) {
                keep.add(p);
                pipeline.addPath(p, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }
            writeReport(gameDir, outcomes, split);
            warnRiskyMods(pipeline, gameDir, modsDir, risky);
            pruneCache(cacheDir, keep);
        } catch (IOException e) {
            LOG.error("Legacy Connector: legacy mod discovery failed", e);
        } finally {
            releaseLoaderData(modsDir);
        }
    }

    /**
     * One consolidated loading warning for the risky mods (unresolved references, heavy stub use), shown once per distinct set:
     * the hash of the set is recorded when the warning is emitted (client: the loading screen with a Continue button; server: the log).
     * {@code warnings.riskyMods = false} turns it off. Never fails startup.
     */
    private static void warnRiskyMods(IDiscoveryPipeline pipeline, Path gameDir, Path modsDir, List<RiskyMods.Entry> risky) {
        try {
            if (risky.isEmpty()) return;
            if ("false".equalsIgnoreCase(LibraryBridges.readConfig(modsDir).getProperty("warnings.riskyMods", "true").trim())) return;
            Path state = gameDir.resolve(".legacyconnector");
            String hash = RiskyMods.hash(risky);
            if (RiskyMods.acknowledged(state, hash)) {
                LOG.info("Legacy Connector: {} risky mod(s) already acknowledged (warning not shown again until the set changes)", risky.size());
                return;
            }
            String text = RiskyMods.message(risky);
            if (text == null) return;
            // Not a ModLoadingIssue: any loading issue opens FML's warning/error screen, which tools such as Crash Assistant
            // take for a crash on every start. The runtime shows the text once in chat when the player joins a world.
            LOG.warn("Legacy Connector: {}", text.replace('\n', ' '));
            Files.createDirectories(state);
            Files.writeString(state.resolve(RiskyMods.NOTICE_FILE), text, java.nio.charset.StandardCharsets.UTF_8);
            RiskyMods.acknowledge(state, hash);
        } catch (IOException | RuntimeException e) {
            LOG.debug("Legacy Connector: risky mods summary failed", e);
        }
    }

    /**
     * {@code perf.releaseLoaderData} (default on): drops the static tables translation filled (legacy hierarchy, methods, fields,
     * MCP names, library class infos). This service's classes live as long as the game, and nothing reads the tables after
     * discovery; a later lookup would read them again.
     */
    private static void releaseLoaderData(Path modsDir) {
        try {
            if ("false".equalsIgnoreCase(LibraryBridges.readConfig(modsDir).getProperty("perf.releaseLoaderData", "true").trim())) return;
            org.kylepitcock.legacyconnector.loader.synth.SynthCaches.release();
            org.kylepitcock.legacyconnector.loader.mapping.McpMappings.releaseCaches();
            org.kylepitcock.legacyconnector.loader.transform.Hierarchy.releaseCaches();
            HASHED.clear();
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("Legacy Connector: could not release the loader's tables", e);
        }
    }

    private static Map<String, ClassInfo> scanPeers(LegacyJarTransformer transformer, List<LegacyJar> legacy, LegacyVersion version, List<LibraryBridges.Bridge> bridges) {
        org.kylepitcock.legacyconnector.loader.transform.PeerClasses out = new org.kylepitcock.legacyconnector.loader.transform.PeerClasses();
        for (LegacyJar jar : legacy) {
            if (jar.version != version || skipReason(jar) != null) continue;
            // a library replaced by its native build, or one that is not loaded at all, is not a class another jar will extend at run time
            if (version == LegacyVersion.V1_20_1 && (LibraryBridges.replacing(jar, bridges) != null || LibraryBridges.obsoleteReason(jar) != null)) continue;
            TranslationPool.heartbeat();
            try {
                out.add(transformer.scan(jar));
            } catch (IOException | RuntimeException e) {
                LOG.debug("Legacy Connector: could not scan {}", jar.fileName(), e);
            }
        }
        return out.classes();
    }

    private static final java.util.regex.Pattern TOML_MOD_ID = java.util.regex.Pattern.compile("(?m)^\s*modId\s*=\s*\"([^\"]+)\"");

    /**
     * Legacy modern ids that a modern jar of the pack declares or names as a dependency, mapped to the FML id the legacy
     * mod gets instead ({@code witchery -> witchery_legacy}; see {@link LegacyModDecl#fmlId()}). Addons of Legacy
     * Connector (jars depending on it) mean the legacy mods and are not counted.
     */
    /**
     * A jar, or a zip: Forge 1.6.4 and older loaded mods from zips, and 1.4.x mods (RedPower 2, ModLoader ones above all) were mostly distributed that way,
     * so they are dropped into {@code mods/} as often as into {@code mods/1.4.7/}. Zips that are not mods (resource packs, shader packs) are rejected
     * by the inspector without a message.
     */
    private static boolean isModArchive(Path f) {
        String name = f.getFileName().toString().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip");
    }

    private static Map<String, String> fmlIdRenames(List<LegacyJar> loading, List<LegacyJar> allLegacy, List<Path> dirs) {
        Set<String> legacyIds = new HashSet<>();
        for (LegacyJar j : loading) for (LegacyModDecl m : j.mods) legacyIds.add(m.modernId());
        if (legacyIds.isEmpty()) return Map.of();
        Set<Path> legacyPaths = new HashSet<>();
        for (LegacyJar j : allLegacy) {
            legacyPaths.add(j.path.toAbsolutePath().normalize());
            legacyPaths.add(j.existing().toAbsolutePath().normalize());
        }
        Set<String> mentioned = new HashSet<>();
        for (Path dir : dirs) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.filter(f -> f.getFileName().toString().toLowerCase().endsWith(".jar")).toList()) {
                    if (p.getFileName().toString().toLowerCase().startsWith("legacyconnector") || legacyPaths.contains(p.toAbsolutePath().normalize())) continue;
                    try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(p.toFile())) {
                        var e = zip.getEntry("META-INF/neoforge.mods.toml");
                        if (e == null) continue;
                        String toml = new String(zip.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8);
                        Set<String> ids = new HashSet<>();
                        var m = TOML_MOD_ID.matcher(toml);
                        while (m.find()) ids.add(m.group(1));
                        if (ids.contains(org.kylepitcock.legacyconnector.loader.meta.ModMetadataGenerator.RUNTIME_MOD_ID)) continue;
                        mentioned.addAll(ids);
                    } catch (IOException | RuntimeException e) {
                        LOG.debug("Legacy Connector: could not read the mod ids of {}", p, e);
                    }
                }
            } catch (IOException e) {
                LOG.debug("Legacy Connector: could not list {}", dir, e);
            }
        }
        Map<String, String> out = new java.util.TreeMap<>();
        for (String id : legacyIds) {
            if (!mentioned.contains(id)) continue;
            String fml = id + "_legacy";
            for (int n = 2; legacyIds.contains(fml) || mentioned.contains(fml); n++) fml = id + "_legacy" + n;
            out.put(id, fml);
        }
        return out;
    }

    /** The class packages that jars of more than one legacy version define (see {@link CrossVersionPackages}). */
    private static org.kylepitcock.legacyconnector.loader.transform.CrossVersionPackages crossVersionPackages(List<LegacyJar> jars, List<LegacyJar> allLegacy, List<Path> modernDirs) {
        var out = new org.kylepitcock.legacyconnector.loader.transform.CrossVersionPackages();
        Set<Path> legacyPaths = new HashSet<>();
        Set<String> modPackages = new HashSet<>();
        for (LegacyJar j : allLegacy) {
            legacyPaths.add(j.path.toAbsolutePath().normalize());
            legacyPaths.add(j.existing().toAbsolutePath().normalize());
        }
        for (LegacyJar j : jars) {
            for (LegacyModDecl m : j.mods) {
                if (m.mainClass != null && m.mainClass.lastIndexOf('/') > 0) modPackages.add(m.mainClass.substring(0, m.mainClass.lastIndexOf('/')));
            }
        }
        for (LegacyJar jar : jars) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.existing().toFile())) {
                var en = zip.entries();
                while (en.hasMoreElements()) {
                    String n = en.nextElement().getName();
                    if (!n.endsWith(".class") || n.startsWith("META-INF/") || n.lastIndexOf('/') <= 0) continue;
                    String name = n.substring(0, n.length() - ".class".length());
                    if (LegacyVersion.isRelocatedRoot(name)) continue; // Minecraft, Forge and bridged APIs go to each version's shim anyway
                    if (org.kylepitcock.legacyconnector.loader.transform.LegacyJarTransformer.isProvidedLibrary(name)) continue; // the game's copy serves it (lang3, Guava...): never relocated
                    out.add(jar.version, name);
                }
            } catch (IOException | RuntimeException e) {
                LOG.debug("Legacy Connector: could not list {}", jar.fileName(), e);
            }
        }
        // modern mods' packages, as internal names (the scan reports a.b.c)
        Set<String> modern = new HashSet<>();
        for (String pkg : BundledLibraries.scanProviders(modernDirs, legacyPaths).keySet()) modern.add(pkg.replace('.', '/'));
        out.modern(modern, modPackages);
        return out.done();
    }

    /**
     * For each legacy version, the class names (internal form) that the jars of the OTHER versions in this game hold; empty when only one version is
     * present. Read from the zip directories only.
     */
    private static Map<LegacyVersion, Set<String>> classesOfOtherVersions(List<LegacyJar> legacy) {
        Map<LegacyVersion, Set<String>> own = new EnumMap<>(LegacyVersion.class);
        for (LegacyJar jar : legacy) {
            if (jar.version == null || skipReason(jar) != null) continue;
            Set<String> names = own.computeIfAbsent(jar.version, v -> new HashSet<>());
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.existing().toFile())) {
                var en = zip.entries();
                while (en.hasMoreElements()) {
                    String n = en.nextElement().getName();
                    if (n.endsWith(".class") && n.indexOf('/') > 0) names.add(n.substring(0, n.length() - ".class".length()));
                }
            } catch (IOException | RuntimeException e) {
                LOG.debug("Legacy Connector: could not list {}", jar.fileName(), e);
            }
        }
        Map<LegacyVersion, Set<String>> out = new EnumMap<>(LegacyVersion.class);
        if (own.size() < 2) return out;
        for (LegacyVersion v : own.keySet()) {
            Set<String> others = new HashSet<>();
            own.forEach((ov, names) -> {
                if (ov != v) others.addAll(names);
            });
            out.put(v, others);
        }
        return out;
    }

    private static Map<LegacyVersion, byte[]> modSetDigests(List<LegacyJar> legacy, List<LibraryBridges.Bridge> bridges,
            org.kylepitcock.legacyconnector.loader.transform.CrossVersionPackages crossVersion, Map<LegacyVersion, Set<String>> bundledElsewhere) throws IOException {
        Map<LegacyVersion, MessageDigest> digests = new EnumMap<>(LegacyVersion.class);
        for (LegacyJar jar : legacy) {
            MessageDigest md = digests.computeIfAbsent(jar.version, v -> SplitPackageResolver.sha256());
            md.update(fileDigest(jar.existing()));
            md.update(bridges.toString().getBytes(StandardCharsets.UTF_8));
            md.update(side().getBytes(StandardCharsets.UTF_8)); // @SideOnly stripping differs per side
        }
        // the relocations depend on the other versions' jars, the FML ids on the modern mods
        byte[] shared = (crossVersion.fingerprint() + "|fmlIds=" + new java.util.TreeMap<>(LegacyModDecl.fmlIds()) + "|" + SideBySide.current().fingerprint()).getBytes(StandardCharsets.UTF_8);
        digests.forEach((v, md) -> md.update(shared));
        // the classes of the other versions' jars decide which optional references are stubbed as absent
        digests.forEach((v, md) -> md.update(Integer.toString(bundledElsewhere.getOrDefault(v, Set.of()).hashCode()).getBytes(StandardCharsets.UTF_8)));
        Map<LegacyVersion, byte[]> out = new EnumMap<>(LegacyVersion.class);
        digests.forEach((v, md) -> out.put(v, md.digest()));
        return out;
    }

    private record Hashed(long size, long modified, byte[] digest) {}
    private static final java.util.concurrent.ConcurrentHashMap<String, Hashed> HASHED = new java.util.concurrent.ConcurrentHashMap<>();

    private static String hex(byte[] b) {
        return HexFormat.of().formatHex(b);
    }

    /** SHA-256 of a file; the originals are hashed several times per start (cache names, mod set, signatures), so the result is kept. */
    private static byte[] fileDigest(Path p) throws IOException {
        long size = Files.size(p), modified = Files.getLastModifiedTime(p).toMillis();
        Hashed known = HASHED.get(p.toString());
        if (known != null && known.size == size && known.modified == modified) return known.digest;
        byte[] d = hashFile(p);
        HASHED.put(p.toString(), new Hashed(size, modified, d));
        return d;
    }

    private static byte[] hashFile(Path p) throws IOException {
        MessageDigest md = SplitPackageResolver.sha256();
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        return md.digest();
    }

    private static Path buildSynthJar(Path cacheDir, LegacyVersion version, ShimIndex index, SynthDemand demand) throws IOException {
        Path tmpDemand = Files.createTempFile(cacheDir, "demand", ".txt");
        try {
            demand.write(tmpDemand);
            MessageDigest md = SplitPackageResolver.sha256();
            md.update(Files.readAllBytes(tmpDemand));
            md.update(("v" + LegacyJarTransformer.TRANSFORMER_VERSION).getBytes(StandardCharsets.UTF_8));
            md.update(LOADER_DIGEST);
            if (runtimeDigest != null) md.update(runtimeDigest);
            if (indexDigest != null) md.update(indexDigest);
            Path target = cacheDir.resolve("legacyconnector-synth-" + version.id() + "-" + HexFormat.of().formatHex(md.digest()).substring(0, 16) + ".jar");
            if (Files.exists(target)) return target;
            long start = System.nanoTime();
            Map<String, byte[]> classes = new SynthJarBuilder(version, index, LegacyModLocator.class.getClassLoader(), demand).build();
            Manifest m = new Manifest();
            m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            m.getMainAttributes().putValue("FMLModType", "GAMELIBRARY");
            m.getMainAttributes().putValue("Automatic-Module-Name", "legacyconnector.synth.v" + version.id().replace('.', '_'));
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp); JarOutputStream jos = new JarOutputStream(os, m)) {
                for (var e : classes.entrySet()) {
                    ZipEntry z = new ZipEntry(e.getKey() + ".class");
                    z.setTime(0);
                    jos.putNextEntry(z);
                    jos.write(e.getValue());
                    jos.closeEntry();
                }
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            LOG.info("Legacy Connector: synthesized {} legacy {} API classes the shim does not implement yet ({} helpers) in {} ms",
                    classes.size(), version.id(), demand.helperCount(), (System.nanoTime() - start) / 1_000_000);
            return target;
        } finally {
            Files.deleteIfExists(tmpDemand);
        }
    }

    /** Translations of removed or updated mods would otherwise pile up forever. */
    private static void pruneCache(Path cacheDir, Set<Path> keep) {
        try (Stream<Path> files = Files.list(cacheDir)) {
            for (Path p : files.toList()) {
                if (keep.contains(p) || !Files.isRegularFile(p)) continue;
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    LOG.debug("Legacy Connector: could not prune {}", p, e);
                }
            }
        } catch (IOException e) {
            LOG.debug("Legacy Connector: could not prune cache", e);
        }
    }

    /** "CLIENT" or "SERVER". Dist is read reflectively: its class is not on the loader's compile classpath. */
    private static String side() {
        try {
            Object dist = net.neoforged.fml.loading.FMLLoader.class.getMethod("getDist").invoke(null);
            return String.valueOf(dist).equals("CLIENT") ? "CLIENT" : "SERVER";
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "CLIENT";
        }
    }

    /** Legacy jars that need a bundled library the pack makes impossible (see {@link BundledLibraries}). */
    private static final Map<LegacyJar, String> LIBRARY_SKIPS = java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());
    private static volatile List<String> libraryNotes = List.of();
    private static volatile List<String> liteNotes = List.of();

    /** Archives in the mod folders that could not be opened (truncated download, not a zip): listed in the report, never fatal. */
    private static final List<String> unreadableNotes = new java.util.concurrent.CopyOnWriteArrayList<>();

    static List<String> unreadableNotes() {
        return unreadableNotes;
    }

    static void noteUnreadable(Path archive, Exception e) {
        String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
        LOG.warn("Legacy Connector: skipped {}: it could not be read as an archive ({}). Download the file again; the other mods are not affected.",
                archive.getFileName(), reason);
        LOG.debug("Legacy Connector: unreadable archive {}", archive, e);
        unreadableNotes.add(archive.getFileName() + ": skipped, unreadable archive (" + reason + ")");
    }

    /** Forge loaded .zip mods too (1.6.4 and older distributed most mods as zips); the inspector rejects the zips that are not mods. */
    private static boolean isModArchive(String lowerCaseName) {
        return lowerCaseName.endsWith(".jar") || lowerCaseName.endsWith(".zip");
    }

    /**
     * {@code .litemod} files (LiteLoader mods: VoxelMap, Reis Minimap, ...) are not Forge mods: they need the LiteLoader launch wrapper and the
     * game client, which this layer does not provide. They are left alone and the report says so.
     */
    private static List<String> liteLoaderNotes(List<Path> dirs) {
        List<String> notes = new ArrayList<>();
        for (Path dir : dirs) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(f -> f.getFileName().toString().toLowerCase().endsWith(".litemod")).sorted().forEach(f -> {
                    LOG.info("Legacy Connector: {} is a LiteLoader mod, not a Forge mod; it is ignored", f.getFileName());
                    notes.add(f.getFileName() + ": LiteLoader mod (.litemod), not a Forge mod; ignored (LiteLoader is not supported)");
                });
            } catch (IOException e) {
                LOG.debug("Legacy Connector: could not list {}", dir, e);
            }
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(f -> isModArchive(f.getFileName().toString().toLowerCase())).sorted().forEach(f -> {
                    if (isPatchedGameJar(f)) {
                        LOG.warn("Legacy Connector: {} is a patched-game jar mod (it replaces Minecraft's own classes), not a Forge mod; it is ignored", f.getFileName());
                        notes.add(f.getFileName() + ": not supported: patched-game jar mod. It is installed by copying files over minecraft.jar and replaces Minecraft's own classes"
                                + " (Better Than Wolves and its add-ons are of this kind); this layer runs mods that add to the game, so the file is ignored");
                    }
                });
            } catch (IOException e) {
                LOG.debug("Legacy Connector: could not list {}", dir, e);
            }
        }
        return notes;
    }

    /**
     * A jar mod: a zip meant to be copied over minecraft.jar (folders such as {@code MINECRAFT-JAR/}, or obfuscated vanilla classes at the root) with no Forge mod
     * metadata. Such a patch replaces vanilla classes, which a translating layer cannot honour.
     */
    static boolean isPatchedGameJar(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            if (zip.getEntry("mcmod.info") != null || zip.getEntry("litemod.json") != null) return false;
            int rootShort = 0, minecraftDirs = 0;
            boolean modClass = false;
            for (var en = zip.entries(); en.hasMoreElements(); ) {
                String n = en.nextElement().getName();
                String up = n.toUpperCase(Locale.ROOT);
                if (up.startsWith("MINECRAFT-JAR/") || up.startsWith("MINECRAFT_SERVER-JAR/") || up.startsWith("MINECRAFT.JAR/")) minecraftDirs++;
                else if (n.endsWith(".class") && n.indexOf('/') < 0) {
                    if (n.length() <= 9) rootShort++;
                    else if (n.startsWith("mod_")) modClass = true;
                }
            }
            return minecraftDirs >= 10 || (rootShort >= 30 && !modClass);
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static volatile String currentSide = "CLIENT";

    /** The version folders under mods/ that Legacy Connector scans (a legacy jar belongs in the one named after its Minecraft version). */
    private static final java.util.Set<String> VERSION_FOLDERS = java.util.Set.of("1.4.7", "1.5.2", "1.6.4", "1.7.10", "1.12.2", "1.20.1", "b1.7.3");

    /** {@code legacy.autoSortMods} (default true): legacy jars found directly in mods/ are moved into mods/<minecraft version>/ on launch. */
    private static boolean autoSortMods(Path modsDir) {
        return !"false".equalsIgnoreCase(LibraryBridges.readConfig(modsDir).getProperty("legacy.autoSortMods", "true").trim());
    }

    /**
     * Moves a legacy jar out of mods/ into mods/<version>/ (1.12.2 jars that bundle Mixin break NeoForge's own scan of mods/ itself, and a
     * mods folder that mixes versions makes bug reports hard to read). Returns the new path, or null when it stayed where it is (a file of that
     * name is already in the folder, or the move failed): it is then loaded from where it is.
     */
    private static Path moveIntoVersionFolder(Path jar, Path modsDir, LegacyVersion version) {
        if (!VERSION_FOLDERS.contains(version.id())) return null;
        try {
            Path dir = modsDir.resolve(version.id());
            Path target = dir.resolve(jar.getFileName());
            if (Files.exists(target)) {
                LOG.warn("Legacy Connector: {} is also in mods/{}/; the copy in mods/ is left where it is (delete one of them).", jar.getFileName(), version.id());
                return null;
            }
            Files.createDirectories(dir);
            Files.move(jar, target);
            LOG.info("Legacy Connector: moved {} (a Minecraft {} mod) into mods/{}/ (legacy.autoSortMods=false turns this off)", jar.getFileName(), version.id(), version.id());
            return target;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Legacy Connector: could not move {} into mods/{}/ ({}); it is loaded from where it is. Move it there yourself.", jar.getFileName(), version.id(), e.toString());
            return null;
        }
    }

    /** Jars left out because another jar holds the same mod id in a newer version (a player keeps two downloads of one mod in mods/). */
    private static final Map<LegacyJar, String> DUPLICATE_SKIPS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * FML refuses two files with one mod id; here the second copy used to make neither load (the mod never registered, with no message). Keeps the
     * jar with the highest declared version (ties: the newer file) and says which one was dropped.
     */
    /**
     * Two jars with one mod id: within a legacy version the newer one wins and the other is skipped. Across legacy versions (CodeChickenLib
     * for 1.7.10 and for 1.12.2) both stay when side-by-side libraries are on: the best copy of the primary version keeps the id, the others become
     * isolated units (their packages are relocated per version by {@link org.kylepitcock.legacyconnector.loader.transform.CrossVersionPackages}).
     * @return the units of the copies that run next to another version's copy (id isolation only)
     */
    private static List<SideBySide.Unit> dropDuplicateMods(List<LegacyJar> legacy, boolean sideBySide) {
        DUPLICATE_SKIPS.clear();
        List<SideBySide.Unit> units = new ArrayList<>();
        Map<String, List<LegacyJar>> byId = new java.util.LinkedHashMap<>();
        for (LegacyJar j : legacy) for (LegacyModDecl m : j.mods) byId.computeIfAbsent(m.modernId(), k -> new ArrayList<>()).add(j);
        for (Map.Entry<String, List<LegacyJar>> e : byId.entrySet()) {
            List<LegacyJar> jars = e.getValue().stream().distinct().toList();
            if (jars.size() < 2) continue;
            Map<LegacyVersion, LegacyJar> bestPerVersion = new java.util.LinkedHashMap<>();
            for (LegacyJar j : jars) {
                LegacyJar best = bestPerVersion.get(j.version);
                if (best == null || compareDeclared(j, e.getKey(), best, e.getKey()) > 0) bestPerVersion.put(j.version, j);
            }
            for (LegacyJar j : jars) {
                LegacyJar best = bestPerVersion.get(j.version);
                if (j == best || DUPLICATE_SKIPS.containsKey(j)) continue;
                DUPLICATE_SKIPS.put(j, "another file holds the same mod (" + e.getKey() + ") in a newer version: " + best.fileName());
                LOG.warn("Legacy Connector: {} and {} are two copies of the mod '{}'; only {} is loaded (remove the other from mods/).",
                        j.fileName(), best.fileName(), e.getKey(), best.fileName());
            }
            if (bestPerVersion.size() < 2) continue;
            LegacyVersion primary = null;
            for (LegacyVersion v : bestPerVersion.keySet()) if (primary == null || v.ordinal() > primary.ordinal()) primary = v;
            for (var v : bestPerVersion.entrySet()) {
                if (v.getKey() == primary) continue;
                if (!sideBySide) {
                    DUPLICATE_SKIPS.put(v.getValue(), "another file holds the same mod (" + e.getKey() + ") for " + primary.id() + ": " + bestPerVersion.get(primary).fileName()
                            + " (experimental.sideBySideLibraries=false)");
                    continue;
                }
                units.add(new SideBySide.Unit(v.getKey(), e.getKey(), SideBySide.newIdFor(e.getKey(), v.getKey()), Set.of(),
                        "the " + primary.id() + " copy " + bestPerVersion.get(primary).fileName() + " has the same mod id"));
            }
        }
        return units;
    }

    private static int compareDeclared(LegacyJar a, String idA, LegacyJar b, String idB) {
        long[] na = numbers(declaredVersion(a, idA)), nb = numbers(declaredVersion(b, idB));
        for (int i = 0; i < Math.max(na.length, nb.length); i++) {
            long x = i < na.length ? na[i] : 0, y = i < nb.length ? nb[i] : 0;
            if (x != y) return Long.compare(x, y);
        }
        try {
            return Long.compare(Files.getLastModifiedTime(a.existing()).toMillis(), Files.getLastModifiedTime(b.existing()).toMillis());
        } catch (IOException | RuntimeException ex) {
            return 0;
        }
    }

    private static String declaredVersion(LegacyJar jar, String modernId) {
        return jar.mods.stream().filter(m -> m.modernId().equals(modernId)).map(m -> m.version).filter(java.util.Objects::nonNull).findFirst().orElse("");
    }

    /** The digit groups of a version string ("1.3.5 B3" gives 1, 3, 5, 3). */
    private static long[] numbers(String version) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{1,9}").matcher(version);
        List<Long> out = new ArrayList<>();
        while (m.find()) out.add(Long.parseLong(m.group()));
        return out.stream().mapToLong(Long::longValue).toArray();
    }

    private static String skipReason(LegacyJar jar) {
        String lib = LIBRARY_SKIPS.get(jar);
        if (lib != null) return lib;
        String dup = DUPLICATE_SKIPS.get(jar);
        if (dup != null) return dup;
        // A client-only mod (@Mod class under @SideOnly(CLIENT), or clientSideOnly = true) has nothing to do on a dedicated server: FML dropped it there.
        if (jar.hasMods() && "SERVER".equals(currentSide) && jar.mods.stream().allMatch(m -> m.clientSideOnly)) {
            return "client-only mod (@SideOnly(CLIENT) mod class); not loaded on a dedicated server";
        }
        if (!jar.hasMods() && jar.tweakClass != null) {
            return "LaunchWrapper tweaker (" + jar.tweakClass + ") with no @Mod; tweakers cannot run on modlauncher";
        }
        return null;
    }

    /** Content hash + transformer version + shim index + the other legacy jars of its version: any change re-translates. */
    private static String cacheName(LegacyJar jar, byte[] modSet) throws IOException {
        MessageDigest md = SplitPackageResolver.sha256();
        md.update(fileDigest(jar.existing()));
        md.update(("v" + LegacyJarTransformer.TRANSFORMER_VERSION).getBytes(StandardCharsets.UTF_8));
        if ("false".equals(System.getProperty("legacyconnector.forge1201.softlink"))) md.update("nosoftlink".getBytes(StandardCharsets.UTF_8));
        md.update(LOADER_DIGEST);
        // Any change to the embedded runtime (shim code, bridges) re-translates: no cache can outlive the build that wrote it.
        if (runtimeDigest != null) md.update(runtimeDigest);
        if (indexDigest != null) md.update(indexDigest);
        if (modSet != null) md.update(modSet);
        String name = jar.fileName().replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.endsWith(".jar") || name.endsWith(".zip")) name = name.substring(0, name.length() - 4);
        return name + "-" + HexFormat.of().formatHex(md.digest()).substring(0, 16) + ".jar";
    }

    private static byte[] indexDigest;
    private static byte[] runtimeDigest;
    /** Digest of the loader's own jar: any change to the translator re-translates, without manual version bumps. */
    private static final byte[] LOADER_DIGEST = loaderDigest();

    private static byte[] loaderDigest() {
        try (InputStream in = LegacyModLocator.class.getClassLoader().getResourceAsStream("legacyconnector/loader-build-id.txt")) {
            if (in != null) return in.readAllBytes();
        } catch (IOException e) {
            LOG.debug("Legacy Connector: cannot read loader build id", e);
        }
        LOG.warn("Legacy Connector: loader build id missing; translation caches may be stale after updates");
        return new byte[0];
    }

    private static Path extractRuntime(Path cacheDir) throws IOException {
        try (InputStream in = LegacyModLocator.class.getResourceAsStream(EMBEDDED_RUNTIME)) {
            if (in == null) return null; // development: the runtime mod is on the classpath already
            byte[] bytes = in.readAllBytes();
            runtimeDigest = SplitPackageResolver.sha256().digest(bytes);
            String hash = HexFormat.of().formatHex(runtimeDigest).substring(0, 16);
            Path target = cacheDir.resolve("legacyconnector-runtime-" + hash + ".jar");
            if (!Files.exists(target)) Files.write(target, bytes);
            return target;
        }
    }

    private static ShimIndex loadShimIndex(Path runtimeJar) throws IOException {
        byte[] data = null;
        String override = System.getProperty(SHIM_INDEX_PROPERTY);
        if (override != null && Files.exists(Path.of(override))) {
            data = Files.readAllBytes(Path.of(override));
        } else if (runtimeJar != null) {
            try (ZipFile zip = new ZipFile(runtimeJar.toFile())) {
                ZipEntry e = zip.getEntry(ShimIndex.RESOURCE);
                if (e != null) try (InputStream in = zip.getInputStream(e)) {
                    data = in.readAllBytes();
                }
            }
        }
        if (data == null) {
            try (InputStream in = LegacyModLocator.class.getClassLoader().getResourceAsStream(ShimIndex.RESOURCE)) {
                if (in != null) data = in.readAllBytes();
            }
        }
        if (data == null) {
            LOG.warn("Legacy Connector: no shim index found; link checking and duplicate-class removal are disabled");
            return ShimIndex.empty();
        }
        indexDigest = SplitPackageResolver.sha256().digest(data);
        return ShimIndex.read(new java.io.ByteArrayInputStream(data));
    }

    /** Jars with a mod whose required dependency is missing, and the plain-words messages (first lines of their report entry). */
    private static final Map<LegacyJar, List<String>> MISSING_DEPENDENCIES = new java.util.LinkedHashMap<>();

    private static void writeReport(Path gameDir, Map<LegacyJar, String> outcomes, SplitPackageResolver.Outcome split) {
        Path report = gameDir.resolve("logs").resolve("legacyconnector-report.txt");
        try {
            Files.createDirectories(report.getParent());
            try (Writer w = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
                w.write("Legacy Connector " + Provenance.translatorVersion() + " legacy mod report - " + LocalDateTime.now() + "\n");
                w.write("=".repeat(78) + "\n\n");
                for (var e : outcomes.entrySet()) {
                    LegacyJar jar = e.getKey();
                    w.write(jar.fileName() + "  [" + jar.version.id() + ", " + jar.versionEvidence + "]\n");
                    for (String missing : MISSING_DEPENDENCIES.getOrDefault(jar, List.of())) w.write("  MISSING DEPENDENCY: " + missing + "\n");
                    for (LegacyModDecl mod : jar.mods) {
                        w.write("  mod " + mod.legacyId + " -> " + mod.modernId() + "  (" + mod.name + " " + mod.version + ")\n");
                    }
                    if (jar.coreModClass != null) {
                        w.write("  WARNING: ships a coremod (" + jar.coreModClass + "): its plugin runs, its bytecode transformers do NOT. Features that\n"
                                + "           depend on patching Minecraft code will be missing.\n");
                    }
                    if (jar.mixinConfigs != null) {
                        w.write("  " + LegacyModReportNotes.mixinStatus(jar.version, jar.mixinConfigs) + "\n");
                    }
                    if (jar.accessTransformer != null) w.write("  NOTE: legacy access transformer " + jar.accessTransformer + " ignored (shim members are public).\n");
                    for (String n : jar.notes) w.write("  note: " + n + "\n");
                    String outcome = e.getValue();
                    w.write(outcome.startsWith("  ") ? outcome : "  " + outcome + "\n");
                    w.write("\n");
                }
                if (!unreadableNotes.isEmpty()) {
                    w.write("Archives that could not be read (skipped):" + System.lineSeparator());
                    for (String n : unreadableNotes) w.write("  " + n + System.lineSeparator());
                    w.write(System.lineSeparator());
                }
                if (!liteNotes.isEmpty()) {
                    w.write("Files that are not Forge mods:" + System.lineSeparator());
                    for (String n : liteNotes) w.write("  " + n + System.lineSeparator());
                    w.write(System.lineSeparator());
                }
                if (!libraryNotes.isEmpty()) {
                    w.write("Bundled libraries (Scala, trove, ...):\n");
                    for (String n : libraryNotes) w.write("  " + n + "\n");
                    w.write("\n");
                }
                if (!split.sharedPackages().isEmpty()) {
                    w.write("Packages shared by several jars (moved to " + split.sharedLibrary().getFileName() + "):\n");
                    for (String p : split.sharedPackages()) w.write("  " + p.replace('/', '.') + "\n");
                }
            }
            LOG.info("Legacy Connector: wrote compatibility report to {}", report);
        } catch (IOException e) {
            LOG.warn("Legacy Connector: could not write report", e);
        }
    }

    @Override
    public String toString() {
        return "Legacy Connector legacy mod locator";
    }
}
