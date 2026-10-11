package org.kylepitcock.mccompat.loader;

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
import java.util.zip.ZipFile;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.kylepitcock.mccompat.loader.shim.ClassInfo;
import org.kylepitcock.mccompat.loader.shim.ShimIndex;
import org.kylepitcock.mccompat.loader.synth.SynthDemand;
import org.kylepitcock.mccompat.loader.synth.SynthJarBuilder;
import org.kylepitcock.mccompat.loader.transform.LegacyJarTransformer;
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
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit");
    /** Nested runtime (shim) mod inside the distributed loader jar. Absent in development. */
    static final String EMBEDDED_RUNTIME = "/META-INF/mccompat/mccompat-runtime.jar";
    static final String SHIM_INDEX_PROPERTY = "mccompat.shimIndex";

    @Override
    public int getPriority() {
        return 100; // before ModsFolderLocator (0)
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path modsDir = FMLPaths.MODSDIR.get();
        Path cacheDir = gameDir.resolve(".retrofit").resolve("cache");
        try {
            Files.createDirectories(cacheDir);
            Path runtimeJar = extractRuntime(cacheDir);
            if (runtimeJar != null) {
                pipeline.addPath(runtimeJar, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }

            ShimIndex index = loadShimIndex(runtimeJar);
            LOG.info("Retrofit: shim index has {} legacy API classes", index.size());

            List<LegacyJar> legacy = new ArrayList<>();
            if (Files.isDirectory(modsDir)) {
                try (Stream<Path> files = Files.list(modsDir)) {
                    for (Path p : files.filter(f -> f.getFileName().toString().toLowerCase().endsWith(".jar"))
                            .sorted(Comparator.comparing(f -> f.getFileName().toString().toLowerCase())).toList()) {
                        if (context.isLocated(p)) continue;
                        try {
                            LegacyJar jar = LegacyJarInspector.inspect(p);
                            if (jar != null) legacy.add(jar);
                        } catch (IOException | RuntimeException e) {
                            LOG.debug("Retrofit: could not inspect {}", p, e);
                        }
                    }
                }
            }
            if (legacy.isEmpty()) return;

            List<LibraryBridges.Bridge> bridges = LibraryBridges.active(modsDir);
            LegacyVersion.setBridgedRoots(bridges.stream().map(LibraryBridges.Bridge::apiRoot).toList());
            List<String> present = new ArrayList<>(legacy.stream().flatMap(j -> j.mods.stream()).map(m -> m.legacyId).toList());
            for (LibraryBridges.Bridge b : bridges) present.add(b.legacyModId());
            // Read by the runtime's Loader.isModLoaded: bridged libraries count as loaded.
            System.setProperty("mccompat.bridgedMods", String.join(",", bridges.stream().map(LibraryBridges.Bridge::legacyModId).toList()));
            if (!bridges.isEmpty()) LOG.info("Retrofit: bridging {} to their modern replacements", bridges);
            String side = side();
            LegacyJarTransformer transformer = new LegacyJarTransformer(index, LegacyModLocator.class.getClassLoader()).presentMods(present).side(side);
            org.kylepitcock.mccompat.loader.meta.ModMetadataGenerator.setLoadedMods(legacy.stream()
                    .filter(j -> skipReason(j) == null && LibraryBridges.replacing(j, bridges) == null)
                    .flatMap(j -> j.mods.stream()).map(LegacyModDecl::modernId).toList());
            Map<LegacyJar, String> outcomes = new LinkedHashMap<>();
            List<Path> translated = new ArrayList<>();
            Set<Path> keep = new HashSet<>();
            if (runtimeJar != null) keep.add(runtimeJar);
            // Mods link against each other (RotaryCraft extends DragonAPI classes), so a jar's translation depends on
            // every legacy jar of its version: that set is part of the cache key, and peers are scanned on a miss.
            Map<LegacyVersion, byte[]> modSet = modSetDigests(legacy, bridges);
            Map<LegacyVersion, Map<String, ClassInfo>> peers = new EnumMap<>(LegacyVersion.class);
            Map<LegacyVersion, SynthDemand> demand = new EnumMap<>(LegacyVersion.class);
            for (LegacyJar jar : legacy) {
                // Claim the original even if we end up skipping it, so FML does not raise its own
                // "this is an old Forge mod" error on top of ours.
                context.addLocated(jar.path);
                LibraryBridges.Bridge replacedBy = LibraryBridges.replacing(jar, bridges);
                String skip = replacedBy != null ? "replaced by " + replacedBy.modernModId() + " (legacy " + replacedBy.legacyModId()
                        + " API bridged)" : skipReason(jar);
                if (skip != null) {
                    outcomes.put(jar, skip);
                    // Unknown translation keys are displayed verbatim by FML's error screen. Bridged libraries are expected.
                    if (replacedBy == null) pipeline.addIssue(ModLoadingIssue.warning("Retrofit skipped " + jar.fileName() + ": " + skip.replace("'", ""))
                            .withAffectedPath(jar.path));
                    continue;
                }
                try {
                    Path out = cacheDir.resolve(cacheName(jar, modSet.get(jar.version)));
                    Path linkReport = out.resolveSibling(out.getFileName() + ".links.txt");
                    Path demandFile = out.resolveSibling(out.getFileName() + ".synth.txt");
                    if (!Files.exists(out) || !Files.exists(demandFile)) {
                        Map<String, ClassInfo> jarPeers = peers.computeIfAbsent(jar.version, v -> scanPeers(transformer, legacy, v));
                        long start = System.nanoTime();
                        LegacyJarTransformer.Result result = transformer.transform(jar, out, jarPeers);
                        result.demand.write(demandFile);
                        LOG.info("Retrofit: translated {} ({} classes, {} JDK fixes, {} virtual fields, {} synthesized API classes) in {} ms",
                                jar.fileName(), result.classes, result.compatRedirects, result.virtualFieldRedirects,
                                result.links.synthesizedClasses.size(), (System.nanoTime() - start) / 1_000_000);
                        StringBuilder text = new StringBuilder();
                        for (String n : result.notes) text.append("  note: ").append(n).append('\n');
                        text.append("  ").append(result.links.errorCount()).append(" unresolved link(s)")
                                .append(result.links.errorCount() == 0 ? " - every legacy API reference links\n" : ":\n");
                        text.append(result.links.render());
                        Files.writeString(linkReport, text);
                        if (result.links.errorCount() > 0) {
                            LOG.warn("Retrofit: {} has {} reference(s) that cannot be linked; see logs/retrofit-report.txt",
                                    jar.fileName(), result.links.errorCount());
                        }
                    }
                    demand.computeIfAbsent(jar.version, v -> new SynthDemand()).merge(SynthDemand.read(demandFile));
                    keep.add(out);
                    keep.add(linkReport);
                    keep.add(demandFile);
                    outcomes.put(jar, Files.exists(linkReport) ? Files.readString(linkReport) : "  (cached, no link report)\n");
                    translated.add(out);
                } catch (Exception e) {
                    LOG.error("Retrofit: failed to translate {}", jar.fileName(), e);
                    outcomes.put(jar, "translation failed: " + e);
                    pipeline.addIssue(ModLoadingIssue.error("Retrofit could not translate " + jar.fileName())
                            .withAffectedPath(jar.path).withCause(e));
                }
            }

            // One generated jar per legacy version with every API class/member the shim lacks, for all mods at once
            // (mods pass these types to each other, so there must be exactly one copy of each).
            for (var e : demand.entrySet()) {
                if (e.getValue().isEmpty()) continue;
                Path synthJar = buildSynthJar(cacheDir, e.getKey(), index, e.getValue());
                keep.add(synthJar);
                pipeline.addPath(synthJar, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }

            SplitPackageResolver.Outcome split = SplitPackageResolver.resolve(translated, cacheDir);
            if (split.sharedLibrary() != null) {
                LOG.info("Retrofit: {} packages shipped by several legacy jars were merged into {}",
                        split.sharedPackages().size(), split.sharedLibrary().getFileName());
                keep.add(split.sharedLibrary());
                pipeline.addPath(split.sharedLibrary(), ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }
            for (Path p : split.jars()) {
                keep.add(p);
                pipeline.addPath(p, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }
            writeReport(gameDir, outcomes, split);
            pruneCache(cacheDir, keep);
        } catch (IOException e) {
            LOG.error("Retrofit: legacy mod discovery failed", e);
        }
    }

    private static Map<String, ClassInfo> scanPeers(LegacyJarTransformer transformer, List<LegacyJar> legacy, LegacyVersion version) {
        Map<String, ClassInfo> out = new HashMap<>();
        for (LegacyJar jar : legacy) {
            if (jar.version != version || skipReason(jar) != null) continue;
            try {
                for (ClassInfo c : transformer.scan(jar)) out.putIfAbsent(c.name, c);
            } catch (IOException | RuntimeException e) {
                LOG.debug("Retrofit: could not scan {}", jar.fileName(), e);
            }
        }
        return out;
    }

    private static Map<LegacyVersion, byte[]> modSetDigests(List<LegacyJar> legacy, List<LibraryBridges.Bridge> bridges) throws IOException {
        Map<LegacyVersion, MessageDigest> digests = new EnumMap<>(LegacyVersion.class);
        for (LegacyJar jar : legacy) {
            MessageDigest md = digests.computeIfAbsent(jar.version, v -> SplitPackageResolver.sha256());
            md.update(fileDigest(jar.path));
            md.update(bridges.toString().getBytes(StandardCharsets.UTF_8));
            md.update(side().getBytes(StandardCharsets.UTF_8)); // @SideOnly stripping differs per side
        }
        Map<LegacyVersion, byte[]> out = new EnumMap<>(LegacyVersion.class);
        digests.forEach((v, md) -> out.put(v, md.digest()));
        return out;
    }

    private static byte[] fileDigest(Path p) throws IOException {
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
            if (indexDigest != null) md.update(indexDigest);
            Path target = cacheDir.resolve("retrofit-synth-" + version.id() + "-" + HexFormat.of().formatHex(md.digest()).substring(0, 16) + ".jar");
            if (Files.exists(target)) return target;
            long start = System.nanoTime();
            Map<String, byte[]> classes = new SynthJarBuilder(version, index, LegacyModLocator.class.getClassLoader(), demand).build();
            Manifest m = new Manifest();
            m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            m.getMainAttributes().putValue("FMLModType", "GAMELIBRARY");
            m.getMainAttributes().putValue("Automatic-Module-Name", "mccompat.synth.v" + version.id().replace('.', '_'));
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
            LOG.info("Retrofit: synthesized {} legacy {} API classes the shim does not implement yet ({} helpers) in {} ms",
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
                    LOG.debug("Retrofit: could not prune {}", p, e);
                }
            }
        } catch (IOException e) {
            LOG.debug("Retrofit: could not prune cache", e);
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

    private static String skipReason(LegacyJar jar) {
        if (!jar.hasMods() && jar.tweakClass != null) {
            return "LaunchWrapper tweaker (" + jar.tweakClass + ") with no @Mod; tweakers cannot run on modlauncher";
        }
        return null;
    }

    /** Content hash + transformer version + shim index + the other legacy jars of its version: any change re-translates. */
    private static String cacheName(LegacyJar jar, byte[] modSet) throws IOException {
        MessageDigest md = SplitPackageResolver.sha256();
        md.update(fileDigest(jar.path));
        md.update(("v" + LegacyJarTransformer.TRANSFORMER_VERSION).getBytes(StandardCharsets.UTF_8));
        md.update(LOADER_DIGEST);
        if (indexDigest != null) md.update(indexDigest);
        if (modSet != null) md.update(modSet);
        String name = jar.fileName().replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.endsWith(".jar")) name = name.substring(0, name.length() - 4);
        return name + "-" + HexFormat.of().formatHex(md.digest()).substring(0, 16) + ".jar";
    }

    private static byte[] indexDigest;
    /** Digest of the loader's own jar: any change to the translator re-translates, without manual version bumps. */
    private static final byte[] LOADER_DIGEST = loaderDigest();

    private static byte[] loaderDigest() {
        try (InputStream in = LegacyModLocator.class.getClassLoader().getResourceAsStream("mccompat/loader-build-id.txt")) {
            if (in != null) return in.readAllBytes();
        } catch (IOException e) {
            LOG.debug("Retrofit: cannot read loader build id", e);
        }
        LOG.warn("Retrofit: loader build id missing; translation caches may be stale after updates");
        return new byte[0];
    }

    private static Path extractRuntime(Path cacheDir) throws IOException {
        try (InputStream in = LegacyModLocator.class.getResourceAsStream(EMBEDDED_RUNTIME)) {
            if (in == null) return null; // development: the runtime mod is on the classpath already
            byte[] bytes = in.readAllBytes();
            String hash = HexFormat.of().formatHex(SplitPackageResolver.sha256().digest(bytes)).substring(0, 16);
            Path target = cacheDir.resolve("retrofit-runtime-" + hash + ".jar");
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
            LOG.warn("Retrofit: no shim index found; link checking and duplicate-class removal are disabled");
            return ShimIndex.empty();
        }
        indexDigest = SplitPackageResolver.sha256().digest(data);
        return ShimIndex.read(new java.io.ByteArrayInputStream(data));
    }

    private static void writeReport(Path gameDir, Map<LegacyJar, String> outcomes, SplitPackageResolver.Outcome split) {
        Path report = gameDir.resolve("logs").resolve("retrofit-report.txt");
        try {
            Files.createDirectories(report.getParent());
            try (Writer w = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
                w.write("Retrofit legacy mod report - " + LocalDateTime.now() + "\n");
                w.write("=".repeat(78) + "\n\n");
                for (var e : outcomes.entrySet()) {
                    LegacyJar jar = e.getKey();
                    w.write(jar.fileName() + "  [" + jar.version.id() + ", " + jar.versionEvidence + "]\n");
                    for (LegacyModDecl mod : jar.mods) {
                        w.write("  mod " + mod.legacyId + " -> " + mod.modernId() + "  (" + mod.name + " " + mod.version + ")\n");
                    }
                    if (jar.coreModClass != null) {
                        w.write("  WARNING: ships a coremod (" + jar.coreModClass + ") which is NOT executed. Features that\n"
                                + "           depend on patching Minecraft code will be missing.\n");
                    }
                    if (jar.mixinConfigs != null) w.write("  WARNING: ships mixins (" + jar.mixinConfigs + ") targeting legacy Minecraft; not applied.\n");
                    if (jar.accessTransformer != null) w.write("  NOTE: legacy access transformer " + jar.accessTransformer + " ignored (shim members are public).\n");
                    for (String n : jar.notes) w.write("  note: " + n + "\n");
                    String outcome = e.getValue();
                    w.write(outcome.startsWith("  ") ? outcome : "  " + outcome + "\n");
                    w.write("\n");
                }
                if (!split.sharedPackages().isEmpty()) {
                    w.write("Packages shared by several jars (moved to " + split.sharedLibrary().getFileName() + "):\n");
                    for (String p : split.sharedPackages()) w.write("  " + p.replace('/', '.') + "\n");
                }
            }
            LOG.info("Retrofit: wrote compatibility report to {}", report);
        } catch (IOException e) {
            LOG.warn("Retrofit: could not write report", e);
        }
    }

    @Override
    public String toString() {
        return "Retrofit legacy mod locator";
    }
}
